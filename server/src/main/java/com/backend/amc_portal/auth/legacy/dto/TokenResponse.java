package com.backend.amc_portal.auth.legacy.dto;

public record TokenResponse(
    String accessToken, String refreshToken, long accessTokenExpiresIn, UserSummary user) {

  public record UserSummary(Long id, String email, String displayName, String role) {}
}
