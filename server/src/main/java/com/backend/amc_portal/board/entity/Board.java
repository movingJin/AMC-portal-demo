package com.backend.amc_portal.board.entity;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "boards", schema = "portal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_master_id")
  private BoardMaster boardMaster;

  @Column(nullable = false)
  private long viewCount = 0L;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by")
  private User deletedBy;

  @Builder
  public Board(String title, String content, User createdBy, BoardMaster boardMaster) {
    this.title = title;
    this.content = content;
    this.createdBy = createdBy;
    this.boardMaster = boardMaster;
  }

  public void update(String title, String content, User updatedBy) {
    this.title = title;
    this.content = content;
    this.updatedBy = updatedBy;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }

  public void softDelete(User deletedBy) {
    this.deletedAt = OffsetDateTime.now();
    this.deletedBy = deletedBy;
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}
