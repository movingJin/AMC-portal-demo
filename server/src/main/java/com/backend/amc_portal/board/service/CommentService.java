package com.backend.amc_portal.board.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.board.dto.CommentRequest;
import com.backend.amc_portal.board.dto.CommentResponse;
import com.backend.amc_portal.board.entity.Board;
import com.backend.amc_portal.board.entity.Comment;
import com.backend.amc_portal.board.repository.BoardRepository;
import com.backend.amc_portal.board.repository.CommentRepository;
import com.backend.amc_portal.common.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final BoardRepository boardRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<CommentResponse> list(Long boardId) {
    return commentRepository.findByBoardIdOrderByIdAsc(boardId).stream()
        .map(CommentResponse::from)
        .toList();
  }

  @Transactional
  public CommentResponse create(Long userId, Long boardId, CommentRequest req) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    Board board =
        boardRepository
            .findById(boardId)
            .orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
    Comment c = Comment.builder().board(board).createdBy(user).content(req.content()).build();
    return CommentResponse.from(commentRepository.save(c));
  }

  @Transactional
  public CommentResponse update(Long userId, Long commentId, CommentRequest req) {
    Comment c =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없습니다."));
    if (!c.getCreatedBy().getId().equals(userId)) throw ApiException.forbidden("권한이 없습니다.");
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    c.updateContent(req.content(), user);
    return CommentResponse.from(c);
  }

  @Transactional
  public void delete(Long userId, Long commentId) {
    Comment c =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없습니다."));
    if (!c.getCreatedBy().getId().equals(userId)) throw ApiException.forbidden("권한이 없습니다.");
    commentRepository.delete(c);
  }
}
