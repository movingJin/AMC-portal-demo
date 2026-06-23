package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.BoardFileHistory;
import com.backend.amc_portal.board.enums.BoardFileEventType;
import java.time.OffsetDateTime;

public record BoardFileHistoryResponse(
    Long id,
    Long fileId,
    String originalName,
    BoardFileEventType eventType,
    String actedByName,
    OffsetDateTime actedAt) {

  public static BoardFileHistoryResponse from(BoardFileHistory h) {
    return new BoardFileHistoryResponse(
        h.getId(),
        h.getFileId(),
        h.getOriginalName(),
        h.getEventType(),
        h.getActedBy().getDisplayName(),
        h.getActedAt());
  }
}
