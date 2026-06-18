package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardRepositoryCustom {
  Page<Board> search(String keyword, Long boardMasterId, Pageable pageable);
}
