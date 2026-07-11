package com.parkflow.mall.parking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
class ParkingServiceApplicationTests {
    private static final String JWT_SECRET = "parkflow-local-development-secret-change-me-2026";
    private static final AtomicLong PLATE_SEQUENCE = new AtomicLong(12000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot authorize exit")));
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
    void noCheckoutEndpointExistsForLookupTokenOrSession() throws Exception {
        MvcResult checkIn = checkIn(nextPlate(), staffToken()).andExpect(status().isCreated()).andReturn();
        String sessionId = objectMapper.readTree(checkIn.getResponse().getContentAsString()).path("sessionId").asText();
        mockMvc.perform(post("/api/parking/sessions/{sessionId}/check-out", sessionId)
                        .header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isNotFound());
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
