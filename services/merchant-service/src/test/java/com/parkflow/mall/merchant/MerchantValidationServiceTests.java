package com.parkflow.mall.merchant;

import com.parkflow.mall.merchant.dto.ValidateInvoiceRequest;
import com.parkflow.mall.merchant.repository.InMemoryInvoiceValidationRepository;
import com.parkflow.mall.merchant.security.MerchantUser;
import com.parkflow.mall.merchant.service.MerchantValidationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MerchantValidationServiceTests {
    private RestTemplate restTemplate;
    private MockRestServiceServer parking;
    private MerchantValidationService service;
    private final MerchantUser merchant = new MerchantUser("usr_merchant", "merchant", List.of("MERCHANT_STAFF"));
    private final MerchantUser admin = new MerchantUser("usr_admin", "admin", List.of("ADMIN"));

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        parking = MockRestServiceServer.bindTo(restTemplate).build();
        service = new MerchantValidationService(new InMemoryInvoiceValidationRepository(), restTemplate,
                "http://parking", "internal-token", 300000, 5000);
    }

    @Test
    void merchantAndAdminCanValidateAndTenantIsDerivedFromUser() {
        expectTicket("merchant-token", "session-1", "ACTIVE", 5000);
        expectDiscount("session-1", 180000, 0, 5000);
        var merchantResult = service.validate(new ValidateInvoiceRequest("merchant-token", "INV-001", 180000), merchant);
        assertEquals("tenant-demo-001", merchantResult.tenantId());
        assertEquals(0, merchantResult.discountAmount());
        parking.verify(); parking.reset();

        expectTicket("admin-token", "session-2", "ACTIVE", 5000);
        expectDiscount("session-2", 1000, 0, 5000);
        var adminResult = service.validate(new ValidateInvoiceRequest("admin-token", "INV-002", 1000), admin);
        assertEquals("tenant-demo-admin", adminResult.tenantId());
        parking.verify();
    }

    @Test
    void missingBlankAndNonPositiveInvoiceInputsAreRejectedBeforeParkingCall() {
        assertBad(new ValidateInvoiceRequest(null, "INV", 1));
        assertBad(new ValidateInvoiceRequest("token", " ", 1));
        assertBad(new ValidateInvoiceRequest("token", "INV", 0));
        assertBad(new ValidateInvoiceRequest("token", "INV", -1));
    }

    @Test
    void invalidLookupAndExitedSessionAreRejected() {
        parking.expect(requestTo("http://parking/api/public/tickets/bad"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
        var invalid = assertThrows(ResponseStatusException.class,
                () -> service.validate(new ValidateInvoiceRequest("bad", "INV-001", 1), merchant));
        assertEquals(404, invalid.getStatusCode().value());
        parking.verify(); parking.reset();

        expectTicket("exited", "session-x", "EXITED", 5000);
        var exited = assertThrows(ResponseStatusException.class,
                () -> service.validate(new ValidateInvoiceRequest("exited", "INV-002", 1), merchant));
        assertEquals(409, exited.getStatusCode().value());
        parking.verify();
    }

    @Test
    void duplicateCodeIsGloballyRejectedAndHistoryReturnsAcceptedValidation() {
        expectTicket("token", "session-1", "ACTIVE", 5000);
        expectDiscount("session-1", 100, 0, 5000);
        service.validate(new ValidateInvoiceRequest("token", "INV-ONCE", 100), merchant);
        var duplicate = assertThrows(ResponseStatusException.class,
                () -> service.validate(new ValidateInvoiceRequest("token", "INV-ONCE", 100), admin));
        assertEquals(409, duplicate.getStatusCode().value());
        var history = service.history("session-1", merchant);
        assertEquals(1, history.validations().size());
        assertEquals(100, history.totalEligibleInvoiceAmount());
        parking.verify();
    }

    @Test
    void invoicesAggregateAndApplyConfiguredDiscountAtThresholdCappedToFee() {
        expectTicket("token-1", "session-1", "ACTIVE", 5000);
        expectDiscount("session-1", 180000, 0, 5000);
        var first = service.validate(new ValidateInvoiceRequest("token-1", "INV-A", 180000), merchant);
        assertEquals(180000, first.totalEligibleInvoiceAmount());
        assertEquals(0, first.discountAmount());
        parking.verify(); parking.reset();

        expectTicket("token-2", "session-1", "ACTIVE", 5000);
        expectDiscount("session-1", 300000, 5000, 0);
        var second = service.validate(new ValidateInvoiceRequest("token-2", "INV-B", 120000), merchant);
        assertEquals(300000, second.totalEligibleInvoiceAmount());
        assertEquals(5000, second.discountAmount());
        assertEquals(0, second.finalFee());
        parking.verify();
    }

    @Test
    void discountNeverExceedsEstimatedParkingFeeAndParkingFailureIsReported() {
        expectTicket("small-fee", "session-small", "ACTIVE", 1000);
        expectDiscount("session-small", 300000, 1000, 0);
        var result = service.validate(new ValidateInvoiceRequest("small-fee", "INV-SMALL", 300000), merchant);
        assertEquals(1000, result.discountAmount());
        parking.verify(); parking.reset();

        expectTicket("down", "session-down", "ACTIVE", 5000);
        parking.expect(requestTo("http://parking/internal/parking/sessions/session-down/discount"))
                .andRespond(withServerError());
        var failure = assertThrows(ResponseStatusException.class,
                () -> service.validate(new ValidateInvoiceRequest("down", "INV-DOWN", 1), merchant));
        assertEquals(502, failure.getStatusCode().value());
        parking.verify();
    }

    @Test
    void unknownMerchantTenantIsForbidden() {
        var user = new MerchantUser("other", "other", List.of("MERCHANT_STAFF"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.validate(new ValidateInvoiceRequest("token", "INV", 1), user));
        assertEquals(403, error.getStatusCode().value());
    }

    private void expectTicket(String token, String sessionId, String status, long fee) {
        parking.expect(requestTo("http://parking/api/public/tickets/" + token)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sessionId\":\"" + sessionId + "\",\"sessionCode\":\"PF-001\",\"status\":\"" + status + "\",\"estimatedFee\":" + fee + "}", MediaType.APPLICATION_JSON));
    }

    private void expectDiscount(String sessionId, long total, long discount, long finalFee) {
        parking.expect(times(1), requestTo("http://parking/internal/parking/sessions/" + sessionId + "/discount"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-Service-Token", "internal-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"sessionId\":\"" + sessionId + "\",\"totalEligibleInvoiceAmount\":" + total + ",\"discountAmount\":" + discount + ",\"finalFee\":" + finalFee + "}", MediaType.APPLICATION_JSON));
    }

    private void assertBad(ValidateInvoiceRequest request) {
        var error = assertThrows(ResponseStatusException.class, () -> service.validate(request, merchant));
        assertEquals(400, error.getStatusCode().value());
    }
}
