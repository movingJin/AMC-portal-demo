package com.backend.amc_portal.auth.controller;

import com.backend.amc_portal.auth.dto.MeResponse;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository userRepository;

  @GetMapping("/me")
  public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) return ApiResponse.fail("Not authenticated");
    String displayName =
        userRepository
            .findById(principal.id())
            .map(u -> u.getDisplayName())
            .orElse(principal.email());
    return ApiResponse.ok(
        new MeResponse(principal.id(), principal.email(), displayName, principal.role()));
  }
}
