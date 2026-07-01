package com.backend.amc_portal.project.dto;

import com.backend.amc_portal.project.entity.ProjectMember;
import com.backend.amc_portal.project.enums.ProjectRole;
import java.time.OffsetDateTime;

public record ProjectMemberResponse(
    Long id,
    Long userId,
    String displayName,
    String email,
    ProjectRole role,
    OffsetDateTime joinedAt) {

  public static ProjectMemberResponse from(ProjectMember m) {
    return new ProjectMemberResponse(
        m.getId(),
        m.getUser().getId(),
        m.getUser().getDisplayName(),
        m.getUser().getEmail(),
        m.getRole(),
        m.getJoinedAt());
  }
}
