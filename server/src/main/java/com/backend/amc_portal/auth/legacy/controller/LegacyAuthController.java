package com.backend.amc_portal.auth.legacy.controller;

import com.backend.amc_portal.auth.legacy.dto.ForgotPasswordRequest;
import com.backend.amc_portal.auth.legacy.dto.LoginRequest;
import com.backend.amc_portal.auth.legacy.dto.RefreshTokenRequest;
import com.backend.amc_portal.auth.legacy.dto.ResetPasswordRequest;
import com.backend.amc_portal.auth.legacy.dto.SignupRequest;
import com.backend.amc_portal.auth.legacy.dto.TokenResponse;
import com.backend.amc_portal.auth.legacy.dto.VerifyEmailRequest;
import com.backend.amc_portal.auth.legacy.service.AuthService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("legacy")
@RequiredArgsConstructor
public class LegacyAuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest req) {
    authService.signup(req);
    return ApiResponse.ok();
  }

  @PostMapping("/verify-email")
  public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
    authService.verifyEmail(req);
    return ApiResponse.ok();
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
    return ApiResponse.ok(authService.login(req));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    return ApiResponse.ok(authService.refresh(req));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      HttpServletRequest request, @AuthenticationPrincipal UserPrincipal principal) {
    String header = request.getHeader("Authorization");
    String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    authService.logout(token, principal != null ? principal.id() : null);
    return ApiResponse.ok();
  }

  @PostMapping("/forgot-password")
  public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
    authService.forgotPassword(req);
    return ApiResponse.ok();
  }

  @PostMapping("/reset-password")
  public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
    authService.resetPassword(req);
    return ApiResponse.ok();
  }
}
