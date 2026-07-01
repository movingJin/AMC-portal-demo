package com.backend.amc_portal.project.dto;

import com.backend.amc_portal.project.entity.ProjectMemberHistory;
import com.backend.amc_portal.project.enums.ProjectMemberEventType;
import com.backend.amc_portal.project.enums.ProjectRole;
import java.time.OffsetDateTime;

public record ProjectMemberHistoryResponse(
    Long id,
    String displayName,
    String email,
    ProjectRole role,
    ProjectMemberEventType eventType,
    String actedByName,
    OffsetDateTime actedAt) {

  public static ProjectMemberHistoryResponse from(ProjectMemberHistory h) {
    return new ProjectMemberHistoryResponse(
        h.getId(),
        h.getUser().getDisplayName(),
        h.getUser().getEmail(),
        h.getRole(),
        h.getEventType(),
        h.getActedBy().getDisplayName(),
        h.getActedAt());
  }
}
