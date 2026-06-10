package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
  List<Comment> findByBoardIdOrderByIdAsc(Long boardId);
}
