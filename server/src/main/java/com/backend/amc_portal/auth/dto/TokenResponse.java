package com.backend.amc_portal.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn, UserSummary user) {

    public record UserSummary(Long id, String email, String displayName, String role) {}
}
