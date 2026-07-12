package com.parkflow.mall.gateway.parking;

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
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ParkingServiceProxy {
    private final RestTemplate restTemplate;
    private final String parkingBaseUrl;

    public ParkingServiceProxy(RestTemplate restTemplate, @Value("${gateway.parking-base-url}") String parkingBaseUrl) {
        this.restTemplate = restTemplate;
        this.parkingBaseUrl = parkingBaseUrl;
    }

    public ResponseEntity<String> checkIn(String authorization, String requestBody) {
        HttpHeaders headers = headersWithAuthorization(authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/parking/sessions/check-in", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> getSession(String authorization, String sessionId) {
        return forward("/api/parking/sessions/" + sessionId, HttpMethod.GET, new HttpEntity<>(headersWithAuthorization(authorization)));
    }

    public ResponseEntity<String> search(String authorization, String status, String plate) {
        String url = UriComponentsBuilder.fromHttpUrl(parkingBaseUrl + "/api/parking/sessions")
                .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
                .queryParamIfPresent("plate", java.util.Optional.ofNullable(plate))
                .toUriString();
        return forwardUrl(url, HttpMethod.GET, new HttpEntity<>(headersWithAuthorization(authorization)));
    }

    public ResponseEntity<String> publicTicket(String lookupToken) {
        return forward("/api/public/tickets/" + lookupToken, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()));
    }

    public ResponseEntity<String> generateExitPass(String sessionId, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/parking/sessions/" + sessionId + "/exit-passes", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> validateExitPass(String authorization, String exitPassToken, String requestBody) {
        HttpHeaders headers = headersWithAuthorization(authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/parking/exit-passes/" + exitPassToken + "/validate", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> checkOut(String authorization, String sessionId, String requestBody) {
        HttpHeaders headers = headersWithAuthorization(authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/parking/sessions/" + sessionId + "/check-out", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> manualOverride(String authorization, String sessionId, String requestBody) {
        HttpHeaders headers = headersWithAuthorization(authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return forward("/api/parking/sessions/" + sessionId + "/manual-override", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> syncOfflineEvents(String authorization, String idempotencyKey, String requestBody) {
        HttpHeaders headers = headersWithAuthorization(authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) headers.set("Idempotency-Key", idempotencyKey);
        return forward("/api/parking/offline-sync", HttpMethod.POST, new HttpEntity<>(requestBody, headers));
    }

    public ResponseEntity<String> getOfflineEventStatus(String authorization, String eventId) {
        return forward("/api/parking/offline-sync/" + eventId, HttpMethod.GET, new HttpEntity<>(headersWithAuthorization(authorization)));
    }

    private HttpHeaders headersWithAuthorization(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return headers;
    }

    private ResponseEntity<String> forward(String path, HttpMethod method, HttpEntity<String> request) {
        return forwardUrl(parkingBaseUrl + path, method, request);
    }

    private ResponseEntity<String> forwardUrl(String url, HttpMethod method, HttpEntity<String> request) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, request, String.class);
            return jsonResponse(response.getStatusCode(), response.getBody());
        } catch (HttpStatusCodeException exception) {
            return jsonResponse(exception.getStatusCode(), exception.getResponseBodyAsString());
        } catch (ResourceAccessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"parking-service unavailable\"}");
        }
    }

    private ResponseEntity<String> jsonResponse(org.springframework.http.HttpStatusCode status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
