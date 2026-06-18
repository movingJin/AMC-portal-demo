package com.backend.amc_portal.board.controller;

import com.backend.amc_portal.board.dto.BoardMasterRequest;
import com.backend.amc_portal.board.dto.BoardMasterResponse;
import com.backend.amc_portal.board.service.BoardMasterService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/board-master")
@RequiredArgsConstructor
public class BoardMasterController {

  private final BoardMasterService boardMasterService;

  @GetMapping
  public ApiResponse<Page<BoardMasterResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(boardMasterService.list(keyword, PageRequest.of(page, Math.min(size, 100))));
  }

  @GetMapping("/{id}")
  public ApiResponse<BoardMasterResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(boardMasterService.get(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<BoardMasterResponse> update(
      @PathVariable Long id, @Valid @RequestBody BoardMasterRequest req) {
    return ApiResponse.ok(boardMasterService.update(id, req));
  }

  @PostMapping
  public ApiResponse<BoardMasterResponse> create(
      @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody BoardMasterRequest req) {
    return ApiResponse.ok(boardMasterService.create(p.id(), req));
  }

  @PatchMapping("/{id}/use-yn")
  public ApiResponse<BoardMasterResponse> toggleUseYn(@PathVariable Long id) {
    return ApiResponse.ok(boardMasterService.toggleUseYn(id));
  }
}
