package com.parkflow.mall.parking.service;

import com.parkflow.mall.parking.dto.CheckInRequest;
import com.parkflow.mall.parking.dto.CheckOutRequest;
import com.parkflow.mall.parking.dto.CheckOutResponse;
import com.parkflow.mall.parking.dto.ExitPassResponse;
import com.parkflow.mall.parking.dto.GenerateExitPassRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateResponse;
import com.parkflow.mall.parking.dto.InternalDiscountUpdateRequest;
import com.parkflow.mall.parking.dto.InternalDiscountUpdateResponse;
import com.parkflow.mall.parking.dto.ManualOverrideRequest;
import com.parkflow.mall.parking.dto.ParkingSessionResponse;
import com.parkflow.mall.parking.dto.OfflineCheckInPayload;
import com.parkflow.mall.parking.dto.OfflineEventStatusResponse;
import com.parkflow.mall.parking.dto.OfflineSyncEventRequest;
import com.parkflow.mall.parking.dto.OfflineSyncRequest;
import com.parkflow.mall.parking.dto.OfflineSyncResponse;
import com.parkflow.mall.parking.dto.OfflineSyncResult;
import com.parkflow.mall.parking.dto.PublicTicketResponse;
import com.parkflow.mall.parking.dto.ValidateExitPassRequest;
import com.parkflow.mall.parking.dto.ValidateExitPassResponse;
import com.parkflow.mall.parking.exception.ExitPassException;
import com.parkflow.mall.parking.model.ExitPass;
import com.parkflow.mall.parking.model.ExitPassStatus;
import com.parkflow.mall.parking.model.ParkingSession;
import com.parkflow.mall.parking.model.MerchantDiscountState;
import com.parkflow.mall.parking.model.ParkingSessionEvent;
import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import com.parkflow.mall.parking.model.PlateSource;
import com.parkflow.mall.parking.model.VehicleType;
import com.parkflow.mall.parking.model.OfflineEvent;
import com.parkflow.mall.parking.model.OfflineEventType;
import com.parkflow.mall.parking.model.OfflineSyncStatus;
import com.parkflow.mall.parking.repository.ExitPassRepository;
import com.parkflow.mall.parking.repository.ParkingSessionRepository;
import com.parkflow.mall.parking.repository.OfflineEventRepository;
import com.parkflow.mall.parking.security.AuthenticatedUser;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Service
public class ParkingSessionService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private final ParkingSessionRepository repository;
    private final ExitPassRepository exitPassRepository;
    private final OfflineEventRepository offlineEventRepository;
    private final ParkingSessionEventRecorder eventRecorder;
    private final String publicTicketBaseUrl;
    private final long demoFlatFee;
    private final long exitPassTtlSeconds;
    private final RestTemplate restTemplate;
    private final String reservationServiceBaseUrl;
    private final String internalServiceToken;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, MerchantDiscountState> merchantDiscounts = new ConcurrentHashMap<>();

    public ParkingSessionService(
            ParkingSessionRepository repository,
            ExitPassRepository exitPassRepository,
            OfflineEventRepository offlineEventRepository,
            ParkingSessionEventRecorder eventRecorder,
            @Value("${app.public-ticket-base-url}") String publicTicketBaseUrl,
            @Value("${app.demo-flat-fee:5000}") long demoFlatFee,
            @Value("${app.exit-pass-ttl-seconds:60}") long exitPassTtlSeconds,
            RestTemplate restTemplate,
            @Value("${app.reservation-service-base-url}") String reservationServiceBaseUrl,
            @Value("${app.internal-service-token}") String internalServiceToken) {
        this.repository = repository;
        this.exitPassRepository = exitPassRepository;
        this.offlineEventRepository = offlineEventRepository;
        this.eventRecorder = eventRecorder;
        this.publicTicketBaseUrl = publicTicketBaseUrl.replaceAll("/$", "");
        this.demoFlatFee = demoFlatFee;
        this.exitPassTtlSeconds = exitPassTtlSeconds;
        this.restTemplate = restTemplate;
        this.reservationServiceBaseUrl = reservationServiceBaseUrl.replaceAll("/$", "");
        this.internalServiceToken = internalServiceToken;
    }

    public synchronized ParkingSessionResponse checkIn(CheckInRequest request, AuthenticatedUser actor) {
        if (request == null || request.vehiclePlate() == null || request.vehiclePlate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehiclePlate is required");
        }
        if (request.vehicleType() == null || request.entryGate() == null || request.entryGate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleType and entryGate are required");
        }
        if (request.plateSource() != PlateSource.MANUAL && request.plateSource() != PlateSource.OCR_ASSISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plateSource must be MANUAL or OCR_ASSISTED");
        }
        if (request.ocrConfidence() != null && (request.ocrConfidence() < 0 || request.ocrConfidence() > 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ocrConfidence must be between 0 and 1");
        }
        String normalizedPlate = normalizePlate(request.vehiclePlate());
        if (normalizedPlate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehiclePlate is required");
        }

        Instant now = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        String reservationId = null;
        if (!isBlank(request.reservationCode())) {
            if (repository.existsActiveByNormalizedPlate(normalizedPlate)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An ACTIVE session already exists for this vehicle plate");
            }
            reservationId = consumeReservation(request.reservationCode(), request.vehiclePlate(), request.vehicleType(), sessionId);
        }
        ParkingSession session = new ParkingSession(
                sessionId, nextSessionCode(now), normalizedPlate, normalizedPlate,
                request.vehicleType(), ParkingSessionStatus.ACTIVE, PaymentStatus.UNPAID, now,
                request.entryGate().trim(), actor.id(), request.plateSource(), nextToken(), null, null, null,
                null, null, null, false, null, null, null, reservationId, request.reservationCode(), request.ocrRequestId(), request.ocrCandidatePlate(), request.ocrConfidence(), now, now);
        if (!repository.saveIfNoActive(session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ACTIVE session already exists for this vehicle plate");
        }
        eventRecorder.record(new ParkingSessionEvent("CHECK_IN_CREATED", session.id(), actor.id(), now));
        return toStaffResponse(session);
    }

    public ParkingSessionResponse getById(String sessionId) { return toStaffResponse(findSessionById(sessionId)); }

    public List<ParkingSessionResponse> search(String status, String plate) {
        ParkingSessionStatus parsedStatus = parseStatus(status);
        String normalizedPlate = plate == null || plate.isBlank() ? null : normalizePlate(plate);
        return repository.search(parsedStatus, normalizedPlate).stream().map(this::toStaffResponse).toList();
    }

    public PublicTicketResponse publicTicket(String lookupToken) {
        ParkingSession session = repository.findByLookupToken(lookupToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        long duration = Math.max(0, Duration.between(session.entryTime(), Instant.now()).toMinutes());
        MerchantDiscountState discount = discountFor(session.id());
        boolean eligible = isExitEligible(session);
        boolean activePass = exitPassRepository.findActiveBySessionId(session.id()).isPresent();
        String exitPassMessage = session.status() == ParkingSessionStatus.EXITED
                ? "Vehicle has already exited."
                : eligible ? (activePass ? "A current Exit Pass exists. Generate a new one only if needed." : "Payment complete. Generate an Exit Pass before leaving.")
                : "Complete payment before generating an Exit Pass.";
        return new PublicTicketResponse(session.id(), session.sessionCode(), session.vehiclePlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), duration, demoFlatFee, discount.discountAmount(), discount.finalFee(),
                discount.totalEligibleInvoiceAmount(), discount.discountPolicy(), discount.discountAmount() > 0 ? "Merchant invoice discount applied." : "No merchant discount applied.",
                eligible, activePass, exitPassMessage,
                "QR Lookup Token is for ticket lookup only and cannot authorize exit.");
    }

    public synchronized ExitPassResponse generateExitPass(String sessionId, GenerateExitPassRequest request) {
        if (request == null || request.lookupToken() == null || request.lookupToken().isBlank()) {
            throw exitError(HttpStatus.BAD_REQUEST, "LOOKUP_TOKEN_REQUIRED", "lookupToken is required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (!request.lookupToken().equals(session.qrLookupToken())) {
            throw exitError(HttpStatus.FORBIDDEN, "LOOKUP_TOKEN_SESSION_MISMATCH", "Ticket lookup token does not match this session");
        }
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);

        exitPassRepository.invalidateActiveForSession(sessionId);
        Instant now = Instant.now();
        ExitPass pass = new ExitPass(UUID.randomUUID().toString(), nextToken(), sessionId, ExitPassStatus.ACTIVE,
                now, now.plusSeconds(exitPassTtlSeconds), null, null, "CUSTOMER_TICKET", exitPassTtlSeconds);
        exitPassRepository.save(pass);
        repository.update(copyWithExitPass(session, pass.id(), now));
        eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_CREATED", sessionId, "public-ticket", now));
        return new ExitPassResponse(pass.token(), sessionId, session.sessionCode(), pass.expiresAt(), pass.ttlSeconds(),
                pass.status(), "Dynamic Exit Pass is short-lived and can be used only once.");
    }

    public synchronized ValidateExitPassResponse validateExitPass(String token, ValidateExitPassRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "VALIDATION_INPUT_REQUIRED", "exitPlate and exitGate are required");
        }
        ExitPass pass = requireUsablePass(token);
        ParkingSession session = findSessionById(pass.sessionId());
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        if (!session.normalizedPlate().equals(exitPlate)) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_PLATE_MISMATCH", session.id(), actor.id(), Instant.now(), "exitGate=" + request.exitGate().trim()));
            throw exitError(HttpStatus.CONFLICT, "PLATE_MISMATCH", "Exit plate does not match the parking session");
        }
        return new ValidateExitPassResponse(true, session.id(), session.sessionCode(), session.vehiclePlate(),
                session.normalizedPlate(), exitPlate, session.paymentStatus(), session.status(), pass.expiresAt(),
                "Exit Pass is valid. Vehicle can be checked out.");
    }

    public synchronized CheckOutResponse checkOut(String sessionId, CheckOutRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.exitPassToken()) || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "CHECKOUT_INPUT_REQUIRED", "exitPassToken, exitPlate and exitGate are required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        ExitPass pass = requireUsablePass(request.exitPassToken());
        if (!pass.sessionId().equals(sessionId)) {
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_SESSION_MISMATCH", "Exit Pass does not belong to this session");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        if (!session.normalizedPlate().equals(exitPlate)) {
            eventRecorder.record(new ParkingSessionEvent("CHECKOUT_PLATE_MISMATCH", sessionId, actor.id(), Instant.now(), "exitGate=" + request.exitGate().trim()));
            throw exitError(HttpStatus.CONFLICT, "PLATE_MISMATCH", "Exit plate does not match the parking session");
        }
        Instant now = Instant.now();
        repository.update(copyExited(session, request.exitGate(), exitPlate, false, null, null, now));
        exitPassRepository.save(new ExitPass(pass.id(), pass.token(), pass.sessionId(), ExitPassStatus.USED,
                pass.createdAt(), pass.expiresAt(), now, pass.invalidatedAt(), pass.createdFrom(), pass.ttlSeconds()));
        eventRecorder.record(new ParkingSessionEvent("CHECKOUT_COMPLETED", sessionId, actor.id(), now, "exitGate=" + request.exitGate().trim()));
        return toCheckOutResponse(findSessionById(sessionId), false, null, "Vehicle checked out successfully.");
    }

    public synchronized CheckOutResponse manualOverride(String sessionId, ManualOverrideRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.reason()) || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "MANUAL_OVERRIDE_REASON_REQUIRED", "reason, exitPlate and exitGate are required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        String suspiciousReason = session.normalizedPlate().equals(exitPlate) ? null : "Manual override accepted with plate mismatch";
        Instant now = Instant.now();
        repository.update(copyExited(session, request.exitGate(), exitPlate, true, request.reason().trim(), suspiciousReason, now));
        exitPassRepository.invalidateActiveForSession(sessionId);
        eventRecorder.record(new ParkingSessionEvent("MANUAL_OVERRIDE_CHECKOUT", sessionId, actor.id(), now,
                "reason=" + request.reason().trim() + (suspiciousReason == null ? "" : "; " + suspiciousReason)));
        return toCheckOutResponse(findSessionById(sessionId), true, request.reason().trim(), "Vehicle checked out by manual override.");
    }

    public synchronized InternalPaymentUpdateResponse updatePaymentStatus(String sessionId, InternalPaymentUpdateRequest request) {
        if (request == null || request.paymentStatus() != PaymentStatus.PAID || isBlank(request.paymentOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slice 4 accepts only PAID payment updates");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.paymentStatus() == PaymentStatus.PAID) {
            if (request.paymentOrderId().equals(session.paymentOrderId())) {
                return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, session.amountPaid(), true);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already paid by another order");
        }
        Instant now = Instant.now();
        repository.update(new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(),
                session.vehicleType(), ParkingSessionStatus.PAID, PaymentStatus.PAID, session.entryTime(), session.entryGate(), session.staffId(),
                session.plateSource(), session.qrLookupToken(), request.paymentOrderId(), request.amountPaid(), request.paidAt(),
                session.exitTime(), session.exitGate(), session.exitPlate(), session.manualOverride(), session.manualOverrideReason(),
                session.suspiciousReason(), session.lastExitPassId(), session.reservationId(), session.reservationCode(), session.ocrRequestId(), session.ocrCandidatePlate(), session.ocrConfidence(), session.createdAt(), now));
        eventRecorder.record(new ParkingSessionEvent("PAYMENT_CONFIRMED", sessionId, "payment-service", now));
        return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, request.amountPaid(), true);
    }

    public synchronized InternalDiscountUpdateResponse updateMerchantDiscount(String sessionId, InternalDiscountUpdateRequest request) {
        if (request == null || request.discountAmount() < 0 || request.totalEligibleInvoiceAmount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount values cannot be negative");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exited session cannot receive merchant discount");
        }
        if (request.discountAmount() > demoFlatFee) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot exceed estimated fee");
        }
        long finalFee = Math.max(demoFlatFee - request.discountAmount(), 0);
        MerchantDiscountState state = new MerchantDiscountState(request.totalEligibleInvoiceAmount(), request.discountAmount(), finalFee,
                request.discountPolicy() == null || request.discountPolicy().isBlank() ? "AGGREGATE_INVOICE" : request.discountPolicy());
        merchantDiscounts.put(sessionId, state);
        eventRecorder.record(new ParkingSessionEvent("MERCHANT_DISCOUNT_UPDATED", sessionId, request.updatedBy(), Instant.now(), "total=" + request.totalEligibleInvoiceAmount()));
        return new InternalDiscountUpdateResponse(sessionId, demoFlatFee, state.discountAmount(), state.finalFee(), state.totalEligibleInvoiceAmount(), true);
    }

    public synchronized OfflineSyncResponse syncOfflineEvents(OfflineSyncRequest request, String syncRequestIdempotencyKey, AuthenticatedUser actor) {
        if (isBlank(syncRequestIdempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        if (request == null || isBlank(request.deviceId()) || request.events() == null || request.events().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId and at least one event are required");
        }
        List<OfflineSyncResult> results = request.events().stream()
                .map(event -> syncOfflineEvent(request.deviceId().trim(), event, syncRequestIdempotencyKey, actor))
                .toList();
        return new OfflineSyncResponse(UUID.randomUUID().toString(), request.deviceId().trim(), results);
    }

    public OfflineEventStatusResponse getOfflineEventStatus(String eventId) {
        OfflineEvent event = offlineEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offline event not found"));
        return new OfflineEventStatusResponse(event.eventId(), event.status(), event.serverSessionId(), event.sessionCode(), event.message());
    }

    private OfflineSyncResult syncOfflineEvent(String deviceId, OfflineSyncEventRequest request, String syncRequestKey, AuthenticatedUser actor) {
        if (request == null || isBlank(request.eventId()) || isBlank(request.idempotencyKey())) {
            String eventId = request == null || request.eventId() == null ? "unknown" : request.eventId();
            return new OfflineSyncResult(eventId, OfflineSyncStatus.REJECTED, null, null, "eventId and idempotencyKey are required.");
        }
        OfflineEvent existing = offlineEventRepository.findByEventId(request.eventId())
                .or(() -> offlineEventRepository.findByEventIdempotencyKey(request.idempotencyKey()))
                .orElse(null);
        if (existing != null) {
            return new OfflineSyncResult(request.eventId(), OfflineSyncStatus.DUPLICATE, existing.serverSessionId(), existing.sessionCode(),
                    "Offline event was already processed: " + existing.status());
        }
        OfflineCheckInPayload payload = request.payload();
        OfflineEventType type;
        try {
            type = OfflineEventType.valueOf(request.eventType() == null ? "" : request.eventType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, null, OfflineSyncStatus.REJECTED, null, null,
                    "Unsupported offline event type.", null);
        }
        if (type != OfflineEventType.OFFLINE_CHECK_IN) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.MANUAL_REVIEW_REQUIRED, null, null,
                    "Offline event requires manual review.", "Only OFFLINE_CHECK_IN is supported in Slice 6.");
        }
        if (payload == null || isBlank(payload.vehiclePlate()) || isBlank(payload.entryGate())) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.REJECTED, null, null,
                    "vehiclePlate and entryGate are required.", null);
        }
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(payload.vehicleType() == null ? "" : payload.vehicleType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.REJECTED, null, null,
                    "vehicleType is invalid.", null);
        }
        String normalizedPlate = normalizePlate(payload.vehiclePlate());
        if (normalizedPlate.isBlank()) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.REJECTED, null, null,
                    "vehiclePlate is required.", null);
        }
        if (repository.existsActiveByNormalizedPlate(normalizedPlate)) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.CONFLICT, null, null,
                    "Offline check-in conflicts with an active server session.", "ACTIVE_SESSION_FOR_NORMALIZED_PLATE");
        }
        Instant now = Instant.now();
        ParkingSession session = new ParkingSession(UUID.randomUUID().toString(), nextSessionCode(now), normalizedPlate, normalizedPlate,
                vehicleType, ParkingSessionStatus.ACTIVE, PaymentStatus.UNPAID, now, payload.entryGate().trim(), actor.id(), PlateSource.MANUAL,
                nextToken(), null, null, null, null, null, null, false, null, null, null, null, null, null, null, null, now, now);
        if (!repository.saveIfNoActive(session)) {
            return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.CONFLICT, null, null,
                    "Offline check-in conflicts with an active server session.", "ACTIVE_SESSION_FOR_NORMALIZED_PLATE");
        }
        eventRecorder.record(new ParkingSessionEvent("OFFLINE_CHECK_IN_SYNCED", session.id(), actor.id(), now, "eventId=" + request.eventId()));
        return persistOfflineResult(request, deviceId, syncRequestKey, actor, payload, OfflineSyncStatus.SYNCED, session.id(), session.sessionCode(),
                "Offline check-in synced successfully.", null);
    }

    private OfflineSyncResult persistOfflineResult(OfflineSyncEventRequest request, String deviceId, String syncRequestKey,
            AuthenticatedUser actor, OfflineCheckInPayload payload, OfflineSyncStatus status, String serverSessionId, String sessionCode,
            String message, String conflictReason) {
        OfflineEvent event = new OfflineEvent(request.eventId(), parseOfflineEventType(request.eventType()), request.idempotencyKey(),
                syncRequestKey, deviceId, actor.username(), request.localTimestamp(), Instant.now(), payload == null ? null : payload.vehiclePlate(),
                payload == null ? null : payload.vehicleType(), payload == null ? null : payload.entryGate(), payload == null ? null : payload.plateSource(),
                status, serverSessionId, sessionCode, message, conflictReason);
        offlineEventRepository.save(event);
        if (status == OfflineSyncStatus.CONFLICT || status == OfflineSyncStatus.REJECTED) {
            eventRecorder.record(new ParkingSessionEvent("OFFLINE_SYNC_" + status, serverSessionId == null ? "offline-event" : serverSessionId,
                    actor.id(), Instant.now(), "eventId=" + request.eventId() + (conflictReason == null ? "" : "; " + conflictReason)));
        }
        return new OfflineSyncResult(request.eventId(), status, serverSessionId, sessionCode, message);
    }

    private OfflineEventType parseOfflineEventType(String eventType) {
        try { return OfflineEventType.valueOf(eventType == null ? "OFFLINE_CHECK_IN" : eventType.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return OfflineEventType.OFFLINE_CHECK_IN; }
    }

    public static String normalizePlate(String plate) { return plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT); }

    private ExitPass requireUsablePass(String token) {
        ExitPass pass = exitPassRepository.findByToken(token)
                .orElseThrow(() -> exitError(HttpStatus.NOT_FOUND, "EXIT_PASS_NOT_FOUND", "Exit Pass was not found"));
        if (pass.status() == ExitPassStatus.USED) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_REUSE_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_ALREADY_USED", "Exit Pass has already been used");
        }
        if (pass.status() == ExitPassStatus.INVALIDATED) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_INVALIDATED_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_INVALIDATED", "Exit Pass is no longer active");
        }
        if (pass.status() == ExitPassStatus.EXPIRED || !pass.expiresAt().isAfter(Instant.now())) {
            exitPassRepository.save(new ExitPass(pass.id(), pass.token(), pass.sessionId(), ExitPassStatus.EXPIRED,
                    pass.createdAt(), pass.expiresAt(), pass.usedAt(), pass.invalidatedAt(), pass.createdFrom(), pass.ttlSeconds()));
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_EXPIRED_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.GONE, "EXIT_PASS_EXPIRED", "Exit Pass has expired");
        }
        return pass;
    }

    private void requireExitEligible(ParkingSession session) {
        if (!isExitEligible(session)) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_NOT_PAID", "Payment is required before vehicle exit");
        }
    }

    private boolean isExitEligible(ParkingSession session) {
        return session.status() != ParkingSessionStatus.EXITED && (session.paymentStatus() == PaymentStatus.PAID || discountFor(session.id()).finalFee() == 0);
    }

    private MerchantDiscountState discountFor(String sessionId) { return merchantDiscounts.getOrDefault(sessionId, new MerchantDiscountState(0, 0, demoFlatFee, "AGGREGATE_INVOICE")); }

    private ParkingSession copyWithExitPass(ParkingSession session, String passId, Instant now) {
        return new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), session.entryGate(), session.staffId(), session.plateSource(),
                session.qrLookupToken(), session.paymentOrderId(), session.amountPaid(), session.paidAt(), session.exitTime(), session.exitGate(),
                session.exitPlate(), session.manualOverride(), session.manualOverrideReason(), session.suspiciousReason(), passId, session.reservationId(), session.reservationCode(), session.ocrRequestId(), session.ocrCandidatePlate(), session.ocrConfidence(), session.createdAt(), now);
    }

    private ParkingSession copyExited(ParkingSession session, String exitGate, String exitPlate, boolean manualOverride,
            String manualOverrideReason, String suspiciousReason, Instant now) {
        return new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                ParkingSessionStatus.EXITED, session.paymentStatus(), session.entryTime(), session.entryGate(), session.staffId(), session.plateSource(),
                session.qrLookupToken(), session.paymentOrderId(), session.amountPaid(), session.paidAt(), now, exitGate.trim(), exitPlate,
                manualOverride, manualOverrideReason, suspiciousReason, session.lastExitPassId(), session.reservationId(), session.reservationCode(), session.ocrRequestId(), session.ocrCandidatePlate(), session.ocrConfidence(), session.createdAt(), now);
    }

    private CheckOutResponse toCheckOutResponse(ParkingSession session, boolean manualOverride, String reason, String message) {
        return new CheckOutResponse(session.id(), session.sessionCode(), session.status().name(), session.paymentStatus(), session.entryTime(),
                session.exitTime(), session.exitGate(), session.exitPlate(), manualOverride, reason, message);
    }

    private ParkingSession findSessionById(String sessionId) {
        return repository.findById(sessionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking session not found"));
    }

    private ParkingSessionStatus parseStatus(String status) {
        if (isBlank(status)) return null;
        try { return ParkingSessionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parking session status"); }
    }

    private String nextSessionCode(Instant now) {
        return "PF-" + LocalDate.ofInstant(now, ZoneOffset.UTC).toString().replace("-", "") + "-" + String.format("%06d", sequence.incrementAndGet());
    }

    private String nextToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ParkingSessionResponse toStaffResponse(ParkingSession session) {
        return new ParkingSessionResponse(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), session.entryGate(), session.qrLookupToken(), session.reservationId(), session.reservationCode(), session.reservationId() != null, session.ocrRequestId(), session.ocrCandidatePlate(), session.ocrConfidence(),
                publicTicketBaseUrl + "/api/public/tickets/" + session.qrLookupToken());
    }

    private String consumeReservation(String reservationCode, String vehiclePlate, VehicleType vehicleType, String sessionId) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); headers.set("X-Internal-Service-Token", internalServiceToken);
        String body = "{\"vehiclePlate\":\"" + vehiclePlate.replace("\\\"", "") + "\",\"vehicleType\":\"" + vehicleType.name() + "\",\"parkingSessionId\":\"" + sessionId + "\",\"checkInRequestId\":\"" + UUID.randomUUID() + "\"}";
        try {
            var response = restTemplate.exchange(reservationServiceBaseUrl + "/internal/reservations/" + reservationCode + "/consume", HttpMethod.POST, new HttpEntity<>(body, headers), java.util.Map.class);
            Object id = response.getBody() == null ? null : response.getBody().get("reservationId");
            if (id == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reservation service returned an invalid consume response");
            return id.toString();
        } catch (HttpStatusCodeException exception) { throw new ResponseStatusException(exception.getStatusCode(), "Reservation validation failed"); }
          catch (org.springframework.web.client.ResourceAccessException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reservation service unavailable"); }
    }

    private ExitPassException exitError(HttpStatus status, String code, String message) { return new ExitPassException(status, code, message); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
