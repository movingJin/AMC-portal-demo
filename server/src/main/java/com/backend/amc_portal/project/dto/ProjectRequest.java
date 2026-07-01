package com.backend.amc_portal.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
