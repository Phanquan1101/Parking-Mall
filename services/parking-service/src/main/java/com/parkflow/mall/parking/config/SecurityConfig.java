package com.parkflow.mall.parking.config;

import com.parkflow.mall.parking.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SecurityConfig {
    @Bean RestTemplate restTemplate() { return new RestTemplate(); }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info", "/error", "/api/public/tickets/**", "/internal/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/parking/sessions/*/exit-passes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/parking/sessions/check-in").hasAnyRole("ADMIN", "PARKING_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/parking/exit-passes/*/validate", "/api/parking/sessions/*/check-out", "/api/parking/sessions/*/manual-override").hasAnyRole("ADMIN", "PARKING_STAFF")
                        .requestMatchers("/api/parking/offline-sync/**").hasAnyRole("ADMIN", "PARKING_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/parking/sessions/**").hasAnyRole("ADMIN", "PARKING_STAFF")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
