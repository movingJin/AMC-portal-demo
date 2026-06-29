package com.backend.amc_portal.auth.repository;

import com.backend.amc_portal.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByKeycloakId(String keycloakId);

  boolean existsByEmail(String email);
}
