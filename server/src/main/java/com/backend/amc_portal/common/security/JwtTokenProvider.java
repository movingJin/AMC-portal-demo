package com.backend.amc_portal.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret:}")
  private String secret;

  @Value("${app.jwt.access-token-ttl-seconds}")
  private long accessTtlSeconds;

  @Value("${app.jwt.refresh-token-ttl-seconds}")
  private long refreshTtlSeconds;

  private SecretKey key;

  @PostConstruct
  void init() {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET is not set in .env (HS256+, 32+ bytes required)");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
  }

  public String createAccessToken(Long userId, String email, String role) {
    return build(userId, email, role, "access", accessTtlSeconds);
  }

  public String createRefreshToken(Long userId, String email, String role) {
    return build(userId, email, role, "refresh", refreshTtlSeconds);
  }

  public long getRefreshTtlSeconds() {
    return refreshTtlSeconds;
  }

  public long getAccessTtlSeconds() {
    return accessTtlSeconds;
  }

  private String build(Long userId, String email, String role, String type, long ttl) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .claim("role", role)
        .claim("type", type)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + ttl * 1000))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    } catch (JwtException e) {
      throw new io.jsonwebtoken.JwtException("Invalid JWT: " + e.getMessage(), e);
    }
  }
}
