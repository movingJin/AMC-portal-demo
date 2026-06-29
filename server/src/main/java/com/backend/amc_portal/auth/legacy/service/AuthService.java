package com.backend.amc_portal.auth.legacy.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.entity.UserRole;
import com.backend.amc_portal.auth.entity.UserStatus;
import com.backend.amc_portal.auth.legacy.dto.ForgotPasswordRequest;
import com.backend.amc_portal.auth.legacy.dto.LoginRequest;
import com.backend.amc_portal.auth.legacy.dto.RefreshTokenRequest;
import com.backend.amc_portal.auth.legacy.dto.ResetPasswordRequest;
import com.backend.amc_portal.auth.legacy.dto.SignupRequest;
import com.backend.amc_portal.auth.legacy.dto.TokenResponse;
import com.backend.amc_portal.auth.legacy.dto.VerifyEmailRequest;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.common.exception.ApiException;
import com.backend.amc_portal.common.security.legacy.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("legacy")
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final RedisTokenService redisTokenService;
  private final EmailService emailService;

  private static final SecureRandom RAND = new SecureRandom();

  @Value("${app.auth.require-email-verification}")
  private boolean requireEmailVerification;

  @Value("${app.auth.email-verification-ttl-seconds}")
  private long emailVerificationTtl;

  @Value("${app.auth.password-reset-ttl-seconds}")
  private long passwordResetTtl;

  @Value("${app.auth.reset-link-base}")
  private String resetLinkBase;

  @Transactional
  public void signup(SignupRequest req) {
    if (!req.password().equals(req.passwordConfirm())) {
      throw ApiException.badRequest("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }
    if (userRepository.existsByEmail(req.email())) {
      throw ApiException.conflict("이미 등록된 이메일입니다.");
    }
    UserStatus initialStatus =
        requireEmailVerification ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE;

    User user =
        User.builder()
            .email(req.email())
            .displayName(req.displayName())
            .passwordHash(passwordEncoder.encode(req.password()))
            .role(UserRole.USER)
            .status(initialStatus)
            .build();
    userRepository.save(user);

    if (requireEmailVerification) {
      String code = "%06d".formatted(RAND.nextInt(1_000_000));
      redisTokenService.storeEmailVerificationCode(req.email(), code, emailVerificationTtl);
      emailService.sendVerificationCode(req.email(), code);
    }
  }

  @Transactional
  public void verifyEmail(VerifyEmailRequest req) {
    String saved = redisTokenService.getEmailVerificationCode(req.email());
    if (saved == null) throw ApiException.badRequest("인증 코드가 만료되었거나 존재하지 않습니다.");
    if (!saved.equals(req.code())) throw ApiException.badRequest("인증 코드가 일치하지 않습니다.");

    User user =
        userRepository
            .findByEmail(req.email())
            .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));
    user.activate();
    redisTokenService.deleteEmailVerificationCode(req.email());
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest req) {
    User user =
        userRepository
            .findByEmail(req.email())
            .orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다."));
    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      throw ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
    if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
      throw ApiException.forbidden("이메일 인증이 필요합니다.");
    }
    if (user.getStatus() == UserStatus.DISABLED) {
      throw ApiException.forbidden("비활성화된 계정입니다.");
    }
    return issueTokens(user);
  }

  public TokenResponse refresh(RefreshTokenRequest req) {
    Claims claims;
    try {
      claims = tokenProvider.parse(req.refreshToken());
    } catch (Exception e) {
      throw ApiException.unauthorized("리프레시 토큰이 유효하지 않습니다.");
    }
    if (!"refresh".equals(claims.get("type", String.class))) {
      throw ApiException.unauthorized("리프레시 토큰이 아닙니다.");
    }
    long userId = Long.parseLong(claims.getSubject());
    String stored = redisTokenService.getRefreshToken(userId);
    if (stored == null || !stored.equals(req.refreshToken())) {
      throw ApiException.unauthorized("리프레시 토큰이 만료되었거나 폐기되었습니다.");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    return issueTokens(user);
  }

  public void logout(String accessToken, Long userId) {
    try {
      Claims claims = tokenProvider.parse(accessToken);
      long ttl =
          Math.max(0L, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
      if (ttl > 0) redisTokenService.blacklistAccessToken(accessToken, ttl);
    } catch (Exception ignored) {
    }
    if (userId != null) redisTokenService.deleteRefreshToken(userId);
  }

  @Transactional(readOnly = true)
  public void forgotPassword(ForgotPasswordRequest req) {
    userRepository
        .findByEmail(req.email())
        .ifPresent(
            user -> {
              String token = UUID.randomUUID().toString().replace("-", "");
              redisTokenService.storePasswordResetToken(token, user.getEmail(), passwordResetTtl);
              String link = resetLinkBase + "?token=" + token;
              emailService.sendPasswordResetLink(user.getEmail(), link);
            });
    // 의도적으로 동일 응답 — enumeration 공격 방지
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest req) {
    String email = redisTokenService.consumePasswordResetToken(req.token());
    if (email == null) throw ApiException.badRequest("재설정 토큰이 만료되었거나 유효하지 않습니다.");
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));
    user.changePassword(passwordEncoder.encode(req.newPassword()));
  }

  private TokenResponse issueTokens(User user) {
    String access =
        tokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    String refresh =
        tokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name());
    redisTokenService.storeRefreshToken(
        user.getId(), refresh, tokenProvider.getRefreshTtlSeconds());
    return new TokenResponse(
        access,
        refresh,
        tokenProvider.getAccessTtlSeconds(),
        new TokenResponse.UserSummary(
            user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name()));
  }
}
