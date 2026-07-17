package com.parkflow.mall.gateway.controller;

import com.parkflow.mall.gateway.payment.PaymentServiceProxy;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentGatewayController {

    private final PaymentServiceProxy p;

    public PaymentGatewayController(PaymentServiceProxy p) {
        this.p = p;
    }

    @PostMapping("/api/payments/orders")
    public ResponseEntity<String> create(
            @RequestBody String b,
            @RequestHeader(value = "Authorization", required = false) String a,
            @RequestHeader(value = "Idempotency-Key", required = false) String k) {

        return p.forward(
                "/api/payments/orders",
                HttpMethod.POST,
                b,
                a,
                k
        );
    }

    @GetMapping("/api/payments/orders/{id}")
    public ResponseEntity<String> get(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String a) {

        return p.forward(
                "/api/payments/orders/" + id,
                HttpMethod.GET,
                null,
                a,
                null
        );
    }

    @PostMapping("/api/payments/simulations/success")
    public ResponseEntity<String> simulate(
            @RequestBody String b,
            @RequestHeader(value = "Authorization", required = false) String a,
            @RequestHeader(value = "Idempotency-Key", required = false) String k) {

        return p.forward(
                "/api/payments/simulations/success",
                HttpMethod.POST,
                b,
                a,
                k
        );
    }

    @PostMapping("/api/payments/reconciliation/run")
    public ResponseEntity<String> run(
            @RequestBody String b,
            @RequestHeader(value = "Authorization", required = false) String a,
            @RequestHeader(value = "Idempotency-Key", required = false) String k) {

        return p.forward(
                "/api/payments/reconciliation/run",
                HttpMethod.POST,
                b,
                a,
                k
        );
    }

    @GetMapping("/api/payments/reconciliation/items")
    public ResponseEntity<String> items(
            @RequestHeader(value = "Authorization", required = false) String a,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentOrderId,
            @RequestParam(required = false) String targetId) {

        String q = "";

        if (status != null) {
            q += "?status=" + status;
        }

        if (paymentOrderId != null) {
            q += (q.isEmpty() ? "?" : "&") + "paymentOrderId=" + paymentOrderId;
        }

        if (targetId != null) {
            q += (q.isEmpty() ? "?" : "&") + "targetId=" + targetId;
        }

        return p.forward(
                "/api/payments/reconciliation/items" + q,
                HttpMethod.GET,
                null,
                a,
                null
        );
    }

    @GetMapping("/api/payments/reconciliation/items/{id}")
    public ResponseEntity<String> item(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String a) {

        return p.forward(
                "/api/payments/reconciliation/items/" + id,
                HttpMethod.GET,
                null,
                a,
                null
        );
    }
}