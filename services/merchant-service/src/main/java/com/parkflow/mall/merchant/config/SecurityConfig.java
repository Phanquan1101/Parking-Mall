package com.parkflow.mall.merchant.config;

import com.parkflow.mall.merchant.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            JwtAuthenticationFilter filter) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(a ->
                        a.requestMatchers("/actuator/**", "/error").permitAll()
                                .requestMatchers("/api/merchant/**")
                                .hasAnyRole("ADMIN", "MERCHANT_STAFF")
                                .anyRequest().authenticated()
                )

                .exceptionHandling(e ->
                        e.authenticationEntryPoint(
                                (q, r, x) ->
                                        r.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )

                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}