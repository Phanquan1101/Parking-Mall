package com.parkflow.mall.gateway;

import com.parkflow.mall.gateway.controller.ParkingGatewayController;
import com.parkflow.mall.gateway.parking.ParkingServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingGatewayController.class)
class ParkingGatewayControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingServiceProxy parkingServiceProxy;

    @Test
    void checkInRouteForwardsAuthorizationToParkingProxy() throws Exception {
        when(parkingServiceProxy.checkIn(anyString(), anyString()))
                .thenReturn(ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body("{\"status\":\"ACTIVE\"}"));

        mockMvc.perform(post("/api/parking/sessions/check-in")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiclePlate\":\"59A1-12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"status\":\"ACTIVE\"}"));
    }

    @Test
    void publicTicketRouteDoesNotRequireAuthorization() throws Exception {
        when(parkingServiceProxy.publicTicket("lookup-token"))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"sessionCode\":\"PF-20260711-000001\"}"));

        mockMvc.perform(get("/api/public/tickets/lookup-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"sessionCode\":\"PF-20260711-000001\"}"));
    }
}
