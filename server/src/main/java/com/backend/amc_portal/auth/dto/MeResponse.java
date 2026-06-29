package com.backend.amc_portal.auth.dto;

public record MeResponse(Long id, String email, String displayName, String role) {}
