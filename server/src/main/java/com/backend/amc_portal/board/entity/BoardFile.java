package com.backend.amc_portal.board.entity;

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
@Table(name = "board_files", schema = "portal")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 255)
  private String storedName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "storage_path", nullable = false, length = 500)
  private String storagePath;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private OffsetDateTime createdAt;

  @Builder
  public BoardFile(
      Board board,
      String originalName,
      String storedName,
      String contentType,
      long fileSize,
      String storagePath) {
    this.board = board;
    this.originalName = originalName;
    this.storedName = storedName;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.storagePath = storagePath;
  }
}
