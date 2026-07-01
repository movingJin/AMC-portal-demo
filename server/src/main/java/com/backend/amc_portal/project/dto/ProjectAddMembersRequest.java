package com.backend.amc_portal.project.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProjectAddMembersRequest(@NotEmpty List<@NotNull Long> userIds) {}
