package com.backend.amc_portal.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}
