package com.backend.amc_portal.board.entity;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "board_masters", schema = "portal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardMaster extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private BoardType boardType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(nullable = false)
  private boolean fileYn = false;

  @Column(nullable = false)
  private int fileMaxCount = 0;

  @Column(nullable = false)
  private boolean commentYn = false;

  @Column(nullable = false)
  private boolean useYn = true;

  @Builder
  public BoardMaster(
      String title,
      String description,
      BoardType boardType,
      User author,
      boolean fileYn,
      int fileMaxCount,
      boolean commentYn,
      boolean useYn) {
    this.title = title;
    this.description = description;
    this.boardType = boardType;
    this.author = author;
    this.fileYn = fileYn;
    this.fileMaxCount = fileMaxCount;
    this.commentYn = commentYn;
    this.useYn = useYn;
  }

  public void update(
      String title,
      String description,
      boolean fileYn,
      int fileMaxCount,
      boolean commentYn,
      boolean useYn) {
    this.title = title;
    this.description = description;
    this.fileYn = fileYn;
    this.fileMaxCount = fileMaxCount;
    this.commentYn = commentYn;
    this.useYn = useYn;
  }

  public void toggleUseYn() {
    this.useYn = !this.useYn;
  }
}
