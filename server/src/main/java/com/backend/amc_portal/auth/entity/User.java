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

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false, length = 100)
  private String displayName;

  @Column(nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserRole role = UserRole.USER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status = UserStatus.PENDING_VERIFICATION;

  @Builder
  public User(
      String email, String displayName, String passwordHash, UserRole role, UserStatus status) {
    this.email = email;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.role = role != null ? role : UserRole.USER;
    this.status = status != null ? status : UserStatus.PENDING_VERIFICATION;
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
}
