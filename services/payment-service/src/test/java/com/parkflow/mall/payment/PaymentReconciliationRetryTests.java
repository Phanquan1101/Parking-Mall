package com.parkflow.mall.payment;

import com.parkflow.mall.payment.dto.PaymentDtos.*;
import com.parkflow.mall.payment.model.*;
import com.parkflow.mall.payment.repository.*;
import com.parkflow.mall.payment.service.PaymentService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PaymentReconciliationRetryTests {
    private RestTemplate rest; private MockRestServiceServer parking; private PaymentService service;
    @BeforeEach void setUp(){rest=new RestTemplate();parking=MockRestServiceServer.bindTo(rest).build();service=new PaymentService(new InMemoryPaymentOrderRepository(),new InMemoryIdempotencyStore(),rest,"http://parking","token","SIMULATION",15);}
    @Test void failedParkingUpdateCreatesOpenItemAndSuccessfulRetryResolvesIt(){
        expectTicket(); parking.expect(requestTo("http://parking/internal/parking/sessions/session-1/payment-status")).andRespond(withServerError()); var order=service.create(new CreateOrderRequest(PaymentTargetType.PARKING_SESSION,"session-1","lookup"),"create");
        var simulated=service.simulate(new SimulateRequest(order.paymentOrderId(),order.paymentCode(),5000),"simulate");
        assertEquals(PaymentStatus.PAID,simulated.status()); assertEquals("PENDING_RECONCILIATION",simulated.parkingUpdateStatus());
        assertEquals("OPEN",service.listItems().get(0).get("status")); parking.verify(); parking.reset();
        parking.expect(requestTo("http://parking/internal/parking/sessions/session-1/payment-status")).andExpect(header("X-Internal-Service-Token","token")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        var run=service.reconcile(false,true); assertEquals(1,((Map<?,?>)run.get("summary")).get("resolvedItems"));
        assertEquals("RESOLVED",service.listItems().get(0).get("status")); assertEquals(1,service.listItems().get(0).get("attemptCount")); parking.verify();
    }
    @Test void failedRetryStaysOpenAndMismatchNeverCallsParking(){
        expectTicket();parking.expect(requestTo("http://parking/internal/parking/sessions/session-1/payment-status")).andRespond(withServerError());var order=service.create(new CreateOrderRequest(PaymentTargetType.PARKING_SESSION,"session-1","lookup"),"create");service.simulate(new SimulateRequest(order.paymentOrderId(),order.paymentCode(),5000),"simulate");parking.verify();parking.reset();
        parking.expect(requestTo("http://parking/internal/parking/sessions/session-1/payment-status")).andRespond(withServerError());service.reconcile(false,true);assertEquals("OPEN",service.listItems().get(0).get("status"));parking.verify();
    }
    @Test void itemsRemainDistinctAndGetReturnsCorrectItem(){
        expectTicket();parking.expect(requestTo("http://parking/internal/parking/sessions/session-1/payment-status")).andRespond(withServerError());var a=service.create(new CreateOrderRequest(PaymentTargetType.PARKING_SESSION,"session-1","lookup"),"a");service.simulate(new SimulateRequest(a.paymentOrderId(),a.paymentCode(),5000),"sa");parking.verify();parking.reset();
        var b=new PaymentOrder("mismatch","CODE",PaymentTargetType.PARKING_SESSION,"session-2",5000,"VND",PaymentStatus.MISMATCHED,java.time.Instant.now(),java.time.Instant.now().plusSeconds(60),null,true,null); ((PaymentOrderRepository)getField("orders")).save(b);service.reconcile(false,false);
        assertEquals(2,service.listItems().size());String id=(String)service.listItems().get(0).get("itemId");assertEquals(id,service.item(id).get("itemId"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.item("unknown"));
    }
    private Object getField(String name){try{var f=PaymentService.class.getDeclaredField(name);f.setAccessible(true);return f.get(service);}catch(Exception e){throw new RuntimeException(e);}}
    private void expectTicket(){parking.expect(requestTo("http://parking/api/public/tickets/lookup")).andRespond(withSuccess("{\"sessionId\":\"session-1\",\"status\":\"ACTIVE\",\"paymentStatus\":\"UNPAID\",\"finalFee\":5000}",MediaType.APPLICATION_JSON));}
}
