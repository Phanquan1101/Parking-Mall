package com.parkflow.mall.parking.service;

import com.parkflow.mall.parking.dto.CheckInRequest;
import com.parkflow.mall.parking.dto.ParkingSessionResponse;
import com.parkflow.mall.parking.dto.PublicTicketResponse;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateResponse;
import com.parkflow.mall.parking.model.ParkingSession;
import com.parkflow.mall.parking.model.ParkingSessionEvent;
import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import com.parkflow.mall.parking.model.PlateSource;
import com.parkflow.mall.parking.repository.ParkingSessionRepository;
import com.parkflow.mall.parking.security.AuthenticatedUser;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParkingSessionService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private final ParkingSessionRepository repository;
    private final ParkingSessionEventRecorder eventRecorder;
    private final String publicTicketBaseUrl;
    private final long demoFlatFee;
    private final AtomicLong sequence = new AtomicLong();

    public ParkingSessionService(
            ParkingSessionRepository repository,
            ParkingSessionEventRecorder eventRecorder,
            @Value("${app.public-ticket-base-url}") String publicTicketBaseUrl,
            @Value("${app.demo-flat-fee:5000}") long demoFlatFee) {
        this.repository = repository;
        this.eventRecorder = eventRecorder;
        this.publicTicketBaseUrl = publicTicketBaseUrl.replaceAll("/$", "");
        this.demoFlatFee = demoFlatFee;
    }

    public ParkingSessionResponse checkIn(CheckInRequest request, AuthenticatedUser actor) {
        if (request == null || request.vehiclePlate() == null || request.vehiclePlate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehiclePlate is required");
        }
        if (request.vehicleType() == null || request.entryGate() == null || request.entryGate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleType and entryGate are required");
        }
        if (request.plateSource() != PlateSource.MANUAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slice 2 requires MANUAL plateSource");
        }

        String normalizedPlate = normalizePlate(request.vehiclePlate());
        if (normalizedPlate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehiclePlate is required");
        }

        Instant now = Instant.now();
        ParkingSession session = new ParkingSession(
                UUID.randomUUID().toString(),
                nextSessionCode(now),
                normalizedPlate,
                normalizedPlate,
                request.vehicleType(),
                ParkingSessionStatus.ACTIVE,
                PaymentStatus.UNPAID,
                now,
                request.entryGate().trim(),
                actor.id(),
                PlateSource.MANUAL,
                nextLookupToken(),
                null, null, null,
                now,
                now);

        if (!repository.saveIfNoActive(session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ACTIVE session already exists for this vehicle plate");
        }
        eventRecorder.record(new ParkingSessionEvent("CHECK_IN_CREATED", session.id(), actor.id(), now));
        return toStaffResponse(session);
    }

    public ParkingSessionResponse getById(String sessionId) {
        return toStaffResponse(findSessionById(sessionId));
    }

    public List<ParkingSessionResponse> search(String status, String plate) {
        ParkingSessionStatus parsedStatus = parseStatus(status);
        String normalizedPlate = plate == null || plate.isBlank() ? null : normalizePlate(plate);
        return repository.search(parsedStatus, normalizedPlate).stream().map(this::toStaffResponse).toList();
    }

    public PublicTicketResponse publicTicket(String lookupToken) {
        ParkingSession session = repository.findByLookupToken(lookupToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        long duration = Math.max(0, Duration.between(session.entryTime(), Instant.now()).toMinutes());
        return new PublicTicketResponse(
                session.id(),
                session.sessionCode(),
                session.vehiclePlate(),
                session.vehicleType(),
                session.status(),
                session.paymentStatus(),
                session.entryTime(),
                duration,
                demoFlatFee,
                0,
                demoFlatFee,
                "QR Lookup Token is for ticket lookup only and cannot authorize exit.");
    }

    public InternalPaymentUpdateResponse updatePaymentStatus(String sessionId, InternalPaymentUpdateRequest request) {
        if (request == null || request.paymentStatus() != PaymentStatus.PAID || request.paymentOrderId() == null || request.paymentOrderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slice 4 accepts only PAID payment updates");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.paymentStatus() == PaymentStatus.PAID) {
            if (request.paymentOrderId().equals(session.paymentOrderId())) {
                return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, session.amountPaid(), true);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already paid by another order");
        }
        ParkingSession updated = new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(),
                session.vehicleType(), session.status(), PaymentStatus.PAID, session.entryTime(), session.entryGate(), session.staffId(),
                session.plateSource(), session.qrLookupToken(), request.paymentOrderId(), request.amountPaid(), request.paidAt(), session.createdAt(), Instant.now());
        repository.update(updated);
        eventRecorder.record(new ParkingSessionEvent("PAYMENT_CONFIRMED", sessionId, "payment-service", Instant.now()));
        return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, request.amountPaid(), true);
    }

    public static String normalizePlate(String plate) {
        return plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private ParkingSession findSessionById(String sessionId) {
        return repository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking session not found"));
    }

    private ParkingSessionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ParkingSessionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parking session status");
        }
    }

    private String nextSessionCode(Instant now) {
        return "PF-" + LocalDate.ofInstant(now, ZoneOffset.UTC).toString().replace("-", "")
                + "-" + String.format("%06d", sequence.incrementAndGet());
    }

    private String nextLookupToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ParkingSessionResponse toStaffResponse(ParkingSession session) {
        return new ParkingSessionResponse(
                session.id(),
                session.sessionCode(),
                session.vehiclePlate(),
                session.normalizedPlate(),
                session.vehicleType(),
                session.status(),
                session.paymentStatus(),
                session.entryTime(),
                session.entryGate(),
                session.qrLookupToken(),
                publicTicketBaseUrl + "/api/public/tickets/" + session.qrLookupToken());
    }
}
