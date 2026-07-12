package com.parkflow.mall.parking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.parkflow.mall.parking.model.ExitPass;
import com.parkflow.mall.parking.model.ExitPassStatus;
import com.parkflow.mall.parking.repository.ExitPassRepository;
import com.parkflow.mall.parking.repository.ParkingSessionRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@AutoConfigureMockMvc
class ParkingServiceApplicationTests {
    private static final String JWT_SECRET = "parkflow-local-development-secret-change-me-2026";
    private static final AtomicLong PLATE_SEQUENCE = new AtomicLong(12000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExitPassRepository exitPassRepository;

    @Autowired
    private ParkingSessionRepository parkingSessionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void reservationCheckInConsumesReservationAndCreatesNormalUnpaidSession() throws Exception {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8085/internal/reservations/RSV-demo/consume"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", "parkflow-local-internal-token-change-me"))
                .andRespond(withSuccess("{\"reservationId\":\"reservation-1\",\"status\":\"CONSUMED\"}", MediaType.APPLICATION_JSON));
        String plate = nextPlate();
        MvcResult result = mockMvc.perform(post("/api/parking/sessions/check-in").header("Authorization", "Bearer " + staffToken())
                        .contentType(MediaType.APPLICATION_JSON).content(checkInPayload(plate).replace("}", ",\"reservationCode\":\"RSV-demo\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reservationId").value("reservation-1"))
                .andExpect(jsonPath("$.reservationCode").value("RSV-demo")).andExpect(jsonPath("$.paymentStatus").value("UNPAID")).andReturn();
        server.verify();
        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).path("sessionId").asText();
        org.junit.jupiter.api.Assertions.assertEquals("RSV-demo", parkingSessionRepository.findById(sessionId).orElseThrow().reservationCode());
    }

    @Test
    void checkInCreatesActiveSessionWithOpaqueLookupToken() throws Exception {
        MvcResult result = checkIn(nextPlate(), staffToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.sessionCode").value(org.hamcrest.Matchers.startsWith("PF-")))
                .andExpect(jsonPath("$.qrLookupToken").isNotEmpty())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertFalse(response.path("qrLookupToken").asText().contains("59A1"));
    }

    @Test
    void checkInNormalizesPlate() throws Exception {
        checkIn("59 A1-12345", staffToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.normalizedPlate").value("59A112345"));
    }

    @Test
    void checkInRejectsMissingPlate() throws Exception {
        mockMvc.perform(post("/api/parking/sessions/check-in")
                        .header("Authorization", "Bearer " + staffToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"MOTORBIKE\",\"entryGate\":\"GATE_IN_01\",\"staffId\":\"staff-demo-id\",\"plateSource\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkInRejectsDuplicateActiveNormalizedPlate() throws Exception {
        String plate = nextPlate();
        checkIn(plate, staffToken()).andExpect(status().isCreated());
        checkIn(plate.replace("-", " "), staffToken()).andExpect(status().isConflict());
    }

    @Test
    void publicTicketLookupReturnsSafeTicketOnly() throws Exception {
        MvcResult checkIn = checkIn(nextPlate(), staffToken()).andExpect(status().isCreated()).andReturn();
        String token = objectMapper.readTree(checkIn.getResponse().getContentAsString()).path("qrLookupToken").asText();

        mockMvc.perform(get("/api/public/tickets/{lookupToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode").isNotEmpty())
                .andExpect(jsonPath("$.staffId").doesNotExist())
                .andExpect(jsonPath("$.qrLookupToken").doesNotExist())
                .andExpect(jsonPath("$.canGenerateExitPass").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot authorize exit")));
    }

    @Test
    void paidPublicTicketShowsExitPassAvailabilityWithoutExposingPassToken() throws Exception {
        JsonNode session = createPaidSession();
        mockMvc.perform(get("/api/public/tickets/{lookupToken}", session.path("qrLookupToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canGenerateExitPass").value(true))
                .andExpect(jsonPath("$.exitPassAvailable").value(false))
                .andExpect(jsonPath("$.exitPassToken").doesNotExist());
    }

    @Test
    void publicTicketLookupRejectsInvalidTokenSafely() throws Exception {
        mockMvc.perform(get("/api/public/tickets/not-a-valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedCheckInRequiresToken() throws Exception {
        mockMvc.perform(post("/api/parking/sessions/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInPayload(nextPlate())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void merchantRoleCannotCheckIn() throws Exception {
        checkIn(nextPlate(), token("usr_merchant", "merchant", "Merchant Staff", List.of("MERCHANT_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void paidSessionCanGenerateShortLivedOpaqueExitPass() throws Exception {
        JsonNode session = createPaidSession();
        MvcResult result = generateExitPass(session).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ttlSeconds").value(60))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();
        String pass = objectMapper.readTree(result.getResponse().getContentAsString()).path("exitPassToken").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(session.path("qrLookupToken").asText(), pass);
    }

    @Test
    void unpaidSessionCannotGenerateExitPass() throws Exception {
        JsonNode session = createSession();
        generateExitPass(session).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_PAID"));
    }

    @Test
    void invalidOrMismatchedLookupTokenCannotGenerateExitPass() throws Exception {
        JsonNode paidSession = createPaidSession();
        mockMvc.perform(post("/api/parking/sessions/{sessionId}/exit-passes", paidSession.path("sessionId").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lookupToken\":\"not-a-ticket\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("LOOKUP_TOKEN_SESSION_MISMATCH"));
        JsonNode otherSession = createPaidSession();
        mockMvc.perform(post("/api/parking/sessions/{sessionId}/exit-passes", paidSession.path("sessionId").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lookupToken\":\"" + otherSession.path("qrLookupToken").asText() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newExitPassInvalidatesPreviousActivePass() throws Exception {
        JsonNode session = createPaidSession();
        String first = exitPassToken(generateExitPass(session).andReturn());
        String second = exitPassToken(generateExitPass(session).andReturn());
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
        validate(first, "59A1-12345", staffToken()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EXIT_PASS_INVALIDATED"));
    }

    @Test
    void validPassAndMatchingPlateCanBeValidatedAndCheckedOutOnce() throws Exception {
        JsonNode session = createPaidSession();
        String plate = session.path("vehiclePlate").asText();
        String pass = exitPassToken(generateExitPass(session).andReturn());
        validate(pass, plate, staffToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.normalizedEntryPlate").value(plate));
        checkOut(session.path("sessionId").asText(), pass, plate, staffToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXITED"))
                .andExpect(jsonPath("$.manualOverride").value(false));
        validate(pass, plate, staffToken()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EXIT_PASS_ALREADY_USED"));
        checkOut(session.path("sessionId").asText(), pass, plate, staffToken()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SESSION_ALREADY_EXITED"));
    }

    @Test
    void expiredUsedAndUnknownPassAreRejected() throws Exception {
        JsonNode session = createPaidSession();
        String pass = exitPassToken(generateExitPass(session).andReturn());
        ExitPass existing = exitPassRepository.findByToken(pass).orElseThrow();
        exitPassRepository.save(new ExitPass(existing.id(), existing.token(), existing.sessionId(), ExitPassStatus.ACTIVE,
                existing.createdAt(), Instant.now().minusSeconds(1), null, null, existing.createdFrom(), existing.ttlSeconds()));
        validate(pass, session.path("vehiclePlate").asText(), staffToken()).andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("EXIT_PASS_EXPIRED"));
        validate("not-an-exit-pass", "59A1-12345", staffToken()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EXIT_PASS_NOT_FOUND"));
    }

    @Test
    void lookupTokenAndPlateMismatchCannotAuthorizeCheckout() throws Exception {
        JsonNode session = createPaidSession();
        String pass = exitPassToken(generateExitPass(session).andReturn());
        validate(pass, "30A-99999", staffToken()).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("PLATE_MISMATCH"));
        checkOut(session.path("sessionId").asText(), session.path("qrLookupToken").asText(), session.path("vehiclePlate").asText(), staffToken())
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("EXIT_PASS_NOT_FOUND"));
        checkOut(session.path("sessionId").asText(), pass, "30A-99999", staffToken())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("PLATE_MISMATCH"));
    }

    @Test
    void manualOverrideRequiresPaymentAndReasonAndRecordsSuccessfulOverride() throws Exception {
        JsonNode unpaid = createSession();
        manualOverride(unpaid.path("sessionId").asText(), "verified", unpaid.path("vehiclePlate").asText(), staffToken())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SESSION_NOT_PAID"));
        JsonNode paid = createPaidSession();
        manualOverride(paid.path("sessionId").asText(), " ", paid.path("vehiclePlate").asText(), staffToken())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("MANUAL_OVERRIDE_REASON_REQUIRED"));
        manualOverride(paid.path("sessionId").asText(), "Customer lost phone; payment verified.", "30A-99999", staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXITED"))
                .andExpect(jsonPath("$.manualOverride").value(true));
    }

    @Test
    void exitValidationAndCheckoutRequireStaffOrAdminRole() throws Exception {
        JsonNode session = createPaidSession();
        String pass = exitPassToken(generateExitPass(session).andReturn());
        validate(pass, session.path("vehiclePlate").asText(), null).andExpect(status().isUnauthorized());
        validate(pass, session.path("vehiclePlate").asText(), token("usr_merchant", "merchant", "Merchant", List.of("MERCHANT_STAFF")))
                .andExpect(status().isForbidden());
        validate(pass, session.path("vehiclePlate").asText(), token("usr_admin", "admin", "Admin", List.of("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void offlineSyncRequiresStaffJwtAndRequestIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/parking/offline-sync").contentType(MediaType.APPLICATION_JSON).content(offlineSyncBody("event-a", "key-a", nextPlate(), "MOTORBIKE")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/parking/offline-sync").header("Authorization", "Bearer " + token("merchant", "merchant", "Merchant", List.of("MERCHANT_STAFF")))
                        .header("Idempotency-Key", "batch-a").contentType(MediaType.APPLICATION_JSON).content(offlineSyncBody("event-a", "key-a", nextPlate(), "MOTORBIKE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/parking/offline-sync").header("Authorization", "Bearer " + staffToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(offlineSyncBody("event-b", "key-b", nextPlate(), "MOTORBIKE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void offlineCheckInSyncCreatesOfficialActiveSessionWithJwtStaffIdentityAndLookupToken() throws Exception {
        String eventId = "offline-" + UUID.randomUUID();
        MvcResult result = offlineSync(eventId, "key-" + UUID.randomUUID(), nextPlate(), "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("SYNCED"))
                .andExpect(jsonPath("$.results[0].serverSessionId").isNotEmpty()).andExpect(jsonPath("$.results[0].sessionCode").isNotEmpty()).andReturn();
        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).path("results").get(0).path("serverSessionId").asText();
        var session = parkingSessionRepository.findById(sessionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("usr_staff", session.staffId());
        org.junit.jupiter.api.Assertions.assertFalse(session.qrLookupToken().isBlank());
        mockMvc.perform(get("/api/parking/offline-sync/{eventId}", eventId).header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SYNCED"));
    }

    @Test
    void offlineSyncRejectsInvalidPayloadAndKeepsServerTruthForConflicts() throws Exception {
        offlineSync("missing-" + UUID.randomUUID(), "key-" + UUID.randomUUID(), "", "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("REJECTED"));
        offlineSync("invalid-type-" + UUID.randomUUID(), "key-" + UUID.randomUUID(), nextPlate(), "TRUCK", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("REJECTED"));
        String activePlate = nextPlate();
        checkIn(activePlate, staffToken()).andExpect(status().isCreated());
        offlineSync("conflict-" + UUID.randomUUID(), "key-" + UUID.randomUUID(), activePlate, "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("CONFLICT"));
    }

    @Test
    void duplicateOfflineEventOrEventIdempotencyKeyDoesNotCreateSecondSession() throws Exception {
        String eventId = "duplicate-" + UUID.randomUUID();
        String eventKey = "key-" + UUID.randomUUID();
        MvcResult first = offlineSync(eventId, eventKey, nextPlate(), "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andReturn();
        String firstSessionId = objectMapper.readTree(first.getResponse().getContentAsString()).path("results").get(0).path("serverSessionId").asText();
        offlineSync(eventId, eventKey, nextPlate(), "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.results[0].serverSessionId").value(firstSessionId));
        offlineSync("different-" + UUID.randomUUID(), eventKey, nextPlate(), "MOTORBIKE", "batch-" + UUID.randomUUID(), staffToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("DUPLICATE"));
    }

    @Test
    void unknownOfflineEventStatusIsSafeNotFound() throws Exception {
        mockMvc.perform(get("/api/parking/offline-sync/unknown-event").header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalMerchantDiscountRequiresValidTokenAndUpdatesOnlyTicketFeeFields() throws Exception {
        JsonNode session = createSession();
        String sessionId = session.path("sessionId").asText();
        mockMvc.perform(post("/internal/parking/sessions/{sessionId}/discount", sessionId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/parking/sessions/{sessionId}/discount", sessionId)
                        .header("X-Internal-Service-Token", "wrong").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/parking/sessions/{sessionId}/discount", sessionId)
                        .header("X-Internal-Service-Token", "parkflow-local-internal-token-change-me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"MERCHANT_VALIDATION\",\"totalEligibleInvoiceAmount\":300000,\"discountAmount\":5000,\"discountPolicy\":\"AGGREGATE_INVOICE\",\"updatedBy\":\"merchant-service\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(5000)).andExpect(jsonPath("$.finalFee").value(0));
        mockMvc.perform(get("/api/public/tickets/{lookupToken}", session.path("qrLookupToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalEligibleInvoiceAmount").value(300000))
                .andExpect(jsonPath("$.discountAmount").value(5000)).andExpect(jsonPath("$.finalFee").value(0))
                .andExpect(jsonPath("$.merchantDiscountMessage").isNotEmpty());
        var stored = parkingSessionRepository.findById(sessionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("UNPAID", stored.paymentStatus().name());
        org.junit.jupiter.api.Assertions.assertNull(stored.exitTime());
        mockMvc.perform(post("/internal/parking/sessions/{sessionId}/discount", sessionId)
                        .header("X-Internal-Service-Token", "parkflow-local-internal-token-change-me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalEligibleInvoiceAmount\":1,\"discountAmount\":5001}"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode createSession() throws Exception {
        MvcResult result = checkIn(nextPlate(), staffToken()).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createPaidSession() throws Exception {
        JsonNode session = createSession();
        mockMvc.perform(post("/internal/parking/sessions/{sessionId}/payment-status", session.path("sessionId").asText())
                        .header("X-Internal-Service-Token", "parkflow-local-internal-token-change-me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentOrderId\":\"order-" + UUID.randomUUID() + "\",\"paymentStatus\":\"PAID\",\"amountPaid\":5000,\"paidAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk());
        return session;
    }

    private org.springframework.test.web.servlet.ResultActions generateExitPass(JsonNode session) throws Exception {
        return mockMvc.perform(post("/api/parking/sessions/{sessionId}/exit-passes", session.path("sessionId").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"lookupToken\":\"" + session.path("qrLookupToken").asText() + "\"}"));
    }

    private String exitPassToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("exitPassToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions validate(String pass, String plate, String token) throws Exception {
        var request = post("/api/parking/exit-passes/{token}/validate", pass).contentType(MediaType.APPLICATION_JSON)
                .content("{\"exitGate\":\"GATE_OUT_01\",\"exitPlate\":\"" + plate + "\"}");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request);
    }

    private org.springframework.test.web.servlet.ResultActions checkOut(String sessionId, String pass, String plate, String token) throws Exception {
        return mockMvc.perform(post("/api/parking/sessions/{sessionId}/check-out", sessionId)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"exitPassToken\":\"" + pass + "\",\"exitPlate\":\"" + plate + "\",\"exitGate\":\"GATE_OUT_01\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions manualOverride(String sessionId, String reason, String plate, String token) throws Exception {
        return mockMvc.perform(post("/api/parking/sessions/{sessionId}/manual-override", sessionId)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\",\"exitPlate\":\"" + plate + "\",\"exitGate\":\"GATE_OUT_01\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions offlineSync(String eventId, String eventKey, String plate, String vehicleType, String batchKey, String token) throws Exception {
        return mockMvc.perform(post("/api/parking/offline-sync").header("Authorization", "Bearer " + token).header("Idempotency-Key", batchKey)
                .contentType(MediaType.APPLICATION_JSON).content(offlineSyncBody(eventId, eventKey, plate, vehicleType)));
    }

    private String offlineSyncBody(String eventId, String eventKey, String plate, String vehicleType) {
        return "{\"deviceId\":\"staff-device-test\",\"events\":[{\"eventId\":\"" + eventId + "\",\"eventType\":\"OFFLINE_CHECK_IN\",\"idempotencyKey\":\"" + eventKey + "\",\"localTimestamp\":\"" + Instant.now() + "\",\"payload\":{\"vehiclePlate\":\"" + plate + "\",\"vehicleType\":\"" + vehicleType + "\",\"entryGate\":\"GATE_IN_01\",\"plateSource\":\"MANUAL\"}}]}";
    }

    private org.springframework.test.web.servlet.ResultActions checkIn(String plate, String token) throws Exception {
        return mockMvc.perform(post("/api/parking/sessions/check-in")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInPayload(plate)));
    }

    private String checkInPayload(String plate) {
        return "{\"vehiclePlate\":\"" + plate + "\",\"vehicleType\":\"MOTORBIKE\",\"entryGate\":\"GATE_IN_01\",\"staffId\":\"staff-demo-id\",\"plateSource\":\"MANUAL\"}";
    }

    private String nextPlate() {
        return "59A1-" + PLATE_SEQUENCE.incrementAndGet();
    }

    private String staffToken() {
        return token("usr_staff", "staff", "Parking Staff", List.of("PARKING_STAFF"));
    }

    private String token(String userId, String username, String displayName, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("displayName", displayName)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
