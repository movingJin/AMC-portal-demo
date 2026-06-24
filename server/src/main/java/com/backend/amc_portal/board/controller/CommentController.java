package com.backend.amc_portal.board.controller;

import com.backend.amc_portal.board.dto.CommentRequest;
import com.backend.amc_portal.board.dto.CommentResponse;
import com.backend.amc_portal.board.dto.CommentTreeResponse;
import com.backend.amc_portal.board.service.CommentService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @GetMapping("/api/board/{boardId}/comments")
  public ApiResponse<List<CommentTreeResponse>> list(@PathVariable Long boardId) {
    return ApiResponse.ok(commentService.list(boardId));
  }

  @PostMapping("/api/board/{boardId}/comments")
  public ApiResponse<CommentResponse> create(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId,
      @Valid @RequestBody CommentRequest req) {
    return ApiResponse.ok(commentService.create(p.id(), boardId, req));
  }

  @PutMapping("/api/comments/{id}")
  public ApiResponse<CommentResponse> update(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long id,
      @Valid @RequestBody CommentRequest req) {
    return ApiResponse.ok(commentService.update(p.id(), id, req));
  }

  @DeleteMapping("/api/comments/{id}")
  public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
    commentService.delete(p.id(), id);
    return ApiResponse.ok();
  }
}
