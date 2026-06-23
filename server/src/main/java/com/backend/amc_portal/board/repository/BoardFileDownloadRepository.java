package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.BoardFileDownload;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardFileDownloadRepository extends JpaRepository<BoardFileDownload, Long> {

  @Query(
      "SELECT d FROM BoardFileDownload d JOIN FETCH d.user WHERE d.board.id = :boardId ORDER BY d.downloadedAt DESC")
  List<BoardFileDownload> findByBoardIdWithUser(@Param("boardId") Long boardId);

  void deleteByBoardId(Long boardId);
}
