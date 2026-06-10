package com.backend.amc_portal.chatbot.agent;

import com.backend.amc_portal.chatbot.client.Nl2SqlClient;
import com.backend.amc_portal.chatbot.dto.PriorError;
import com.backend.amc_portal.chatbot.dto.SqlExecutorResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sub-agent: SQL 실행 + 결과 분류.
 *
 * <p>NOTE: 스펙은 LLM 기반 sub-agent 로 정의되어 있으나 결정론적 로직이라 Java 로 구현하여 LLM 호출 비용을 절감. 분기는 동일: OK (rows >
 * 0) RETRY_AUTHOR (UNSAFE_SQL / TIMEOUT / SQL_ERROR / ZERO_ROWS_WITH_VALUE_FILTER) FAIL (재시도 2회 초과시
 * 메인이 호출)
 *
 * <p>sql-author 재호출은 메인의 책임 — 여기서는 신호만 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlExecutorAgent {

  private final Nl2SqlClient nl2SqlClient;

  // _cd / _nm / dtl_cd_nm / erp_dtl_cd / _kor_nm / _eng_nm + 리터럴 값 패턴 (= 또는 ILIKE)
  private static final Pattern VALUE_FILTER_PATTERN =
      Pattern.compile(
          "(?i)(\\w*(?:_cd|_nm|_kor_nm|_eng_nm|dtl_cd_nm|erp_dtl_cd))\\s*(?:=|ILIKE)\\s*'([^']+)'");

  public SqlExecutorResult execute(String sql) {
    Map<String, Object> raw = nl2SqlClient.executeSql(sql, 100);

    // 에러 응답 패턴 (Nl2SqlClient 는 실패 시 {"error": "..."} 반환)
    if (raw.containsKey("error")) {
      String msg = String.valueOf(raw.get("error"));
      String code = classifyError(msg);
      return new SqlExecutorResult.RetryAuthor(PriorError.of(code, msg, sql), sql);
    }

    // 표준 응답 패턴
    Object status = raw.get("status");
    Object code = raw.get("code");
    if (code != null) {
      // status != 200 또는 UNSAFE_SQL 등
      String c = String.valueOf(code);
      String msg = String.valueOf(raw.getOrDefault("message", "execute_sql error"));
      return new SqlExecutorResult.RetryAuthor(PriorError.of(c, msg, sql), sql);
    }

    @SuppressWarnings("unchecked")
    List<String> columns =
        raw.get("columns") instanceof List<?> cl
            ? cl.stream().map(String::valueOf).toList()
            : List.of();
    @SuppressWarnings("unchecked")
    List<List<Object>> rows =
        raw.get("rows") instanceof List<?> rl ? ((List<List<Object>>) raw.get("rows")) : List.of();

    int rowCount = rows.size();

    // 행 0건 + 값 기반 필터 → 컬럼 선택 의심
    if (rowCount == 0) {
      SuspectFilter sf = detectSuspectValueFilter(sql);
      if (sf != null) {
        log.info("ZERO_ROWS_WITH_VALUE_FILTER detected: column={}, value={}", sf.column, sf.value);
        return new SqlExecutorResult.RetryAuthor(
            PriorError.zeroRows(sf.column, sf.value, sql), sql);
      }
    }

    return new SqlExecutorResult.Ok(rowCount, columns, normalizeRows(rows), sql);
  }

  private String classifyError(String message) {
    if (message == null) return "SQL_ERROR";
    String lower = message.toLowerCase();
    if (lower.contains("unsafe")) return "UNSAFE_SQL";
    if (lower.contains("timeout")) return "TIMEOUT";
    if (lower.contains("syntax")) return "SQL_ERROR";
    return "SQL_ERROR";
  }

  private SuspectFilter detectSuspectValueFilter(String sql) {
    Matcher m = VALUE_FILTER_PATTERN.matcher(sql);
    if (m.find()) {
      return new SuspectFilter(m.group(1), m.group(2));
    }
    return null;
  }

  private List<List<Object>> normalizeRows(List<List<Object>> rows) {
    // 깊은 복사 (외부 변경 방지) — 그대로 리스트 반환
    List<List<Object>> out = new ArrayList<>(rows.size());
    for (List<Object> r : rows) out.add(new ArrayList<>(r));
    return out;
  }

  private record SuspectFilter(String column, String value) {}
}
