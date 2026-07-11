package com.parkflow.mall.parking.controller;

import com.parkflow.mall.parking.dto.CheckInRequest;
import com.parkflow.mall.parking.dto.ParkingSessionResponse;
import com.parkflow.mall.parking.dto.PublicTicketResponse;
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

@RestController
public class ParkingSessionController {
    private final ParkingSessionService parkingSessionService;

    public ParkingSessionController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
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
}
