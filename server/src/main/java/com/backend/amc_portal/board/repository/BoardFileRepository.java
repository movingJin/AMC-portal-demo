package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.BoardFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {

  List<BoardFile> findByBoardIdOrderByIdAsc(Long boardId);

  void deleteByBoardId(Long boardId);
}
