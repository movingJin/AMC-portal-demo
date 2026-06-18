package com.backend.amc_portal.board.controller;

import com.backend.amc_portal.board.dto.BoardRequest;
import com.backend.amc_portal.board.dto.BoardResponse;
import com.backend.amc_portal.board.service.BoardService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

  private final BoardService boardService;

  @GetMapping
  public ApiResponse<Page<BoardResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam Long boardMasterId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable p = PageRequest.of(page, Math.min(size, 100));
    return ApiResponse.ok(boardService.list(keyword, boardMasterId, p));
  }

  @GetMapping("/{id}")
  public ApiResponse<BoardResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(boardService.get(id));
  }

  @PostMapping("/{id}/view")
  public ApiResponse<Void> incrementView(@PathVariable Long id) {
    boardService.incrementView(id);
    return ApiResponse.ok();
  }

  @PostMapping
  public ApiResponse<BoardResponse> create(
      @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody BoardRequest req) {
    return ApiResponse.ok(boardService.create(p.id(), req));
  }

  @PutMapping("/{id}")
  public ApiResponse<BoardResponse> update(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long id,
      @Valid @RequestBody BoardRequest req) {
    return ApiResponse.ok(boardService.update(p.id(), id, req));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
    boardService.delete(p.id(), id);
    return ApiResponse.ok();
  }
}
