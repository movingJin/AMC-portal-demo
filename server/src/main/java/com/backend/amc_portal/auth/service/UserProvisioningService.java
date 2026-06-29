package com.backend.amc_portal.auth.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.entity.UserRole;
import com.backend.amc_portal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("keycloak")
@RequiredArgsConstructor
public class UserProvisioningService {

  private final UserRepository userRepository;

  @Transactional
  public User provision(String keycloakId, String email, String displayName, UserRole role) {
    return userRepository
        .findByKeycloakId(keycloakId)
        .map(
            existing -> {
              existing.syncFromKeycloak(displayName, role);
              return existing;
            })
        .orElseGet(
            () -> userRepository.save(User.fromKeycloak(keycloakId, email, displayName, role)));
  }
}
