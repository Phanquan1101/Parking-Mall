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
}
