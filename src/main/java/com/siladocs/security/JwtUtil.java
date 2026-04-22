package com.siladocs.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${siladocs.jwt.secret}")
    private String jwtSecret;

    @Value("${siladocs.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Generar JWT con claims multi-tenant
    public String generateToken(String email,
                                String userId,
                                String institutionId,
                                String role) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("institutionId", institutionId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validar JWT y extraer claims
    public Claims validateAndExtractClaims(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return claims.getBody();
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido");
        }
    }

    public String extractEmail(String token) {
        return validateAndExtractClaims(token).getSubject();
    }
}
