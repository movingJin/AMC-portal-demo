package com.backend.amc_portal.board.entity;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.board.enums.BoardFileEventType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "board_file_history", schema = "portal")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardFileHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_id", nullable = false)
  private Long fileId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 10)
  private BoardFileEventType eventType;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 255)
  private String storedName;

  @Column(name = "storage_path", nullable = false, length = 500)
  private String storagePath;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "acted_by", nullable = false)
  private User actedBy;

  @CreatedDate
  @Column(name = "acted_at", updatable = false, nullable = false)
  private OffsetDateTime actedAt;

  @Builder
  public BoardFileHistory(
      Long fileId,
      Board board,
      BoardFileEventType eventType,
      String originalName,
      String storedName,
      String storagePath,
      long fileSize,
      String contentType,
      User actedBy) {
    this.fileId = fileId;
    this.board = board;
    this.eventType = eventType;
    this.originalName = originalName;
    this.storedName = storedName;
    this.storagePath = storagePath;
    this.fileSize = fileSize;
    this.contentType = contentType;
    this.actedBy = actedBy;
  }
}
