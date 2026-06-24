package com.backend.amc_portal.board.dto;

import com.backend.amc_portal.board.entity.Comment;
import java.time.OffsetDateTime;
import java.util.List;

public record CommentTreeResponse(
    Long id,
    Long boardId,
    Long parentId,
    Long createdById,
    String createdByName,
    Long updatedById,
    String updatedByName,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<CommentTreeResponse> children) {

  public static CommentTreeResponse from(Comment c, List<Comment> replies) {
    return new CommentTreeResponse(
        c.getId(),
        c.getBoard().getId(),
        c.getParent() != null ? c.getParent().getId() : null,
        c.getCreatedBy().getId(),
        c.getCreatedBy().getDisplayName(),
        c.getUpdatedBy() != null ? c.getUpdatedBy().getId() : null,
        c.getUpdatedBy() != null ? c.getUpdatedBy().getDisplayName() : null,
        c.getContent(),
        c.getCreatedAt(),
        c.getUpdatedAt(),
        replies.stream().map(r -> CommentTreeResponse.from(r, List.of())).toList());
  }
}
