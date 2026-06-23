package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.enums.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardMasterRequest(
    @NotBlank @Size(max = 100) String title,
    @Size(max = 500) String description,
    @NotNull BoardType type,
    boolean fileYn,
    int fileMaxCount,
    boolean commentYn,
    boolean useYn) {}
