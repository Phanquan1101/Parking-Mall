package com.parkflow.mall.gateway.reservation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ReservationServiceProxy {

 private final RestTemplate rest;
 private final String base;

 public ReservationServiceProxy(
         RestTemplate rest,
         @Value("${gateway.reservation-base-url}") String base) {

  this.rest = rest;
  this.base = base;
 }

 public ResponseEntity<String> forward(
         String path,
         HttpMethod method,
         String body,
         String auth) {

  HttpHeaders h = new HttpHeaders();

  if (auth != null) {
   h.set(HttpHeaders.AUTHORIZATION, auth);
  }

  if (body != null) {
   h.setContentType(MediaType.APPLICATION_JSON);
  }

  try {
   var r = rest.exchange(
           base + path,
           method,
           new HttpEntity<>(body, h),
           String.class
   );

   return ResponseEntity
           .status(r.getStatusCode())
           .contentType(MediaType.APPLICATION_JSON)
           .body(r.getBody());

  } catch (HttpStatusCodeException e) {

   return ResponseEntity
           .status(e.getStatusCode())
           .contentType(MediaType.APPLICATION_JSON)
           .body(e.getResponseBodyAsString());

  } catch (ResourceAccessException e) {

   return ResponseEntity
           .status(HttpStatus.BAD_GATEWAY)
           .contentType(MediaType.APPLICATION_JSON)
           .body("{\"error\":\"reservation-service unavailable\"}");
  }
 }

 public String listPath(
         String status,
         String plate) {

  return UriComponentsBuilder
          .fromPath("/api/reservations")
          .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
          .queryParamIfPresent("vehiclePlate", java.util.Optional.ofNullable(plate))
          .build()
          .encode()
          .toUriString();
 }
}