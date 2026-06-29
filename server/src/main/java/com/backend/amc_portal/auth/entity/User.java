package com.backend.amc_portal.auth.entity;

import com.backend.amc_portal.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "users",
    schema = "portal",
    uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 64)
  private String keycloakId;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false, length = 100)
  private String displayName;

  @Column(length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserRole role = UserRole.USER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status = UserStatus.PENDING_VERIFICATION;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @Builder
  public User(
      String keycloakId,
      String email,
      String displayName,
      String passwordHash,
      UserRole role,
      UserStatus status) {
    this.keycloakId = keycloakId;
    this.email = email;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.role = role != null ? role : UserRole.USER;
    this.status = status != null ? status : UserStatus.PENDING_VERIFICATION;
  }

  public static User fromKeycloak(
      String keycloakId, String email, String displayName, UserRole role) {
    return User.builder()
        .keycloakId(keycloakId)
        .email(email)
        .displayName(displayName)
        .role(role)
        .status(UserStatus.ACTIVE)
        .build();
  }

  public void activate() {
    this.status = UserStatus.ACTIVE;
  }

  public void changePassword(String newHash) {
    this.passwordHash = newHash;
  }

  public void updateDisplayName(String name) {
    this.displayName = name;
  }

  public void syncFromKeycloak(String displayName, UserRole role) {
    if (displayName != null && !displayName.equals(this.displayName)) {
      this.displayName = displayName;
    }
    if (role != null && role != this.role) {
      this.role = role;
    }
  }
}
