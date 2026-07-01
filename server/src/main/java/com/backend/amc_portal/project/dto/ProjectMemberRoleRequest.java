package com.backend.amc_portal.project.dto;

import com.backend.amc_portal.project.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRoleRequest(@NotNull ProjectRole role) {}
