package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.BoardMaster;
import com.backend.amc_portal.board.entity.BoardType;
import java.time.OffsetDateTime;

public record BoardMasterResponse(
    Long id,
    String title,
    String description,
    BoardType boardType,
    Long authorId,
    String authorName,
    boolean fileYn,
    int fileMaxCount,
    boolean commentYn,
    boolean useYn,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static BoardMasterResponse from(BoardMaster bm) {
    return new BoardMasterResponse(
        bm.getId(),
        bm.getTitle(),
        bm.getDescription(),
        bm.getBoardType(),
        bm.getAuthor().getId(),
        bm.getAuthor().getDisplayName(),
        bm.isFileYn(),
        bm.getFileMaxCount(),
        bm.isCommentYn(),
        bm.isUseYn(),
        bm.getCreatedAt(),
        bm.getUpdatedAt());
  }
}
