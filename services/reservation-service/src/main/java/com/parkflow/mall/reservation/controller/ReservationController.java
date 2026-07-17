package com.parkflow.mall.reservation.controller;

import com.parkflow.mall.reservation.dto.CancelReservationRequest;
import com.parkflow.mall.reservation.dto.ConsumeReservationRequest;
import com.parkflow.mall.reservation.dto.CreateReservationRequest;
import com.parkflow.mall.reservation.dto.ExpireReservationsResponse;
import com.parkflow.mall.reservation.dto.ReservationResponse;
import com.parkflow.mall.reservation.service.ReservationService;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReservationController {

 private final ReservationService service;
 private final String internalToken;

 public ReservationController(
         ReservationService service,
         @Value("${app.internal-service-token}") String internalToken
 ) {
  this.service = service;
  this.internalToken = internalToken;
 }

 @PostMapping("/api/reservations")
 @ResponseStatus(HttpStatus.CREATED)
 public ReservationResponse create(
         @RequestBody CreateReservationRequest request
 ) {
  return service.create(request);
 }

 @GetMapping("/api/reservations/{reservationCode}")
 public ReservationResponse get(
         @PathVariable String reservationCode
 ) {
  return service.get(reservationCode);
 }

 @PostMapping("/api/reservations/{reservationCode}/cancel")
 public ReservationResponse cancel(
         @PathVariable String reservationCode,
         @RequestBody(required = false) CancelReservationRequest request
 ) {
  return service.cancel(reservationCode, request);
 }

 @GetMapping("/api/reservations")
 public List<ReservationResponse> list(
         @RequestParam(required = false) String status,
         @RequestParam(required = false) String vehiclePlate
 ) {
  return service.list(status, vehiclePlate);
 }

 @PostMapping("/api/reservations/expire")
 public ExpireReservationsResponse expire() {
  return service.expire();
 }

 @PostMapping("/internal/reservations/{reservationCode}/consume")
 public ReservationResponse consume(
         @PathVariable String reservationCode,
         @RequestHeader(
                 value = "X-Internal-Service-Token",
                 required = false
         ) String token,
         @RequestBody ConsumeReservationRequest request
 ) {
  if (!internalToken.equals(token)) {
   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Invalid internal service token"
   );
  }

  return service.consume(reservationCode, request);
 }
}