package com.backend.amc_portal.project.dto;

import com.backend.amc_portal.project.entity.Project;
import java.time.OffsetDateTime;

public record ProjectResponse(
    Long id, String name, String description, String createdByName, OffsetDateTime createdAt) {

  public static ProjectResponse from(Project p) {
    return new ProjectResponse(
        p.getId(),
        p.getName(),
        p.getDescription(),
        p.getCreatedBy().getDisplayName(),
        p.getCreatedAt());
  }
}
