package com.parkflow.mall.reservation.config;
import com.parkflow.mall.reservation.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig { @Bean SecurityFilterChain chain(HttpSecurity http,JwtAuthenticationFilter jwt)throws Exception{return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/actuator/**","/error","/internal/**").permitAll().requestMatchers(org.springframework.http.HttpMethod.POST,"/api/reservations").permitAll().requestMatchers(org.springframework.http.HttpMethod.GET,"/api/reservations/*").permitAll().requestMatchers(org.springframework.http.HttpMethod.POST,"/api/reservations/*/cancel").permitAll().requestMatchers("/api/reservations/**").hasAnyRole("ADMIN","PARKING_STAFF").anyRequest().authenticated()).exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.sendError(401))).addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();} }
