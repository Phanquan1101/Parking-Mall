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
