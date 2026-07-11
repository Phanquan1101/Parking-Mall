package com.parkflow.mall.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class IdentityServiceProxy {
    private final RestTemplate restTemplate;
    private final String identityBaseUrl;

    public IdentityServiceProxy(RestTemplate restTemplate, @Value("${gateway.identity-base-url}") String identityBaseUrl) {
        this.restTemplate = restTemplate;
        this.identityBaseUrl = identityBaseUrl;
    }

    public ResponseEntity<String> login(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/auth/login", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> me(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return forward("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers));
    }

    public ResponseEntity<String> health() {
        return forward("/actuator/health", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()));
    }

    private ResponseEntity<String> forward(String path, HttpMethod method, HttpEntity<String> request) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(identityBaseUrl + path, method, request, String.class);
            return withJsonContentType(response.getStatusCode(), response.getBody());
        } catch (HttpStatusCodeException exception) {
            return withJsonContentType(exception.getStatusCode(), exception.getResponseBodyAsString());
        } catch (ResourceAccessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"identity-service unavailable\"}");
        }
    }

    private ResponseEntity<String> withJsonContentType(org.springframework.http.HttpStatusCode status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
