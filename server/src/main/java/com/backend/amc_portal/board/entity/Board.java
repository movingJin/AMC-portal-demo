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
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_master_id")
  private BoardMaster boardMaster;

  @Column(nullable = false)
  private long viewCount = 0L;

  @Builder
  public Board(String title, String content, User author, BoardMaster boardMaster) {
    this.title = title;
    this.content = content;
    this.author = author;
    this.boardMaster = boardMaster;
  }

  public void update(String title, String content) {
    this.title = title;
    this.content = content;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }
}
