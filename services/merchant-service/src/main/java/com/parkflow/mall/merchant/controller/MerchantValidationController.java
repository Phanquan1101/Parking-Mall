package com.parkflow.mall.merchant.controller;

import com.parkflow.mall.merchant.dto.*;
import com.parkflow.mall.merchant.security.MerchantUser;
import com.parkflow.mall.merchant.service.MerchantValidationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class MerchantValidationController {

    private final MerchantValidationService service;

    public MerchantValidationController(MerchantValidationService s) {
        service = s;
    }

    @PostMapping("/api/merchant/validations")
    public InvoiceValidationResponse validate(
            @RequestBody ValidateInvoiceRequest req,
            @AuthenticationPrincipal MerchantUser user) {

        return service.validate(req, user);
    }

    @GetMapping("/api/merchant/validations")
    public ValidationHistoryResponse history(
            @RequestParam String sessionId,
            @AuthenticationPrincipal MerchantUser user) {

        return service.history(sessionId, user);
    }
}