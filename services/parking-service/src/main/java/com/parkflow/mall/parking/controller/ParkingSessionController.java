package com.parkflow.mall.parking.controller;

import com.parkflow.mall.parking.dto.CheckInRequest;
import com.parkflow.mall.parking.dto.ParkingSessionResponse;
import com.parkflow.mall.parking.dto.PublicTicketResponse;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateResponse;
import com.parkflow.mall.parking.dto.GenerateExitPassRequest;
import com.parkflow.mall.parking.dto.ExitPassResponse;
import com.parkflow.mall.parking.dto.ValidateExitPassRequest;
import com.parkflow.mall.parking.dto.ValidateExitPassResponse;
import com.parkflow.mall.parking.dto.CheckOutRequest;
import com.parkflow.mall.parking.dto.CheckOutResponse;
import com.parkflow.mall.parking.dto.ManualOverrideRequest;
import com.parkflow.mall.parking.security.AuthenticatedUser;
import com.parkflow.mall.parking.service.ParkingSessionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class ParkingSessionController {
    private final ParkingSessionService parkingSessionService;
    private final String internalServiceToken;

    public ParkingSessionController(ParkingSessionService parkingSessionService, @Value("${app.internal-service-token}") String internalServiceToken) {
        this.parkingSessionService = parkingSessionService;
        this.internalServiceToken = internalServiceToken;
    }

    @PostMapping("/api/parking/sessions/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ParkingSessionResponse checkIn(
            @RequestBody CheckInRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return parkingSessionService.checkIn(request, actor);
    }

    @GetMapping("/api/parking/sessions/{sessionId}")
    public ParkingSessionResponse getById(@PathVariable String sessionId) {
        return parkingSessionService.getById(sessionId);
    }

    @GetMapping("/api/parking/sessions")
    public List<ParkingSessionResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plate) {
        return parkingSessionService.search(status, plate);
    }

    @GetMapping("/api/public/tickets/{lookupToken}")
    public PublicTicketResponse publicTicket(@PathVariable String lookupToken) {
        return parkingSessionService.publicTicket(lookupToken);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/exit-passes")
    @ResponseStatus(HttpStatus.CREATED)
    public ExitPassResponse generateExitPass(@PathVariable String sessionId, @RequestBody GenerateExitPassRequest request) {
        return parkingSessionService.generateExitPass(sessionId, request);
    }

    @PostMapping("/api/parking/exit-passes/{exitPassToken}/validate")
    public ValidateExitPassResponse validateExitPass(
            @PathVariable String exitPassToken,
            @RequestBody ValidateExitPassRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return parkingSessionService.validateExitPass(exitPassToken, request, actor);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/check-out")
    public CheckOutResponse checkOut(
            @PathVariable String sessionId,
            @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return parkingSessionService.checkOut(sessionId, request, actor);
    }

    @PostMapping("/api/parking/sessions/{sessionId}/manual-override")
    public CheckOutResponse manualOverride(
            @PathVariable String sessionId,
            @RequestBody ManualOverrideRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return parkingSessionService.manualOverride(sessionId, request, actor);
    }

    @PostMapping("/internal/parking/sessions/{sessionId}/payment-status")
    public InternalPaymentUpdateResponse updatePayment(@PathVariable String sessionId, @RequestHeader("X-Internal-Service-Token") String token, @RequestBody InternalPaymentUpdateRequest request) {
        if (!internalServiceToken.equals(token)) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid internal service token"); }
        return parkingSessionService.updatePaymentStatus(sessionId, request);
    }
}
