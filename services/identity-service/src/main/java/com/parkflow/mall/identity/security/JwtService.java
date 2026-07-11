package com.parkflow.mall.identity.security;

import com.parkflow.mall.identity.model.DemoUser;
import com.parkflow.mall.identity.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-seconds}") long expirationSeconds) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String issue(DemoUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claim("userId", user.id())
                .claim("displayName", user.displayName())
                .claim("roles", user.roles().stream().map(Role::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<?> rawRoles = claims.get("roles", List.class);
        List<Role> roles = rawRoles.stream()
                .map(Object::toString)
                .map(roleName -> Role.valueOf(roleName))
                .toList();
        return new AuthenticatedUser(
                claims.get("userId", String.class),
                claims.getSubject(),
                claims.get("displayName", String.class),
                roles);
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
