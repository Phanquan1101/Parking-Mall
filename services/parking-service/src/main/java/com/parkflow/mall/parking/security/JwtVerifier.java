package com.parkflow.mall.parking.security;

import com.parkflow.mall.parking.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtVerifier {
    private final SecretKey signingKey;

    public JwtVerifier(@Value("${app.security.jwt-secret}") String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        List<?> rawRoles = claims.get("roles", List.class);
        List<Role> roles = rawRoles.stream().map(Object::toString).map(roleName -> Role.valueOf(roleName)).toList();
        return new AuthenticatedUser(
                claims.get("userId", String.class),
                claims.getSubject(),
                claims.get("displayName", String.class),
                roles);
    }
}
