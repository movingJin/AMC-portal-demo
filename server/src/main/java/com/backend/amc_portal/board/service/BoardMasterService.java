package com.backend.amc_portal.board.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.board.dto.BoardMasterRequest;
import com.backend.amc_portal.board.dto.BoardMasterResponse;
import com.backend.amc_portal.board.entity.BoardMaster;
import com.backend.amc_portal.board.repository.BoardMasterRepository;
import com.backend.amc_portal.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardMasterService {

  private final BoardMasterRepository boardMasterRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<BoardMasterResponse> list(String keyword, Pageable pageable) {
    if (keyword != null && !keyword.isBlank()) {
      return boardMasterRepository
          .findByTitleContainingIgnoreCaseOrderByIdDesc(keyword, pageable)
          .map(BoardMasterResponse::from);
    }
    return boardMasterRepository.findAllByOrderByIdDesc(pageable).map(BoardMasterResponse::from);
  }

  @Transactional(readOnly = true)
  public BoardMasterResponse get(Long id) {
    BoardMaster bm =
        boardMasterRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("게시판을 찾을 수 없습니다."));
    return BoardMasterResponse.from(bm);
  }

  @Transactional
  public BoardMasterResponse update(Long id, BoardMasterRequest req) {
    BoardMaster bm =
        boardMasterRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("게시판을 찾을 수 없습니다."));
    bm.update(req.title(), req.description(), req.fileYn(), req.fileMaxCount(), req.commentYn(), req.useYn());
    return BoardMasterResponse.from(bm);
  }

  @Transactional
  public BoardMasterResponse create(Long userId, BoardMasterRequest req) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    BoardMaster bm =
        BoardMaster.builder()
            .title(req.title())
            .description(req.description())
            .boardType(req.type())
            .author(user)
            .fileYn(req.fileYn())
            .fileMaxCount(req.fileMaxCount())
            .commentYn(req.commentYn())
            .useYn(req.useYn())
            .build();
    return BoardMasterResponse.from(boardMasterRepository.save(bm));
  }

  @Transactional
  public BoardMasterResponse toggleUseYn(Long id) {
    BoardMaster bm =
        boardMasterRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("게시판을 찾을 수 없습니다."));
    bm.toggleUseYn();
    return BoardMasterResponse.from(bm);
  }
}
