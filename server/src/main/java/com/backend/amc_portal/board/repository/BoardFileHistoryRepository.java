package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.BoardFileHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardFileHistoryRepository extends JpaRepository<BoardFileHistory, Long> {

  @Query(
      "SELECT h FROM BoardFileHistory h JOIN FETCH h.actedBy WHERE h.board.id = :boardId ORDER BY h.actedAt DESC")
  List<BoardFileHistory> findByBoardIdWithActedBy(@Param("boardId") Long boardId);

  void deleteByBoardId(Long boardId);
}
