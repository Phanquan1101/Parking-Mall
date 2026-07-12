package com.parkflow.mall.parking.service;

import com.parkflow.mall.parking.dto.CheckInRequest;
import com.parkflow.mall.parking.dto.CheckOutRequest;
import com.parkflow.mall.parking.dto.CheckOutResponse;
import com.parkflow.mall.parking.dto.ExitPassResponse;
import com.parkflow.mall.parking.dto.GenerateExitPassRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateRequest;
import com.parkflow.mall.parking.dto.InternalPaymentUpdateResponse;
import com.parkflow.mall.parking.dto.ManualOverrideRequest;
import com.parkflow.mall.parking.dto.ParkingSessionResponse;
import com.parkflow.mall.parking.dto.PublicTicketResponse;
import com.parkflow.mall.parking.dto.ValidateExitPassRequest;
import com.parkflow.mall.parking.dto.ValidateExitPassResponse;
import com.parkflow.mall.parking.exception.ExitPassException;
import com.parkflow.mall.parking.model.ExitPass;
import com.parkflow.mall.parking.model.ExitPassStatus;
import com.parkflow.mall.parking.model.ParkingSession;
import com.parkflow.mall.parking.model.ParkingSessionEvent;
import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import com.parkflow.mall.parking.model.PlateSource;
import com.parkflow.mall.parking.repository.ExitPassRepository;
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
    private final ExitPassRepository exitPassRepository;
    private final ParkingSessionEventRecorder eventRecorder;
    private final String publicTicketBaseUrl;
    private final long demoFlatFee;
    private final long exitPassTtlSeconds;
    private final AtomicLong sequence = new AtomicLong();

    public ParkingSessionService(
            ParkingSessionRepository repository,
            ExitPassRepository exitPassRepository,
            ParkingSessionEventRecorder eventRecorder,
            @Value("${app.public-ticket-base-url}") String publicTicketBaseUrl,
            @Value("${app.demo-flat-fee:5000}") long demoFlatFee,
            @Value("${app.exit-pass-ttl-seconds:60}") long exitPassTtlSeconds) {
        this.repository = repository;
        this.exitPassRepository = exitPassRepository;
        this.eventRecorder = eventRecorder;
        this.publicTicketBaseUrl = publicTicketBaseUrl.replaceAll("/$", "");
        this.demoFlatFee = demoFlatFee;
        this.exitPassTtlSeconds = exitPassTtlSeconds;
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
                UUID.randomUUID().toString(), nextSessionCode(now), normalizedPlate, normalizedPlate,
                request.vehicleType(), ParkingSessionStatus.ACTIVE, PaymentStatus.UNPAID, now,
                request.entryGate().trim(), actor.id(), PlateSource.MANUAL, nextToken(), null, null, null,
                null, null, null, false, null, null, null, now, now);
        if (!repository.saveIfNoActive(session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ACTIVE session already exists for this vehicle plate");
        }
        eventRecorder.record(new ParkingSessionEvent("CHECK_IN_CREATED", session.id(), actor.id(), now));
        return toStaffResponse(session);
    }

    public ParkingSessionResponse getById(String sessionId) { return toStaffResponse(findSessionById(sessionId)); }

    public List<ParkingSessionResponse> search(String status, String plate) {
        ParkingSessionStatus parsedStatus = parseStatus(status);
        String normalizedPlate = plate == null || plate.isBlank() ? null : normalizePlate(plate);
        return repository.search(parsedStatus, normalizedPlate).stream().map(this::toStaffResponse).toList();
    }

    public PublicTicketResponse publicTicket(String lookupToken) {
        ParkingSession session = repository.findByLookupToken(lookupToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        long duration = Math.max(0, Duration.between(session.entryTime(), Instant.now()).toMinutes());
        boolean eligible = isExitEligible(session);
        boolean activePass = exitPassRepository.findActiveBySessionId(session.id()).isPresent();
        String exitPassMessage = session.status() == ParkingSessionStatus.EXITED
                ? "Vehicle has already exited."
                : eligible ? (activePass ? "A current Exit Pass exists. Generate a new one only if needed." : "Payment complete. Generate an Exit Pass before leaving.")
                : "Complete payment before generating an Exit Pass.";
        return new PublicTicketResponse(session.id(), session.sessionCode(), session.vehiclePlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), duration, demoFlatFee, 0, demoFlatFee,
                eligible, activePass, exitPassMessage,
                "QR Lookup Token is for ticket lookup only and cannot authorize exit.");
    }

    public synchronized ExitPassResponse generateExitPass(String sessionId, GenerateExitPassRequest request) {
        if (request == null || request.lookupToken() == null || request.lookupToken().isBlank()) {
            throw exitError(HttpStatus.BAD_REQUEST, "LOOKUP_TOKEN_REQUIRED", "lookupToken is required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (!request.lookupToken().equals(session.qrLookupToken())) {
            throw exitError(HttpStatus.FORBIDDEN, "LOOKUP_TOKEN_SESSION_MISMATCH", "Ticket lookup token does not match this session");
        }
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);

        exitPassRepository.invalidateActiveForSession(sessionId);
        Instant now = Instant.now();
        ExitPass pass = new ExitPass(UUID.randomUUID().toString(), nextToken(), sessionId, ExitPassStatus.ACTIVE,
                now, now.plusSeconds(exitPassTtlSeconds), null, null, "CUSTOMER_TICKET", exitPassTtlSeconds);
        exitPassRepository.save(pass);
        repository.update(copyWithExitPass(session, pass.id(), now));
        eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_CREATED", sessionId, "public-ticket", now));
        return new ExitPassResponse(pass.token(), sessionId, session.sessionCode(), pass.expiresAt(), pass.ttlSeconds(),
                pass.status(), "Dynamic Exit Pass is short-lived and can be used only once.");
    }

    public synchronized ValidateExitPassResponse validateExitPass(String token, ValidateExitPassRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "VALIDATION_INPUT_REQUIRED", "exitPlate and exitGate are required");
        }
        ExitPass pass = requireUsablePass(token);
        ParkingSession session = findSessionById(pass.sessionId());
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        if (!session.normalizedPlate().equals(exitPlate)) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_PLATE_MISMATCH", session.id(), actor.id(), Instant.now(), "exitGate=" + request.exitGate().trim()));
            throw exitError(HttpStatus.CONFLICT, "PLATE_MISMATCH", "Exit plate does not match the parking session");
        }
        return new ValidateExitPassResponse(true, session.id(), session.sessionCode(), session.vehiclePlate(),
                session.normalizedPlate(), exitPlate, session.paymentStatus(), session.status(), pass.expiresAt(),
                "Exit Pass is valid. Vehicle can be checked out.");
    }

    public synchronized CheckOutResponse checkOut(String sessionId, CheckOutRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.exitPassToken()) || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "CHECKOUT_INPUT_REQUIRED", "exitPassToken, exitPlate and exitGate are required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        ExitPass pass = requireUsablePass(request.exitPassToken());
        if (!pass.sessionId().equals(sessionId)) {
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_SESSION_MISMATCH", "Exit Pass does not belong to this session");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        if (!session.normalizedPlate().equals(exitPlate)) {
            eventRecorder.record(new ParkingSessionEvent("CHECKOUT_PLATE_MISMATCH", sessionId, actor.id(), Instant.now(), "exitGate=" + request.exitGate().trim()));
            throw exitError(HttpStatus.CONFLICT, "PLATE_MISMATCH", "Exit plate does not match the parking session");
        }
        Instant now = Instant.now();
        repository.update(copyExited(session, request.exitGate(), exitPlate, false, null, null, now));
        exitPassRepository.save(new ExitPass(pass.id(), pass.token(), pass.sessionId(), ExitPassStatus.USED,
                pass.createdAt(), pass.expiresAt(), now, pass.invalidatedAt(), pass.createdFrom(), pass.ttlSeconds()));
        eventRecorder.record(new ParkingSessionEvent("CHECKOUT_COMPLETED", sessionId, actor.id(), now, "exitGate=" + request.exitGate().trim()));
        return toCheckOutResponse(findSessionById(sessionId), false, null, "Vehicle checked out successfully.");
    }

    public synchronized CheckOutResponse manualOverride(String sessionId, ManualOverrideRequest request, AuthenticatedUser actor) {
        if (request == null || isBlank(request.reason()) || isBlank(request.exitPlate()) || isBlank(request.exitGate())) {
            throw exitError(HttpStatus.BAD_REQUEST, "MANUAL_OVERRIDE_REASON_REQUIRED", "reason, exitPlate and exitGate are required");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.status() == ParkingSessionStatus.EXITED) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_ALREADY_EXITED", "Vehicle has already exited");
        }
        requireExitEligible(session);
        String exitPlate = normalizePlate(request.exitPlate());
        String suspiciousReason = session.normalizedPlate().equals(exitPlate) ? null : "Manual override accepted with plate mismatch";
        Instant now = Instant.now();
        repository.update(copyExited(session, request.exitGate(), exitPlate, true, request.reason().trim(), suspiciousReason, now));
        exitPassRepository.invalidateActiveForSession(sessionId);
        eventRecorder.record(new ParkingSessionEvent("MANUAL_OVERRIDE_CHECKOUT", sessionId, actor.id(), now,
                "reason=" + request.reason().trim() + (suspiciousReason == null ? "" : "; " + suspiciousReason)));
        return toCheckOutResponse(findSessionById(sessionId), true, request.reason().trim(), "Vehicle checked out by manual override.");
    }

    public synchronized InternalPaymentUpdateResponse updatePaymentStatus(String sessionId, InternalPaymentUpdateRequest request) {
        if (request == null || request.paymentStatus() != PaymentStatus.PAID || isBlank(request.paymentOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slice 4 accepts only PAID payment updates");
        }
        ParkingSession session = findSessionById(sessionId);
        if (session.paymentStatus() == PaymentStatus.PAID) {
            if (request.paymentOrderId().equals(session.paymentOrderId())) {
                return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, session.amountPaid(), true);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already paid by another order");
        }
        Instant now = Instant.now();
        repository.update(new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(),
                session.vehicleType(), ParkingSessionStatus.PAID, PaymentStatus.PAID, session.entryTime(), session.entryGate(), session.staffId(),
                session.plateSource(), session.qrLookupToken(), request.paymentOrderId(), request.amountPaid(), request.paidAt(),
                session.exitTime(), session.exitGate(), session.exitPlate(), session.manualOverride(), session.manualOverrideReason(),
                session.suspiciousReason(), session.lastExitPassId(), session.createdAt(), now));
        eventRecorder.record(new ParkingSessionEvent("PAYMENT_CONFIRMED", sessionId, "payment-service", now));
        return new InternalPaymentUpdateResponse(sessionId, PaymentStatus.PAID, request.amountPaid(), true);
    }

    public static String normalizePlate(String plate) { return plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT); }

    private ExitPass requireUsablePass(String token) {
        ExitPass pass = exitPassRepository.findByToken(token)
                .orElseThrow(() -> exitError(HttpStatus.NOT_FOUND, "EXIT_PASS_NOT_FOUND", "Exit Pass was not found"));
        if (pass.status() == ExitPassStatus.USED) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_REUSE_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_ALREADY_USED", "Exit Pass has already been used");
        }
        if (pass.status() == ExitPassStatus.INVALIDATED) {
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_INVALIDATED_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.CONFLICT, "EXIT_PASS_INVALIDATED", "Exit Pass is no longer active");
        }
        if (pass.status() == ExitPassStatus.EXPIRED || !pass.expiresAt().isAfter(Instant.now())) {
            exitPassRepository.save(new ExitPass(pass.id(), pass.token(), pass.sessionId(), ExitPassStatus.EXPIRED,
                    pass.createdAt(), pass.expiresAt(), pass.usedAt(), pass.invalidatedAt(), pass.createdFrom(), pass.ttlSeconds()));
            eventRecorder.record(new ParkingSessionEvent("EXIT_PASS_EXPIRED_ATTEMPT", pass.sessionId(), "unknown", Instant.now()));
            throw exitError(HttpStatus.GONE, "EXIT_PASS_EXPIRED", "Exit Pass has expired");
        }
        return pass;
    }

    private void requireExitEligible(ParkingSession session) {
        if (!isExitEligible(session)) {
            throw exitError(HttpStatus.CONFLICT, "SESSION_NOT_PAID", "Payment is required before vehicle exit");
        }
    }

    private boolean isExitEligible(ParkingSession session) {
        return session.status() != ParkingSessionStatus.EXITED && (session.paymentStatus() == PaymentStatus.PAID || demoFlatFee == 0);
    }

    private ParkingSession copyWithExitPass(ParkingSession session, String passId, Instant now) {
        return new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), session.entryGate(), session.staffId(), session.plateSource(),
                session.qrLookupToken(), session.paymentOrderId(), session.amountPaid(), session.paidAt(), session.exitTime(), session.exitGate(),
                session.exitPlate(), session.manualOverride(), session.manualOverrideReason(), session.suspiciousReason(), passId, session.createdAt(), now);
    }

    private ParkingSession copyExited(ParkingSession session, String exitGate, String exitPlate, boolean manualOverride,
            String manualOverrideReason, String suspiciousReason, Instant now) {
        return new ParkingSession(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                ParkingSessionStatus.EXITED, session.paymentStatus(), session.entryTime(), session.entryGate(), session.staffId(), session.plateSource(),
                session.qrLookupToken(), session.paymentOrderId(), session.amountPaid(), session.paidAt(), now, exitGate.trim(), exitPlate,
                manualOverride, manualOverrideReason, suspiciousReason, session.lastExitPassId(), session.createdAt(), now);
    }

    private CheckOutResponse toCheckOutResponse(ParkingSession session, boolean manualOverride, String reason, String message) {
        return new CheckOutResponse(session.id(), session.sessionCode(), session.status().name(), session.paymentStatus(), session.entryTime(),
                session.exitTime(), session.exitGate(), session.exitPlate(), manualOverride, reason, message);
    }

    private ParkingSession findSessionById(String sessionId) {
        return repository.findById(sessionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking session not found"));
    }

    private ParkingSessionStatus parseStatus(String status) {
        if (isBlank(status)) return null;
        try { return ParkingSessionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parking session status"); }
    }

    private String nextSessionCode(Instant now) {
        return "PF-" + LocalDate.ofInstant(now, ZoneOffset.UTC).toString().replace("-", "") + "-" + String.format("%06d", sequence.incrementAndGet());
    }

    private String nextToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ParkingSessionResponse toStaffResponse(ParkingSession session) {
        return new ParkingSessionResponse(session.id(), session.sessionCode(), session.vehiclePlate(), session.normalizedPlate(), session.vehicleType(),
                session.status(), session.paymentStatus(), session.entryTime(), session.entryGate(), session.qrLookupToken(),
                publicTicketBaseUrl + "/api/public/tickets/" + session.qrLookupToken());
    }

    private ExitPassException exitError(HttpStatus status, String code, String message) { return new ExitPassException(status, code, message); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
