package com.backend.amc_portal.chatbot.agent;

import com.backend.amc_portal.chatbot.client.AzureOpenAiClient;
import com.backend.amc_portal.chatbot.dto.PriorError;
import com.backend.amc_portal.chatbot.dto.SchemaBrief;
import com.backend.amc_portal.chatbot.dto.SqlAuthorResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sub-agent: SchemaBrief 기반 PostgreSQL SELECT 작성. 도구 없음 (순수 추론). 결과는 3-블록 마크다운 또는 "##
 * VALUE_UNCONFIRMED" 반려.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlAuthorAgent {

  private final AzureOpenAiClient azureClient;
  private final ObjectMapper om;

  private static final String SYSTEM_PROMPT =
      """
            당신은 PostgreSQL **SELECT 쿼리 작성 전문가** 입니다. 도구는 없습니다.
            입력으로 받은 schema_brief 와 question 만 보고 단일 SELECT 문 + SchemaCitation 을 반환합니다.

            # 출력 형식 (3-블록 모두 채우거나, 반려 형식 둘 중 하나)
            ## 의도
            <한 줄: 어느 테이블을 어떻게 조인하고 어떤 조건을 거는지>

            ## SchemaCitation
            - public.poc_xxx.colA : "..." (확인됨)
            - public.poc_yyy.colB : "→ FK ..." (확인됨)
            ※ 이 블록에 인용한 모든 컬럼은 schema_brief.tables[*].columns 에서 그대로 찾아져야 합니다.

            ## SQL
            ```sql
            SELECT ...
            FROM public.poc_xxx
            ...
            LIMIT 100;
            ```

            # 절대 규칙
            1) 단일 SELECT 만. CTE(WITH) 는 허용. DDL/DML/멀티문 금지.
            2) ORDER BY + LIMIT(기본 100) 항상.
            3) PostgreSQL 문법만. **SYSDATE / NVL / ROWNUM / DATE_FORMAT / QUALIFY 금지.**
               QUALIFY 가 필요해 보이면 반드시 CTE + ROW_NUMBER() + WHERE rn=1 로 작성.
            4) *_dt(TEXT 'YYYYMMDD') 비교는 TO_DATE(col,'YYYYMMDD'). occur_ym 은 'YYYYMM'.
            5) *_yn 필터는 그 테이블의 실제 컬럼명으로 (vald_yn / vald_op_yn / use_yn 혼동 금지).
            6) 한글 LIKE 는 ILIKE.
            7) JOIN 체크리스트:
               - 필요한 컬럼이 한 테이블(특히 분석 마트)에 다 있으면 JOIN 불필요
               - join_candidates 에 없는 임의 JOIN 금지
               - 복합 PK 테이블 JOIN 은 PK 전 컬럼을 ON 에
            8) schema_brief 에 없는 컬럼/테이블 등장 절대 금지.

            9) **[필수] 값-컬럼 검증 (Value Anchoring)** — 사용자 질문의 리터럴 값으로 WHERE 절을 쓸 때:
               - `WHERE <col> = '<리터럴>'` 또는 `WHERE <col> ILIKE '%<리터럴>%'` 의 컬럼은
                 반드시 schema_brief.value_anchors[*] 중 found=true 의 locations 에 등장해야 함.
               - value_anchors 가 비어 있거나 그 값이 found=false 이면 SQL 을 쓰지 말고
                 다음 형식으로 반려:
                 ```
                 ## VALUE_UNCONFIRMED
                 - 값: "PSA"
                 - 후보 컬럼: [poc_xxx.dtl_cd_nm, poc_yyy.erp_dtl_cd]
                 - 요청: schema-explorer 를 value_lookups=["PSA"] 로 재호출해 value_anchors 보강
                 ```
               - 컬럼명만 보고 의미 가정 금지. 예: `exam_cd` 라는 이름만 보고 `WHERE exam_cd='PSA'` 금지.
                 'PSA' 는 보통 `erp_dtl_cd` 또는 `dtl_cd_nm` 같은 상세 컬럼.

            # 재시도 (prior_error 가 있는 경우)
            - prior_error.code == "UNSAFE_SQL" : 단일 SELECT/허용 함수만 사용했는지 점검 후 재작성.
              QUALIFY 가 있었다면 1순위 의심 — CTE + ROW_NUMBER 로 치환.
            - prior_error.code == "TIMEOUT" : WHERE 좁히기, LIMIT 줄이기, 불필요 JOIN 제거.
            - 일반 SQL 에러 : 메시지의 컬럼/테이블명을 식별 → schema_brief 와 대조 → 수정.
            - prior_error.code == "ZERO_ROWS_WITH_VALUE_FILTER" : 결과 0건 + 값 기반 필터.
              **컬럼 선택이 잘못됐을 가능성이 가장 큼.** 같은 SQL 을 다시 쓰지 말고 VALUE_UNCONFIRMED
              로 반려해 value_anchors 재탐색을 요청하라.
            - prior_error.code == "CITATION_INVALID" : SchemaCitation 의 컬럼이 schema_brief 에 없음.
              schema_brief 의 컬럼만 사용해 재작성.

            # 금지
            - DB 도구 호출 금지(도구 없음).
            - schema_brief 외 컬럼·테이블 사용 금지.
            - 결과 추측·예측 금지.
            - value_anchors 없이 리터럴 값을 WHERE 에 박지 말 것 (가장 흔한 환각 경로).
            """;

  public SqlAuthorResult write(String question, SchemaBrief brief, PriorError priorError) {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("question", question);
    input.put("schema_brief", brief);
    if (priorError != null) input.put("prior_error", priorError);

    String userContent;
    try {
      userContent = om.writeValueAsString(input);
    } catch (Exception e) {
      userContent = question;
    }

    List<Map<String, Object>> messages =
        List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", userContent));

    JsonNode resp = azureClient.chatCompletion(messages, null);
    String content = resp.path("choices").path(0).path("message").path("content").asText("");
    return parseAuthorResult(content);
  }

  private SqlAuthorResult parseAuthorResult(String content) {
    String trimmed = content == null ? "" : content.trim();
    if (trimmed.contains("## VALUE_UNCONFIRMED")) {
      return parseValueUnconfirmed(trimmed);
    }

    String intent = extractBlock(trimmed, "## 의도", "## SchemaCitation");
    String citation = extractBlock(trimmed, "## SchemaCitation", "## SQL");
    String sql = extractSql(trimmed);

    if (sql == null || sql.isBlank()) {
      return new SqlAuthorResult.SchemaInsufficient(
          "sql-author 응답에서 SQL 블록을 찾지 못함: " + abbreviate(trimmed));
    }
    return new SqlAuthorResult.Authored(
        intent != null ? intent.trim() : "", citation != null ? citation.trim() : "", sql.trim());
  }

  private SqlAuthorResult.ValueUnconfirmed parseValueUnconfirmed(String content) {
    String value = matchFirst(content, Pattern.compile("값:\\s*\"([^\"]+)\""));
    if (value == null) value = matchFirst(content, Pattern.compile("값:\\s*'([^']+)'"));
    if (value == null) value = "";

    String candStr = matchFirst(content, Pattern.compile("후보 컬럼:\\s*\\[([^\\]]+)\\]"));
    List<String> candidates = new ArrayList<>();
    if (candStr != null) {
      for (String c : candStr.split(",")) {
        String t = c.trim();
        if (!t.isEmpty()) candidates.add(t);
      }
    }
    return new SqlAuthorResult.ValueUnconfirmed(value, candidates, content);
  }

  private String extractBlock(String content, String startMarker, String endMarker) {
    int s = content.indexOf(startMarker);
    if (s < 0) return null;
    int after = s + startMarker.length();
    int e = content.indexOf(endMarker, after);
    return (e < 0 ? content.substring(after) : content.substring(after, e));
  }

  private String extractSql(String content) {
    Matcher m = Pattern.compile("```(?:sql)?\\s*(.+?)```", Pattern.DOTALL).matcher(content);
    if (m.find()) return m.group(1);
    // fenceless fallback
    int idx = content.indexOf("## SQL");
    if (idx >= 0) {
      String tail = content.substring(idx + "## SQL".length()).trim();
      if (tail.toUpperCase().startsWith("SELECT") || tail.toUpperCase().startsWith("WITH"))
        return tail;
    }
    return null;
  }

  private String matchFirst(String content, Pattern pattern) {
    Matcher m = pattern.matcher(content);
    return m.find() ? m.group(1) : null;
  }

  private String abbreviate(String s) {
    return s.length() > 200 ? s.substring(0, 200) + "..." : s;
  }
}
