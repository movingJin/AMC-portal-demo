package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.BoardMaster;
import com.backend.amc_portal.board.enums.BoardType;
import java.time.OffsetDateTime;

public record BoardMasterResponse(
    Long id,
    String title,
    String description,
    BoardType boardType,
    Long createdById,
    String createdByName,
    Long updatedById,
    String updatedByName,
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
        bm.getCreatedBy().getId(),
        bm.getCreatedBy().getDisplayName(),
        bm.getUpdatedBy() != null ? bm.getUpdatedBy().getId() : null,
        bm.getUpdatedBy() != null ? bm.getUpdatedBy().getDisplayName() : null,
        bm.isFileYn(),
        bm.getFileMaxCount(),
        bm.isCommentYn(),
        bm.isUseYn(),
        bm.getCreatedAt(),
        bm.getUpdatedAt());
  }
}
