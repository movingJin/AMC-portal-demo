package com.backend.amc_portal.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate redis;

    private static final String REFRESH_KEY = "auth:refresh:%d";
    private static final String VERIFY_KEY = "auth:verify:%s";
    private static final String RESET_KEY = "auth:reset:%s";
    private static final String BLACKLIST_KEY = "jwt:blacklist:%s";

    public void storeRefreshToken(long userId, String token, long ttlSeconds) {
        redis.opsForValue().set(REFRESH_KEY.formatted(userId), token, Duration.ofSeconds(ttlSeconds));
    }

    public String getRefreshToken(long userId) {
        return redis.opsForValue().get(REFRESH_KEY.formatted(userId));
    }

    public void deleteRefreshToken(long userId) {
        redis.delete(REFRESH_KEY.formatted(userId));
    }

    public void blacklistAccessToken(String token, long ttlSeconds) {
        redis.opsForValue().set(BLACKLIST_KEY.formatted(token), "1", Duration.ofSeconds(ttlSeconds));
    }

    public void storeEmailVerificationCode(String email, String code, long ttlSeconds) {
        redis.opsForValue().set(VERIFY_KEY.formatted(email), code, Duration.ofSeconds(ttlSeconds));
    }

    public String getEmailVerificationCode(String email) {
        return redis.opsForValue().get(VERIFY_KEY.formatted(email));
    }

    public void deleteEmailVerificationCode(String email) {
        redis.delete(VERIFY_KEY.formatted(email));
    }

    public void storePasswordResetToken(String token, String email, long ttlSeconds) {
        redis.opsForValue().set(RESET_KEY.formatted(token), email, Duration.ofSeconds(ttlSeconds));
    }

    public String consumePasswordResetToken(String token) {
        String key = RESET_KEY.formatted(token);
        String email = redis.opsForValue().get(key);
        if (email != null) redis.delete(key);
        return email;
    }
}
