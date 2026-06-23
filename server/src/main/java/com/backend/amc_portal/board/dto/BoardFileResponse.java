package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.BoardFile;
import java.time.OffsetDateTime;

public record BoardFileResponse(
    Long id,
    Long boardId,
    String originalName,
    String contentType,
    long fileSize,
    OffsetDateTime createdAt) {

  public static BoardFileResponse from(BoardFile f) {
    return new BoardFileResponse(
        f.getId(),
        f.getBoard().getId(),
        f.getOriginalName(),
        f.getContentType(),
        f.getFileSize(),
        f.getCreatedAt());
  }
}
