package com.backend.amc_portal.board.controller;

import com.backend.amc_portal.board.dto.BoardFileDownloadResponse;
import com.backend.amc_portal.board.dto.BoardFileHistoryResponse;
import com.backend.amc_portal.board.dto.BoardFileResponse;
import com.backend.amc_portal.board.service.BoardFileService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/board/{boardId}/files")
@RequiredArgsConstructor
public class BoardFileController {

  private final BoardFileService boardFileService;

  @GetMapping
  public ApiResponse<List<BoardFileResponse>> list(@PathVariable Long boardId) {
    return ApiResponse.ok(boardFileService.list(boardId));
  }

  @GetMapping("/history")
  public ApiResponse<List<BoardFileHistoryResponse>> history(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId) {
    return ApiResponse.ok(boardFileService.listHistory(p.id(), boardId));
  }

  @GetMapping("/downloads")
  public ApiResponse<List<BoardFileDownloadResponse>> downloads(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId) {
    return ApiResponse.ok(boardFileService.listDownloads(p.id(), boardId));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<List<BoardFileResponse>> upload(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId,
      @RequestPart("files") List<MultipartFile> files) {
    return ApiResponse.ok(boardFileService.upload(p.id(), boardId, files));
  }

  @GetMapping("/{fileId}/download")
  public ResponseEntity<Resource> download(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId,
      @PathVariable Long fileId,
      HttpServletRequest request) {
    String ip = resolveClientIp(request);
    BoardFileService.DownloadResult result = boardFileService.download(fileId, p.id(), ip);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + result.originalName() + "\"")
        .contentType(MediaType.parseMediaType(result.contentType()))
        .body(result.resource());
  }

  @DeleteMapping("/{fileId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long boardId,
      @PathVariable Long fileId) {
    boardFileService.delete(p.id(), fileId);
    return ApiResponse.ok();
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
