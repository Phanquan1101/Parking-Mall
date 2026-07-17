package com.parkflow.mall.payment.service;

import com.parkflow.mall.payment.dto.PaymentDtos.*;
import com.parkflow.mall.payment.model.*;
import com.parkflow.mall.payment.repository.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

 private final PaymentOrderRepository orders;
 private final InMemoryIdempotencyStore idempotency;
 private final RestTemplate rest;
 private final String parkingUrl;
 private final String internalToken;
 private final String mode;
 private final long ttl;

 private final Map<String, ParkingUpdateStatus> updates = new ConcurrentHashMap<>();
 private final Map<String, PaymentReconciliationItem> items = new ConcurrentHashMap<>();

 public PaymentService(
         PaymentOrderRepository o,
         InMemoryIdempotencyStore i,
         RestTemplate r,
         @Value("${app.parking-base-url}") String p,
         @Value("${app.internal-service-token}") String t,
         @Value("${app.payment-mode}") String m,
         @Value("${app.order-ttl-minutes}") long x
 ) {
  orders = o;
  idempotency = i;
  rest = r;
  parkingUrl = p;
  internalToken = t;
  mode = m;
  ttl = x;
 }

 public OrderResponse create(CreateOrderRequest r, String key) {
  if (r == null
          || r.targetType() != PaymentTargetType.PARKING_SESSION
          || blank(r.lookupToken())
          || blank(r.targetId())) {
   throw bad("Valid PARKING_SESSION target and lookupToken are required");
  }

  if (!blank(key)) {
   var old = orders.findByCreationKey(key);

   if (old.isPresent()) {
    return response(old.get());
   }
  }

  Ticket t = ticket(r.lookupToken());

  if (!r.targetId().equals(t.sessionId)) {
   throw bad("targetId does not match lookup token");
  }

  if ("PAID".equals(t.paymentStatus)
          || !List.of("ACTIVE", "PENDING_PAYMENT").contains(t.status)) {
   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Parking session is not payable"
   );
  }

  var o = new PaymentOrder(
          UUID.randomUUID().toString(),
          "PFPAY-" + UUID.randomUUID()
                  .toString()
                  .substring(0, 8)
                  .toUpperCase(),
          r.targetType(),
          r.targetId(),
          t.finalFee,
          "VND",
          PaymentStatus.PENDING,
          Instant.now(),
          Instant.now().plus(Duration.ofMinutes(ttl)),
          null,
          true,
          key
  );

  return response(orders.save(o));
 }

 public OrderResponse get(String id) {
  return response(find(id));
 }

 public synchronized SimulationResponse simulate(SimulateRequest r, String key) {
  if (!"SIMULATION".equalsIgnoreCase(mode)) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN,
           "Payment simulation is disabled"
   );
  }

  if (blank(key)) {
   throw bad("Idempotency-Key is required");
  }

  String prior = idempotency.get(key);

  if (prior != null) {
   return simulation(find(prior), status(find(prior)));
  }

  var o = find(r.paymentOrderId());

  if (o.status() == PaymentStatus.PAID) {
   idempotency.putIfAbsent(key, o.id());
   return simulation(o, status(o));
  }

  if (!o.paymentCode().equals(r.paymentCode()) || o.amount() != r.amount()) {
   orders.save(copy(o, PaymentStatus.MISMATCHED, null));

   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Payment code or amount mismatch"
   );
  }

  var paid = copy(o, PaymentStatus.PAID, Instant.now());
  String state = "UPDATED";

  try {
   updateParking(paid);
   updates.put(o.id(), ParkingUpdateStatus.UPDATED);
  } catch (RestClientException ex) {
   updates.put(o.id(), ParkingUpdateStatus.PENDING_RECONCILIATION);

   open(
           o,
           "PARKING_UPDATE_FAILED",
           "Parking update failed during simulation success."
   );

   state = "PENDING_RECONCILIATION";
  }

  orders.save(paid);
  idempotency.putIfAbsent(key, paid.id());

  return simulation(paid, state);
 }

 public synchronized Map<String, Object> reconcile(
         boolean includeExpired,
         boolean retry
 ) {
  int checked = 0;
  int expired = 0;
  int resolved = 0;
  int manual = 0;
  int mismatched = 0;
  int retried = 0;

  List<Map<String, Object>> out = new ArrayList<>();

  for (var o : orders.findAll()) {
   checked++;

   if (includeExpired
           && o.status() == PaymentStatus.PENDING
           && o.expiresAt().isBefore(Instant.now())) {
    orders.save(copy(o, PaymentStatus.EXPIRED, null));

    out.add(
            view(
                    open(
                            o,
                            "PENDING_EXPIRED",
                            "Pending order expired."
                    )
            )
    );

    expired++;
   }

   if (o.status() == PaymentStatus.MISMATCHED) {
    var i = open(
            o,
            "PAYMENT_MISMATCHED",
            "Mismatched payment requires manual review."
    );

    items.put(
            i.id(),
            new PaymentReconciliationItem(
                    i.id(),
                    i.paymentOrderId(),
                    i.targetId(),
                    i.issueType(),
                    "PENDING_MANUAL_REVIEW",
                    i.message(),
                    i.createdAt(),
                    Instant.now(),
                    i.attemptCount()
            )
    );

    out.add(view(items.get(i.id())));
    manual++;
    mismatched++;
   }

   if (retry
           && o.status() == PaymentStatus.PAID
           && updates.get(o.id()) == ParkingUpdateStatus.PENDING_RECONCILIATION) {
    retried++;

    var i = open(
            o,
            "PARKING_UPDATE_FAILED",
            "Parking update retry pending."
    );

    try {
     updateParking(o);
     updates.put(o.id(), ParkingUpdateStatus.UPDATED);

     var done = new PaymentReconciliationItem(
             i.id(),
             i.paymentOrderId(),
             i.targetId(),
             i.issueType(),
             "RESOLVED",
             "Parking payment status updated successfully on retry.",
             i.createdAt(),
             Instant.now(),
             i.attemptCount() + 1
     );

     items.put(done.id(), done);
     out.add(view(done));
     resolved++;
    } catch (RestClientException e) {
     out.add(view(i));
    }
   }
  }

  return Map.of(
          "reconciliationRunId", UUID.randomUUID().toString(),
          "startedAt", Instant.now().toString(),
          "completedAt", Instant.now().toString(),
          "summary", Map.of(
                  "checkedOrders", checked,
                  "expiredPendingOrders", expired,
                  "retriedParkingUpdates", retried,
                  "resolvedItems", resolved,
                  "pendingManualReview", manual,
                  "mismatchedOrders", mismatched
          ),
          "items", out
  );
 }

 public List<Map<String, Object>> listItems() {
  return items.values()
          .stream()
          .map(this::view)
          .toList();
 }

 public Map<String, Object> item(String id) {
  var i = items.get(id);

  if (i == null) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND,
           "Reconciliation item not found"
   );
  }

  return view(i);
 }

 private void updateParking(PaymentOrder o) {
  HttpHeaders h = new HttpHeaders();

  h.set("X-Internal-Service-Token", internalToken);
  h.setContentType(MediaType.APPLICATION_JSON);

  rest.exchange(
          parkingUrl
                  + "/internal/parking/sessions/"
                  + o.targetId()
                  + "/payment-status",
          HttpMethod.POST,
          new HttpEntity<>(
                  Map.of(
                          "paymentOrderId", o.id(),
                          "paymentStatus", "PAID",
                          "amountPaid", o.amount(),
                          "paidAt", o.paidAt().toString()
                  ),
                  h
          ),
          String.class
  );
 }

 private PaymentReconciliationItem open(
         PaymentOrder o,
         String issue,
         String msg
 ) {
  return items.values()
          .stream()
          .filter(i ->
                  i.paymentOrderId().equals(o.id())
                          && i.issueType().equals(issue)
                          && !"RESOLVED".equals(i.status())
          )
          .findFirst()
          .orElseGet(() -> {
           var i = new PaymentReconciliationItem(
                   UUID.randomUUID().toString(),
                   o.id(),
                   o.targetId(),
                   issue,
                   "OPEN",
                   msg,
                   Instant.now(),
                   Instant.now(),
                   0
           );

           items.put(i.id(), i);
           return i;
          });
 }

 private Map<String, Object> view(PaymentReconciliationItem i) {
  return Map.of(
          "itemId", i.id(),
          "paymentOrderId", i.paymentOrderId(),
          "targetType", "PARKING_SESSION",
          "targetId", i.targetId(),
          "issueType", i.issueType(),
          "status", i.status(),
          "message", i.message(),
          "attemptCount", i.attemptCount()
  );
 }

 private Ticket ticket(String token) {
  try {
   return rest.getForObject(
           parkingUrl + "/api/public/tickets/" + token,
           Ticket.class
   );
  } catch (RestClientException e) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND,
           "Ticket not found"
   );
  }
 }

 private PaymentOrder find(String id) {
  return orders.findById(id)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "Payment order not found"
                  )
          );
 }

 private PaymentOrder copy(
         PaymentOrder o,
         PaymentStatus s,
         Instant paid
 ) {
  return new PaymentOrder(
          o.id(),
          o.paymentCode(),
          o.targetType(),
          o.targetId(),
          o.amount(),
          o.currency(),
          s,
          o.createdAt(),
          o.expiresAt(),
          paid,
          o.simulationMode(),
          o.creationIdempotencyKey()
  );
 }

 private OrderResponse response(PaymentOrder o) {
  return new OrderResponse(
          o.id(),
          o.targetType(),
          o.targetId(),
          o.paymentCode(),
          o.amount(),
          o.currency(),
          o.status(),
          o.expiresAt(),
          o.paidAt(),
          o.simulationMode()
  );
 }

 private SimulationResponse simulation(PaymentOrder o, String p) {
  return new SimulationResponse(
          o.id(),
          o.status(),
          o.paidAt(),
          o.targetType(),
          o.targetId(),
          p
  );
 }

 private String status(PaymentOrder o) {
  return updates.getOrDefault(
          o.id(),
          ParkingUpdateStatus.UPDATED
  ).name();
 }

 private boolean blank(String s) {
  return s == null || s.isBlank();
 }

 private ResponseStatusException bad(String s) {
  return new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          s
  );
 }

 static class Ticket {

  public String sessionId;
  public String status;
  public String paymentStatus;
  public long finalFee;
 }
}