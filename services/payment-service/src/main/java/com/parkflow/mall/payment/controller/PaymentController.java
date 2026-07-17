package com.parkflow.mall.payment.controller;

import com.parkflow.mall.payment.dto.PaymentDtos.*;
import com.parkflow.mall.payment.service.PaymentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    @Value("${app.jwt-secret:parkflow-local-development-secret-change-me-2026}")
    String jwtSecret;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public OrderResponse create(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key
    ) {
        return service.create(request, key);
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/simulations/success")
    public SimulationResponse simulate(
            @RequestBody SimulateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key
    ) {
        return service.simulate(request, key);
    }

    @PostMapping("/reconciliation/run")
    public Map<String, Object> reconcile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        admin(authorization);

        return service.reconcile(
                !Boolean.FALSE.equals(request == null ? null : request.get("includeExpiredPending")),
                !Boolean.FALSE.equals(request == null ? null : request.get("retryParkingUpdates"))
        );
    }

    @GetMapping("/reconciliation/items")
    public List<Map<String, Object>> items(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        admin(authorization);
        return service.listItems();
    }

    @GetMapping("/reconciliation/items/{id}")
    public Map<String, Object> item(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id
    ) {
        admin(authorization);
        return service.item(id);
    }

    private void admin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin token required"
            );
        }

        try {
            Object roles = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(authorization.substring(7))
                    .getPayload()
                    .get("roles");

            if (!(roles instanceof List<?> list)
                    || list.stream().noneMatch(x -> "ADMIN".equals(x.toString()))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "ADMIN role required"
                );
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid admin token"
            );
        }
    }
}