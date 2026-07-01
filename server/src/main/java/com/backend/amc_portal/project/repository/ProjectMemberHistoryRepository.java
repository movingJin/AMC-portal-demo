package com.backend.amc_portal.project.repository;

import com.backend.amc_portal.project.entity.ProjectMemberHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberHistoryRepository extends JpaRepository<ProjectMemberHistory, Long> {

  @EntityGraph(attributePaths = {"user", "actedBy"})
  List<ProjectMemberHistory> findAllByProjectIdOrderByActedAtDesc(Long projectId);
}
