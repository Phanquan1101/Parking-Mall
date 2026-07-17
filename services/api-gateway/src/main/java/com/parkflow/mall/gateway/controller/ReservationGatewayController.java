package com.parkflow.mall.gateway.controller;

import com.parkflow.mall.gateway.reservation.ReservationServiceProxy;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReservationGatewayController {

 private final ReservationServiceProxy proxy;

 public ReservationGatewayController(ReservationServiceProxy proxy) {
  this.proxy = proxy;
 }

 @PostMapping("/api/reservations")
 public ResponseEntity<String> create(
         @RequestBody String body) {

  return proxy.forward(
          "/api/reservations",
          HttpMethod.POST,
          body,
          null
  );
 }

 @GetMapping("/api/reservations/{code}")
 public ResponseEntity<String> get(
         @PathVariable String code) {

  return proxy.forward(
          "/api/reservations/" + code,
          HttpMethod.GET,
          null,
          null
  );
 }

 @PostMapping("/api/reservations/{code}/cancel")
 public ResponseEntity<String> cancel(
         @PathVariable String code,
         @RequestBody(required = false) String body) {

  return proxy.forward(
          "/api/reservations/" + code + "/cancel",
          HttpMethod.POST,
          body,
          null
  );
 }

 @GetMapping("/api/reservations")
 public ResponseEntity<String> list(
         @RequestHeader(value = "Authorization", required = false) String auth,
         @RequestParam(required = false) String status,
         @RequestParam(required = false) String vehiclePlate) {

  return proxy.forward(
          proxy.listPath(status, vehiclePlate),
          HttpMethod.GET,
          null,
          auth
  );
 }

 @PostMapping("/api/reservations/expire")
 public ResponseEntity<String> expire(
         @RequestHeader(value = "Authorization", required = false) String auth) {

  return proxy.forward(
          "/api/reservations/expire",
          HttpMethod.POST,
          null,
          auth
  );
 }
}