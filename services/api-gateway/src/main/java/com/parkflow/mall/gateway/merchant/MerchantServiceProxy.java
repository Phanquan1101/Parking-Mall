package com.parkflow.mall.gateway.merchant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MerchantServiceProxy {

    private final RestTemplate rest;
    private final String base;

    public MerchantServiceProxy(
            RestTemplate r,
            @Value("${gateway.merchant-base-url}") String b) {

        rest = r;
        base = b;
    }

    public ResponseEntity<String> validate(
            String auth,
            String body) {

        return forward(
                base + "/api/merchant/validations",
                HttpMethod.POST,
                auth,
                body
        );
    }

    public ResponseEntity<String> history(
            String auth,
            String id) {

        return forward(
                UriComponentsBuilder
                        .fromHttpUrl(base + "/api/merchant/validations")
                        .queryParam("sessionId", id)
                        .toUriString(),
                HttpMethod.GET,
                auth,
                null
        );
    }

    private ResponseEntity<String> forward(
            String url,
            HttpMethod m,
            String auth,
            String b) {

        HttpHeaders h = new HttpHeaders();

        if (auth != null) {
            h.set(HttpHeaders.AUTHORIZATION, auth);
        }

        if (b != null) {
            h.setContentType(MediaType.APPLICATION_JSON);
        }

        try {
            var r = rest.exchange(
                    url,
                    m,
                    new HttpEntity<>(b, h),
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
                    .body("{\"error\":\"merchant-service unavailable\"}");
        }
    }
}