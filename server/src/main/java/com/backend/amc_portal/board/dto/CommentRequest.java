package com.backend.amc_portal.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(Long parentId, @NotBlank @Size(max = 2000) String content) {}
