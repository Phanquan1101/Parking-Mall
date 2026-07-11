package com.parkflow.mall.gateway.controller;

import com.parkflow.mall.gateway.service.IdentityServiceProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthGatewayController {
    private final IdentityServiceProxy identityServiceProxy;

    public AuthGatewayController(IdentityServiceProxy identityServiceProxy) {
        this.identityServiceProxy = identityServiceProxy;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<String> login(@RequestBody String requestBody) {
        return identityServiceProxy.login(requestBody);
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<String> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return identityServiceProxy.me(authorization);
    }

    @GetMapping("/identity/health")
    public ResponseEntity<String> identityHealth() {
        return identityServiceProxy.health();
    }
}
