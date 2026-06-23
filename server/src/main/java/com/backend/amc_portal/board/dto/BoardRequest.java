package com.backend.amc_portal.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BoardRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String content,
    @NotNull Long boardMasterId,
    List<Long> deleteFileIds) {}
