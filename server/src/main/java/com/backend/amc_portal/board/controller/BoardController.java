package com.backend.amc_portal.board.controller;

import com.backend.amc_portal.board.dto.BoardRequest;
import com.backend.amc_portal.board.dto.BoardResponse;
import com.backend.amc_portal.board.service.BoardService;
import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.exception.ApiException;
import com.backend.amc_portal.common.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

  private final BoardService boardService;
  private final ObjectMapper objectMapper;
  private final Validator validator;

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

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<BoardResponse> create(
      @AuthenticationPrincipal UserPrincipal p,
      @RequestPart("data") String dataJson,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    BoardRequest req = parseAndValidate(dataJson);
    return ApiResponse.ok(boardService.create(p.id(), req, files == null ? List.of() : files));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<BoardResponse> update(
      @AuthenticationPrincipal UserPrincipal p,
      @PathVariable Long id,
      @RequestPart("data") String dataJson,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    BoardRequest req = parseAndValidate(dataJson);
    return ApiResponse.ok(boardService.update(p.id(), id, req, files == null ? List.of() : files));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
    boardService.delete(p.id(), id);
    return ApiResponse.ok();
  }

  private BoardRequest parseAndValidate(String json) {
    BoardRequest req;
    try {
      req = objectMapper.readValue(json, BoardRequest.class);
    } catch (JsonProcessingException e) {
      throw ApiException.badRequest("요청 데이터 형식이 올바르지 않습니다.");
    }
    Set<ConstraintViolation<BoardRequest>> violations = validator.validate(req);
    if (!violations.isEmpty()) {
      throw ApiException.badRequest(violations.iterator().next().getMessage());
    }
    return req;
  }
}
