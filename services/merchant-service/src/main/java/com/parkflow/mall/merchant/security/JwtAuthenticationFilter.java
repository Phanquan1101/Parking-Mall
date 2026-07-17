package com.parkflow.mall.merchant.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final byte[] secret;

    public JwtAuthenticationFilter(
            @Value("${merchant.jwt-secret}") String secret) {

        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");

        if (h != null && h.startsWith("Bearer ")) {
            try {
                var claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(secret))
                        .build()
                        .parseSignedClaims(h.substring(7))
                        .getPayload();

                Object rawRoles = claims.get("roles");

                List<String> roles =
                        rawRoles instanceof List<?> list
                                ? list.stream().map(Object::toString).toList()
                                : List.of();

                MerchantUser user = new MerchantUser(
                        claims.get("userId", String.class),
                        claims.getSubject(),
                        roles
                );

                var auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList()
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(auth);

            } catch (Exception ignored) {
            }
        }

        chain.doFilter(req, res);
    }
}