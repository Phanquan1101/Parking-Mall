package com.parkflow.mall.reservation.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final byte[] key; public JwtAuthenticationFilter(@Value("${app.security.jwt-secret}") String secret){key=secret.getBytes(StandardCharsets.UTF_8);}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException { String h=req.getHeader("Authorization"); if(h==null||!h.startsWith("Bearer ")){chain.doFilter(req,res);return;} try {Claims c=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(key)).build().parseSignedClaims(h.substring(7)).getPayload(); List<?> roles=c.get("roles",List.class); var authorities=roles.stream().map(Object::toString).map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList(); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(c.get("userId",String.class),null,authorities)); chain.doFilter(req,res);} catch(JwtException|IllegalArgumentException e){SecurityContextHolder.clearContext();res.sendError(401,"Invalid access token");} }
}
