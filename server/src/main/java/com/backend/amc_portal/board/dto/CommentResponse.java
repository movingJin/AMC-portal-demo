package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.Comment;
import java.time.OffsetDateTime;

public record CommentResponse(
    Long id,
    Long boardId,
    Long createdById,
    String createdByName,
    Long updatedById,
    String updatedByName,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static CommentResponse from(Comment c) {
    return new CommentResponse(
        c.getId(),
        c.getBoard().getId(),
        c.getCreatedBy().getId(),
        c.getCreatedBy().getDisplayName(),
        c.getUpdatedBy() != null ? c.getUpdatedBy().getId() : null,
        c.getUpdatedBy() != null ? c.getUpdatedBy().getDisplayName() : null,
        c.getContent(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
