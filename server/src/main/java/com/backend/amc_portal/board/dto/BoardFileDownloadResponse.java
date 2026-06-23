package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.BoardFileDownload;
import java.time.OffsetDateTime;

public record BoardFileDownloadResponse(
    Long id,
    Long fileId,
    String originalName,
    String userName,
    String ipAddress,
    OffsetDateTime downloadedAt) {

  public static BoardFileDownloadResponse from(BoardFileDownload d) {
    return new BoardFileDownloadResponse(
        d.getId(),
        d.getFileId(),
        d.getOriginalName(),
        d.getUser().getDisplayName(),
        d.getIpAddress(),
        d.getDownloadedAt());
  }
}
