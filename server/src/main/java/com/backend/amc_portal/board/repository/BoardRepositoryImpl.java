package com.backend.amc_portal.board.repository;

import com.backend.amc_portal.board.entity.Board;
import com.backend.amc_portal.board.entity.QBoard;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom {

  private final JPAQueryFactory query;

  @Override
  public Page<Board> search(String keyword, Long boardMasterId, Pageable pageable) {
    QBoard b = QBoard.board;
    BooleanBuilder where = new BooleanBuilder();
    where.and(b.deletedAt.isNull());
    if (boardMasterId != null) {
      where.and(b.boardMaster.id.eq(boardMasterId));
    }
    if (keyword != null && !keyword.isBlank()) {
      String like = "%" + keyword.trim() + "%";
      where.and(b.title.likeIgnoreCase(like).or(b.content.likeIgnoreCase(like)));
    }

    List<Board> content =
        query
            .selectFrom(b)
            .where(where)
            .orderBy(b.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    return PageableExecutionUtils.getPage(
        content,
        pageable,
        () -> {
          Long c = query.select(b.count()).from(b).where(where).fetchOne();
          return c == null ? 0L : c;
        });
  }
}
