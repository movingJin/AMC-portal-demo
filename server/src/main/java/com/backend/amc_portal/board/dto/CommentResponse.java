package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.Comment;
import java.time.OffsetDateTime;

public record CommentResponse(
    Long id,
    Long boardId,
    Long authorId,
    String authorName,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static CommentResponse from(Comment c) {
    return new CommentResponse(
        c.getId(),
        c.getBoard().getId(),
        c.getAuthor().getId(),
        c.getAuthor().getDisplayName(),
        c.getContent(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
