package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.BoardMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardMasterRepository extends JpaRepository<BoardMaster, Long> {

  Page<BoardMaster> findAllByOrderByIdDesc(Pageable pageable);

  Page<BoardMaster> findByTitleContainingIgnoreCaseOrderByIdDesc(String title, Pageable pageable);
}
