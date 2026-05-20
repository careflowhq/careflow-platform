package com.careflow.apigateway.service;

import com.careflow.apigateway.security.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${careflow.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtClaims validateAndExtractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            UUID clinicId = claims.get("clinicId", UUID.class);
            String role = claims.get("role", String.class);

            if (userId == null || clinicId == null || role == null) {
                throw new JwtException("Missing required JWT claims");
            }

            return new JwtClaims(UUID.fromString(userId), clinicId, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid or expired JWT", ex);
        }
    }
}
