package com.backend.amc_portal.board.entity;

import com.backend.amc_portal.auth.entity.User;
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
@Table(name = "board_file_downloads", schema = "portal")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardFileDownload {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_id", nullable = false)
  private Long fileId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 255)
  private String storedName;

  @Column(name = "storage_path", nullable = false, length = 500)
  private String storagePath;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @CreatedDate
  @Column(name = "downloaded_at", updatable = false, nullable = false)
  private OffsetDateTime downloadedAt;

  @Builder
  public BoardFileDownload(
      Long fileId,
      Board board,
      String originalName,
      String storedName,
      String storagePath,
      User user,
      String ipAddress) {
    this.fileId = fileId;
    this.board = board;
    this.originalName = originalName;
    this.storedName = storedName;
    this.storagePath = storagePath;
    this.user = user;
    this.ipAddress = ipAddress;
  }
}
