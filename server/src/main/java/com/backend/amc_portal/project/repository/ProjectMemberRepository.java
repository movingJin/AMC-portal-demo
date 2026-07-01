package com.backend.amc_portal.project.repository;

import com.backend.amc_portal.project.entity.ProjectMember;
import com.backend.amc_portal.project.enums.ProjectRole;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
  @EntityGraph(attributePaths = {"user"})
  List<ProjectMember> findAllByProjectId(Long projectId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, ProjectRole role);

  long countByProjectIdAndRole(Long projectId, ProjectRole role);
}
