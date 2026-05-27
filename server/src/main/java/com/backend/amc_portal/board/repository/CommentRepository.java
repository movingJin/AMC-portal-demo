package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBoardIdOrderByIdAsc(Long boardId);
}
