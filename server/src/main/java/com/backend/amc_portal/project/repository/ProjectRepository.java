package com.backend.amc_portal.project.repository;

import com.backend.amc_portal.project.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
  List<Project> findAllByOrderByCreatedAtDesc();
}
