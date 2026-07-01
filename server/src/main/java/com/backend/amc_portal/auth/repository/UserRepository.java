package com.backend.amc_portal.auth.repository;

import com.backend.amc_portal.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByKeycloakId(String keycloakId);

  boolean existsByEmail(String email);

  @Query(
      "SELECT u FROM User u"
          + " WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))"
          + " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))"
          + " ORDER BY u.displayName ASC")
  List<User> searchByKeyword(@Param("q") String q, Pageable pageable);
}
