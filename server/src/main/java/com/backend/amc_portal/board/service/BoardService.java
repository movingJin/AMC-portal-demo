package com.backend.amc_portal.board.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.board.dto.BoardRequest;
import com.backend.amc_portal.board.dto.BoardResponse;
import com.backend.amc_portal.board.entity.Board;
import com.backend.amc_portal.board.entity.BoardMaster;
import com.backend.amc_portal.board.repository.BoardMasterRepository;
import com.backend.amc_portal.board.repository.BoardRepository;
import com.backend.amc_portal.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

  private final BoardRepository boardRepository;
  private final BoardMasterRepository boardMasterRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<BoardResponse> list(String keyword, Long boardMasterId, Pageable pageable) {
    return boardRepository.search(keyword, boardMasterId, pageable).map(BoardResponse::summary);
  }

  @Transactional
  public BoardResponse get(Long id) {
    Board b =
        boardRepository.findById(id).orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    b.incrementViewCount();
    return BoardResponse.from(b);
  }

  @Transactional
  public BoardResponse create(Long userId, BoardRequest req) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    BoardMaster boardMaster =
        boardMasterRepository
            .findById(req.boardMasterId())
            .orElseThrow(() -> ApiException.notFound("게시판을 찾을 수 없습니다."));
    Board board =
        Board.builder()
            .title(req.title())
            .content(req.content())
            .author(user)
            .boardMaster(boardMaster)
            .build();
    return BoardResponse.from(boardRepository.save(board));
  }

  @Transactional
  public BoardResponse update(Long userId, Long boardId, BoardRequest req) {
    Board board =
        boardRepository
            .findById(boardId)
            .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    checkOwner(board.getAuthor().getId(), userId);
    board.update(req.title(), req.content());
    return BoardResponse.from(board);
  }

  @Transactional
  public void delete(Long userId, Long boardId) {
    Board board =
        boardRepository
            .findById(boardId)
            .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    checkOwner(board.getAuthor().getId(), userId);
    boardRepository.delete(board);
  }

  private void checkOwner(Long ownerId, Long actorId) {
    if (!ownerId.equals(actorId)) throw ApiException.forbidden("권한이 없습니다.");
  }
}
