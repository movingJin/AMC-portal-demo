package com.backend.amc_portal.auth.dto;

import com.backend.amc_portal.auth.entity.User;

public record UserSearchResponse(Long id, String displayName, String email) {

  public static UserSearchResponse from(User u) {
    return new UserSearchResponse(u.getId(), u.getDisplayName(), u.getEmail());
  }
}
