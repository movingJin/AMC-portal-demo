package com.backend.amc_portal.project.entity;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.project.enums.ProjectRole;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_members", schema = "portal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ProjectRole role;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private OffsetDateTime joinedAt = OffsetDateTime.now();

  @Builder
  public ProjectMember(Project project, User user, ProjectRole role) {
    this.project = project;
    this.user = user;
    this.role = role;
  }

  public void updateRole(ProjectRole newRole) {
    this.role = newRole;
  }
}
