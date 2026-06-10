package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {}
