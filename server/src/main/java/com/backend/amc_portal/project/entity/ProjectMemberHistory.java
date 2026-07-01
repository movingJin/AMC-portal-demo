package com.backend.amc_portal.project.entity;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.project.enums.ProjectMemberEventType;
import com.backend.amc_portal.project.enums.ProjectRole;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_member_history", schema = "portal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMemberHistory {

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

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 20)
  private ProjectMemberEventType eventType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "acted_by", nullable = false)
  private User actedBy;

  @Column(name = "acted_at", nullable = false, updatable = false)
  private OffsetDateTime actedAt = OffsetDateTime.now();

  @Builder
  public ProjectMemberHistory(
      Project project,
      User user,
      ProjectRole role,
      ProjectMemberEventType eventType,
      User actedBy) {
    this.project = project;
    this.user = user;
    this.role = role;
    this.eventType = eventType;
    this.actedBy = actedBy;
  }
}
