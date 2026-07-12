package com.parkflow.mall.gateway.controller;

import com.parkflow.mall.gateway.parking.ParkingServiceProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingGatewayController {
    private final ParkingServiceProxy parkingServiceProxy;

    public ParkingGatewayController(ParkingServiceProxy parkingServiceProxy) {
        this.parkingServiceProxy = parkingServiceProxy;
    }

    @PostMapping("/api/parking/sessions/check-in")
    public ResponseEntity<String> checkIn(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String requestBody) {
        return parkingServiceProxy.checkIn(authorization, requestBody);
    }

    @GetMapping("/api/parking/sessions/{sessionId}")
    public ResponseEntity<String> getSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String sessionId) {
        return parkingServiceProxy.getSession(authorization, sessionId);
    }

    @GetMapping("/api/parking/sessions")
    public ResponseEntity<String> search(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plate) {
        return parkingServiceProxy.search(authorization, status, plate);
    }

    @GetMapping("/api/public/tickets/{lookupToken}")
    public ResponseEntity<String> publicTicket(@PathVariable String lookupToken) {
        return parkingServiceProxy.publicTicket(lookupToken);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/exit-passes")
    public ResponseEntity<String> generateExitPass(@PathVariable String sessionId, @RequestBody String requestBody) {
        return parkingServiceProxy.generateExitPass(sessionId, requestBody);
    }

    @PostMapping("/api/parking/exit-passes/{exitPassToken}/validate")
    public ResponseEntity<String> validateExitPass(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String exitPassToken,
            @RequestBody String requestBody) {
        return parkingServiceProxy.validateExitPass(authorization, exitPassToken, requestBody);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/check-out")
    public ResponseEntity<String> checkOut(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String sessionId,
            @RequestBody String requestBody) {
        return parkingServiceProxy.checkOut(authorization, sessionId, requestBody);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/manual-override")
    public ResponseEntity<String> manualOverride(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String sessionId,
            @RequestBody String requestBody) {
        return parkingServiceProxy.manualOverride(authorization, sessionId, requestBody);
    }

    @PostMapping("/api/parking/offline-sync")
    public ResponseEntity<String> syncOfflineEvents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody String requestBody) {
        return parkingServiceProxy.syncOfflineEvents(authorization, idempotencyKey, requestBody);
    }

    @GetMapping("/api/parking/offline-sync/{eventId}")
    public ResponseEntity<String> getOfflineEventStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String eventId) {
        return parkingServiceProxy.getOfflineEventStatus(authorization, eventId);
    }
}
