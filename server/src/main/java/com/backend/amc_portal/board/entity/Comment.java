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
@Table(name = "comments", schema = "portal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Lob
  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Builder
  public Comment(Board board, User author, String content) {
    this.board = board;
    this.author = author;
    this.content = content;
  }

  public void updateContent(String content) {
    this.content = content;
  }
}
