package com.parkflow.mall.reservation.service;

import com.parkflow.mall.reservation.dto.CancelReservationRequest;
import com.parkflow.mall.reservation.dto.ConsumeReservationRequest;
import com.parkflow.mall.reservation.dto.CreateReservationRequest;
import com.parkflow.mall.reservation.dto.ExpireReservationsResponse;
import com.parkflow.mall.reservation.dto.ReservationResponse;
import com.parkflow.mall.reservation.model.Reservation;
import com.parkflow.mall.reservation.model.ReservationStatus;
import com.parkflow.mall.reservation.model.VehicleType;
import com.parkflow.mall.reservation.repository.ReservationRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {

 private final ReservationRepository repository;
 private final int motorbikeCapacity;
 private final int carCapacity;
 private final int holdMinutes;
 private final SecureRandom random = new SecureRandom();

 public ReservationService(
         ReservationRepository repository,
         @Value("${app.motorbike-capacity:20}") int motorbikeCapacity,
         @Value("${app.car-capacity:10}") int carCapacity,
         @Value("${app.default-hold-minutes:60}") int holdMinutes
 ) {
  this.repository = repository;
  this.motorbikeCapacity = motorbikeCapacity;
  this.carCapacity = carCapacity;
  this.holdMinutes = holdMinutes;
 }

 public synchronized ReservationResponse create(
         CreateReservationRequest request
 ) {
  expireStale();

  if (request == null || blank(request.vehiclePlate())) {
   bad("vehiclePlate is required");
  }

  if (request.vehicleType() == null) {
   bad("vehicleType is required");
  }

  if (request.reservedFrom() == null
          || request.reservedUntil() == null
          || !request.reservedUntil().isAfter(request.reservedFrom())) {
   bad("reservedUntil must be after reservedFrom");
  }

  if (request.reservedFrom().isBefore(Instant.now().minusSeconds(300))) {
   bad("reservedFrom cannot be too far in the past");
  }

  String plate = normalize(request.vehiclePlate());

  if (repository.hasActivePlate(plate)) {
   conflict("ACTIVE_RESERVATION_EXISTS");
  }

  if (repository.activeCountByVehicleType(request.vehicleType())
          >= capacity(request.vehicleType())) {
   conflict("RESERVATION_CAPACITY_FULL");
  }

  Instant now = Instant.now();

  Instant expiresAt =
          request.reservedUntil().isBefore(
                  now.plusSeconds((long) holdMinutes * 60)
          )
                  ? request.reservedUntil()
                  : now.plusSeconds((long) holdMinutes * 60);

  Reservation reservation = new Reservation(
          UUID.randomUUID().toString(),
          code(),
          request.vehiclePlate().trim(),
          plate,
          request.vehicleType(),
          request.customerName(),
          request.customerPhone(),
          request.reservedFrom(),
          request.reservedUntil(),
          expiresAt,
          ReservationStatus.RESERVED,
          now,
          null,
          null,
          null,
          null,
          null
  );

  repository.save(reservation);

  return response(
          reservation,
          "Reservation created successfully."
  );
 }

 public synchronized ReservationResponse get(String code) {
  expireStale();
  return response(require(code), null);
 }

 public synchronized ReservationResponse cancel(
         String code,
         CancelReservationRequest request
 ) {
  Reservation reservation = require(code);

  expireIfNeeded(reservation);

  reservation = require(code);

  if (reservation.status() == ReservationStatus.CANCELLED) {
   return response(
           reservation,
           "Reservation cancelled successfully."
   );
  }

  if (reservation.status() != ReservationStatus.RESERVED) {
   conflict(
           "Reservation cannot be cancelled in its current status"
   );
  }

  Reservation updated = new Reservation(
          reservation.id(),
          reservation.reservationCode(),
          reservation.vehiclePlate(),
          reservation.normalizedVehiclePlate(),
          reservation.vehicleType(),
          reservation.customerName(),
          reservation.customerPhone(),
          reservation.reservedFrom(),
          reservation.reservedUntil(),
          reservation.expiresAt(),
          ReservationStatus.CANCELLED,
          reservation.createdAt(),
          Instant.now(),
          request == null ? null : request.reason(),
          null,
          null,
          null
  );

  repository.save(updated);

  return response(
          updated,
          "Reservation cancelled successfully."
  );
 }

 public synchronized List<ReservationResponse> list(
         String status,
         String vehiclePlate
 ) {
  expireStale();

  ReservationStatus wanted = null;

  if (!blank(status)) {
   try {
    wanted = ReservationStatus.valueOf(
            status.trim().toUpperCase(Locale.ROOT)
    );
   } catch (IllegalArgumentException exception) {
    bad("Unknown reservation status");
   }
  }

  String plate = blank(vehiclePlate)
          ? null
          : normalize(vehiclePlate);

  ReservationStatus finalWanted = wanted;

  return repository.findAll()
          .stream()
          .filter(reservation ->
                  finalWanted == null
                          || reservation.status() == finalWanted
          )
          .filter(reservation ->
                  plate == null
                          || reservation.normalizedVehiclePlate()
                          .equals(plate)
          )
          .map(reservation -> response(reservation, null))
          .toList();
 }

 public synchronized ExpireReservationsResponse expire() {
  return new ExpireReservationsResponse(
          expireStale(),
          "Expired stale reservations."
  );
 }

 public synchronized ReservationResponse consume(
         String code,
         ConsumeReservationRequest request
 ) {
  if (request == null
          || blank(request.vehiclePlate())
          || request.vehicleType() == null
          || blank(request.parkingSessionId())
          || blank(request.checkInRequestId())) {
   bad(
           "vehiclePlate, vehicleType, parkingSessionId "
                   + "and checkInRequestId are required"
   );
  }

  Reservation reservation = require(code);

  expireIfNeeded(reservation);

  reservation = require(code);

  if (reservation.status() == ReservationStatus.CONSUMED) {
   if (request.checkInRequestId()
           .equals(reservation.consumeRequestId())) {
    return response(
            reservation,
            "Reservation already consumed for this check-in."
    );
   }

   conflict("Reservation has already been consumed");
  }

  if (reservation.status() != ReservationStatus.RESERVED) {
   conflict("Reservation is not available for check-in");
  }

  if (!reservation.normalizedVehiclePlate()
          .equals(normalize(request.vehiclePlate()))) {
   conflict(
           "Reservation plate does not match check-in plate"
   );
  }

  if (reservation.vehicleType() != request.vehicleType()) {
   conflict("Reservation vehicle type does not match");
  }

  Instant now = Instant.now();

  Reservation updated = new Reservation(
          reservation.id(),
          reservation.reservationCode(),
          reservation.vehiclePlate(),
          reservation.normalizedVehiclePlate(),
          reservation.vehicleType(),
          reservation.customerName(),
          reservation.customerPhone(),
          reservation.reservedFrom(),
          reservation.reservedUntil(),
          reservation.expiresAt(),
          ReservationStatus.CONSUMED,
          reservation.createdAt(),
          null,
          null,
          now,
          request.parkingSessionId(),
          request.checkInRequestId()
  );

  repository.save(updated);

  return response(
          updated,
          "Reservation consumed successfully."
  );
 }

 private int expireStale() {
  int count = 0;

  for (Reservation reservation : repository.findAll()) {
   if (reservation.status() == ReservationStatus.RESERVED
           && expired(reservation)) {
    repository.save(
            new Reservation(
                    reservation.id(),
                    reservation.reservationCode(),
                    reservation.vehiclePlate(),
                    reservation.normalizedVehiclePlate(),
                    reservation.vehicleType(),
                    reservation.customerName(),
                    reservation.customerPhone(),
                    reservation.reservedFrom(),
                    reservation.reservedUntil(),
                    reservation.expiresAt(),
                    ReservationStatus.EXPIRED,
                    reservation.createdAt(),
                    null,
                    null,
                    null,
                    null,
                    null
            )
    );

    count++;
   }
  }

  return count;
 }

 private void expireIfNeeded(Reservation reservation) {
  if (reservation.status() == ReservationStatus.RESERVED
          && expired(reservation)) {
   expireStale();
  }
 }

 private boolean expired(Reservation reservation) {
  Instant now = Instant.now();

  return !now.isBefore(reservation.reservedUntil())
          || !now.isBefore(reservation.expiresAt());
 }

 private Reservation require(String code) {
  return repository.findByCode(code)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "Reservation not found"
                  )
          );
 }

 private ReservationResponse response(
         Reservation reservation,
         String message
 ) {
  return new ReservationResponse(
          reservation.id(),
          reservation.reservationCode(),
          reservation.vehiclePlate(),
          reservation.vehicleType(),
          reservation.reservedFrom(),
          reservation.reservedUntil(),
          reservation.expiresAt(),
          reservation.status(),
          message
  );
 }

 private int capacity(VehicleType type) {
  return type == VehicleType.CAR
          ? carCapacity
          : motorbikeCapacity;
 }

 private String code() {
  byte[] bytes = new byte[18];
  random.nextBytes(bytes);

  return "RSV-"
          + Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(bytes);
 }

 private String normalize(String plate) {
  return plate.replaceAll("[^A-Za-z0-9]", "")
          .toUpperCase(Locale.ROOT);
 }

 private boolean blank(String value) {
  return value == null || value.isBlank();
 }

 private void bad(String message) {
  throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          message
  );
 }

 private void conflict(String message) {
  throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          message
  );
 }
}