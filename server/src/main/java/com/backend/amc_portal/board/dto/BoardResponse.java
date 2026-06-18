package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.Board;
import java.time.OffsetDateTime;

public record BoardResponse(
    Long id,
    String title,
    String content,
    Long authorId,
    String authorName,
    long viewCount,
    Long boardMasterId,
    String boardMasterTitle,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static BoardResponse from(Board b) {
    return new BoardResponse(
        b.getId(),
        b.getTitle(),
        b.getContent(),
        b.getAuthor().getId(),
        b.getAuthor().getDisplayName(),
        b.getViewCount(),
        b.getBoardMaster() != null ? b.getBoardMaster().getId() : null,
        b.getBoardMaster() != null ? b.getBoardMaster().getTitle() : null,
        b.getCreatedAt(),
        b.getUpdatedAt());
  }

  public static BoardResponse summary(Board b) {
    return new BoardResponse(
        b.getId(),
        b.getTitle(),
        null,
        b.getAuthor().getId(),
        b.getAuthor().getDisplayName(),
        b.getViewCount(),
        b.getBoardMaster() != null ? b.getBoardMaster().getId() : null,
        b.getBoardMaster() != null ? b.getBoardMaster().getTitle() : null,
        b.getCreatedAt(),
        b.getUpdatedAt());
  }
}
