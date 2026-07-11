package com.parkflow.mall.gateway;

import com.parkflow.mall.gateway.controller.AuthGatewayController;
import com.parkflow.mall.gateway.service.IdentityServiceProxy;
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

@WebMvcTest(AuthGatewayController.class)
class AuthGatewayControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityServiceProxy identityServiceProxy;

    @Test
    void loginRouteForwardsToIdentityProxy() throws Exception {
        when(identityServiceProxy.login(anyString()))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"accessToken\":\"token\"}"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"token\"}"));
    }

    @Test
    void meRouteForwardsAuthorizationHeader() throws Exception {
        when(identityServiceProxy.me("Bearer token"))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"username\":\"admin\"}"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"admin\"}"));
    }
}
