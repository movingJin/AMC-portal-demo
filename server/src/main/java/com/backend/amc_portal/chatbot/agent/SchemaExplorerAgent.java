package com.backend.amc_portal.chatbot.agent;

import com.backend.amc_portal.chatbot.client.AzureOpenAiClient;
import com.backend.amc_portal.chatbot.client.Nl2SqlClient;
import com.backend.amc_portal.chatbot.dto.SchemaBrief;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Sub-agent: 한 키워드(또는 후보 테이블) 에 대한 스키마 탐색 + value_anchors 확인. 도구: search_tables, get_table_schema,
 * get_sample_rows (execute_sql 금지) 출력: SchemaBrief JSON
 *
 * <p>호출별로 독립된 LLM 컨텍스트 — 메인 컨텍스트에는 SchemaBrief 만 반환됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaExplorerAgent {

  private final AzureOpenAiClient azureClient;
  private final Nl2SqlClient nl2SqlClient;
  private final ObjectMapper om;
  private final @Qualifier("subAgentExecutor") Executor executor;

  private static final int MAX_TOOL_ITERATIONS = 8;

  private static final String SYSTEM_PROMPT =
      """
            당신은 amc_portal PostgreSQL 의 **스키마 탐색 전문가** 입니다. 도구는 search_tables /
            get_table_schema / get_sample_rows 세 가지뿐이며, SQL 실행 도구는 없습니다.
            입력으로 받은 도메인 키워드(또는 후보 테이블) 1개에 대해 탐색을 수행하고,
            최종 응답은 반드시 SchemaBrief JSON 한 덩어리로만 반환합니다.

            # 절차
            1) keyword 가 있으면 search_tables(keyword=keyword) 호출. match_reason 이 table_* 인 항목을
               후보 테이블로 선정(최대 3개). 후보가 없으면 list_tables 의존하지 말고 그대로 진행.
            2) value_lookups 의 각 값에 대해 별도로 search_tables(keyword=값) 호출.
               match_reason 이 column_* / column_value_* 면 그 컬럼이 값을 보유하는 후보.
            3) 각 후보 테이블에 대해 get_table_schema(schema, table) 호출. foreign_keys 가 가리키는
               부모 테이블도 후보로 추가(최대 2단계).
            4) get_sample_rows(table, limit=10) — 다음 중 하나라도 해당하면 **반드시** 호출:
               - value_lookups 가 비어있지 않다 → 무조건 호출 (값이 어느 컬럼에 들어있는지 확인)
               - 사용자 질문이 코드값 의미("유효한","남성","위암"…)를 요구
               - description 만으로 _cd/_yn 컬럼 값 분포 추정 불가
            5) **값 위치 확정 (value_anchors)** — value_lookups 의 각 값에 대해:
               - sample 응답에서 그 값이 등장하는 (table, column) 쌍을 모두 기록
               - 여러 컬럼이 후보일 때 추측으로 하나만 선택 금지 — 전부 locations 에 실어 반환
               - 어느 sample 에도 없으면 found=false 로 명시 (절대 "있는 듯하다" 라고 쓰지 말 것)
            6) description 은 60자 이내로 잘라 SchemaBrief 에 포함. 단, "→ FK X.Y" / "(논리 매핑: X.Y)" /
               "[분석 마트]" 태그는 절대 자르지 말 것.

            # 도메인 함정 (notes 에 1~3줄로 요약)
            - 날짜 컬럼은 대부분 TEXT 'YYYYMMDD'. TO_DATE 캐스팅 필요 (notes 에 명시).
            - *_yn 은 TEXT 'Y'/'N'. 컬럼명이 vald_yn / vald_op_yn / use_yn 등 테이블마다 다름 —
              실제 이름 그대로 columns 에. notes 에 "vald_yn 으로 가정 금지" 경고.
            - 분석 마트(poc_prostate_*) 와 원천에 같은 의미 컬럼이 있으면 둘 다 columns 에 표기, notes 에 "마트 우선".

            # 도메인 함정 — 값 위치 확인 (Hallucination 방지)
            - 컬럼명이 의미를 보장하지 않는다. `exam_cd` 라고 해서 "PSA" 값이 들어있을 보장 없음.
              PSA 같은 항목명은 보통 `erp_dtl_cd` 또는 `dtl_cd_nm` 같은 상세 컬럼.
            - 자주 헷갈리는 코드/명칭 컬럼 쌍 (반드시 sample 로 확인):
              · 혈액/검사: exam_cd vs erp_dtl_cd vs dtl_cd_nm vs exam_nm
              · 처방   : ordr_cd vs ordr_kor_nm vs ordr_eng_nm
              · 수술   : inhosp_op_cd vs inhosp_op_eng_nm vs shrtg_op_nm
              · 진단   : std_diag_cd vs diag_nm

            # 금지
            - SQL 작성 금지. SQL 실행 도구 호출 시도 금지(권한 없음).
            - 도구로 확인되지 않은 테이블/컬럼은 SchemaBrief 에 포함 금지.
            - 한 호출에서 너무 넓게 탐색하지 말 것 (후보 테이블 최대 3 + FK 2단계).

            # 출력 (반드시 JSON 객체 하나로만 응답 — markdown 코드블록 금지)
            {
              "tables": [
                {
                  "name": "public.poc_xxx",
                  "kind": "MART" | "원천" | "코드마스터",
                  "description": "...",
                  "pk": ["col1"],
                  "columns": [
                    {"name":"col","type":"TEXT","nullable":false,"desc":"...","tag":"FK→other.col" | null}
                  ],
                  "foreign_keys": [{"from":"col","to":"other.col"}],
                  "sample_hints": [{"col":"prmr_organ_cd","values":["STM","LNG","BRT"]}]
                }
              ],
              "join_candidates": [{"from":"a.col","to":"b.col","via":"FK" | "논리매핑" | "허브키(s_patno)"}],
              "value_anchors": [
                {
                  "value":"PSA",
                  "found":true,
                  "locations":[{"table":"public.poc_xxx","column":"dtl_cd_nm","sample":"PSA (정량)"}],
                  "ruled_out":[{"table":"public.poc_xxx","column":"exam_cd","reason":"sample 10건 중 'PSA' 없음"}],
                  "note":"여러 컬럼 발견 — sql-author 가 결정"
                }
              ],
              "notes": "도메인 함정 1~3줄"
            }
            """;

  /** 비동기 호출 — 메인이 여러 인스턴스를 병렬로 호출. */
  public CompletableFuture<SchemaBrief> exploreAsync(ExploreRequest req) {
    return CompletableFuture.supplyAsync(() -> explore(req), executor);
  }

  public SchemaBrief explore(ExploreRequest req) {
    try {
      return runToolLoop(req);
    } catch (Exception e) {
      log.warn("SchemaExplorer failed for {}: {}", req, e.getMessage());
      return SchemaBrief.empty();
    }
  }

  private SchemaBrief runToolLoop(ExploreRequest req) throws Exception {
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

    Map<String, Object> input = new LinkedHashMap<>();
    input.put("query_hint", req.queryHint());
    if (req.keyword() != null) input.put("keyword", req.keyword());
    if (req.table() != null) input.put("table", req.table());
    input.put("value_lookups", req.valueLookups());
    messages.add(Map.of("role", "user", "content", om.writeValueAsString(input)));

    List<Map<String, Object>> tools = explorerTools();
    // Layer B: get_sample_rows 응답을 캡쳐 — LLM 이 anchor 누락 시 Java 가 직접 값↔컬럼 매칭에 사용.
    // table 이름(qualified) → SampleRowsResponse 원본.
    Map<String, Map<String, Object>> capturedSamples = new LinkedHashMap<>();

    for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
      // 마지막 iteration 에서는 JSON 모드 강제 (도구 호출 그만하고 결과만)
      boolean lastIter = (i == MAX_TOOL_ITERATIONS - 1);
      JsonNode resp = azureClient.chatCompletion(messages, lastIter ? null : tools, lastIter);
      JsonNode message = resp.path("choices").path(0).path("message");
      JsonNode toolCalls = message.path("tool_calls");

      if (toolCalls.isArray() && !toolCalls.isEmpty() && !lastIter) {
        appendAssistantWithToolCalls(messages, message, toolCalls);
        for (JsonNode tc : toolCalls) {
          String name = tc.path("function").path("name").asText();
          String argsJson = tc.path("function").path("arguments").asText("{}");
          Map<String, Object> args = parseMap(argsJson);
          Object result = invokeTool(name, args);
          // sample_rows 결과는 별도 캡쳐 (Layer B 후처리용)
          if ("get_sample_rows".equals(name) && result instanceof Map<?, ?> r) {
            String tableKey = qualifiedTableKey(args);
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) r;
            if (casted.containsKey("rows") && casted.containsKey("columns")) {
              capturedSamples.put(tableKey, casted);
            }
          }
          messages.add(
              Map.of(
                  "role",
                  "tool",
                  "tool_call_id",
                  tc.path("id").asText(),
                  "name",
                  name,
                  "content",
                  om.writeValueAsString(result)));
        }
        continue;
      }

      // 최종 응답
      String content = message.path("content").asText("");
      SchemaBrief brief = parseBrief(content);
      // Layer B: LLM 이 못 채운 anchor 를 sample 직매칭으로 보강
      return attachDeterministicAnchors(brief, capturedSamples, req.valueLookups());
    }
    return SchemaBrief.empty();
  }

  /** LLM tool call 의 args ({schema, table}) → "schema.table" qualified key. */
  private static String qualifiedTableKey(Map<String, Object> args) {
    Object t = args.get("table");
    if (t == null) return "";
    String table = String.valueOf(t);
    if (table.contains(".")) return table;
    Object s = args.get("schema");
    String schema = s == null || String.valueOf(s).isBlank() ? "public" : String.valueOf(s);
    return schema + "." + table;
  }

  /**
   * Layer B: LLM 이 value_anchors 를 빠뜨린 경우, 캡쳐된 sample_rows 응답에서 결정론적(case-insensitive substring)
   * 매칭으로 anchor 를 보강. 의미적 매칭(약어, 다국어, 코드↔명칭)은 LLM 영역이라 여기선 글자 매칭만 수행 — fast path.
   */
  private SchemaBrief attachDeterministicAnchors(
      SchemaBrief brief,
      Map<String, Map<String, Object>> capturedSamples,
      List<String> valueLookups) {
    if (valueLookups == null || valueLookups.isEmpty()) return brief;
    if (capturedSamples.isEmpty()) return brief;

    List<SchemaBrief.ValueAnchor> newAnchors = new ArrayList<>(brief.valueAnchors());
    for (String value : valueLookups) {
      if (value == null || value.isBlank()) continue;
      // LLM 이 이미 found=true 로 채웠으면 skip — 의미적 매칭이 더 정확.
      boolean alreadyFound =
          newAnchors.stream().anyMatch(a -> a.value().equals(value) && a.found());
      if (alreadyFound) continue;

      String needle = value.trim().toLowerCase();
      List<SchemaBrief.Location> hits = new ArrayList<>();
      for (Map.Entry<String, Map<String, Object>> e : capturedSamples.entrySet()) {
        String tableKey = e.getKey();
        Map<String, Object> sample = e.getValue();
        Object colsObj = sample.get("columns");
        Object rowsObj = sample.get("rows");
        if (!(colsObj instanceof List<?> cols) || !(rowsObj instanceof List<?> rows)) continue;
        // 열 인덱스별로 매칭된 sample 값 1개만 기록 (중복 location 방지)
        Map<Integer, String> matchedByCol = new LinkedHashMap<>();
        for (Object rowObj : rows) {
          if (!(rowObj instanceof List<?> row)) continue;
          for (int ci = 0; ci < row.size() && ci < cols.size(); ci++) {
            if (matchedByCol.containsKey(ci)) continue;
            Object cell = row.get(ci);
            if (cell == null) continue;
            String text = String.valueOf(cell).toLowerCase();
            if (text.contains(needle)) {
              matchedByCol.put(ci, String.valueOf(cell));
            }
          }
        }
        for (Map.Entry<Integer, String> m : matchedByCol.entrySet()) {
          String colName = String.valueOf(cols.get(m.getKey()));
          hits.add(new SchemaBrief.Location(tableKey, colName, m.getValue()));
        }
      }
      if (hits.isEmpty()) continue;

      // 기존 anchor entry (found=false 였을 수도) 가 있으면 머지, 없으면 신규.
      int existingIdx = -1;
      for (int i = 0; i < newAnchors.size(); i++) {
        if (newAnchors.get(i).value().equals(value)) {
          existingIdx = i;
          break;
        }
      }
      SchemaBrief.ValueAnchor merged;
      if (existingIdx >= 0) {
        SchemaBrief.ValueAnchor old = newAnchors.get(existingIdx);
        List<SchemaBrief.Location> locs = new ArrayList<>(old.locations());
        for (SchemaBrief.Location h : hits) if (!locs.contains(h)) locs.add(h);
        merged =
            new SchemaBrief.ValueAnchor(
                value,
                true,
                locs,
                old.ruledOut(),
                (old.note() == null ? "" : old.note() + " | ") + "sample 직매칭으로 보강");
        newAnchors.set(existingIdx, merged);
      } else {
        merged =
            new SchemaBrief.ValueAnchor(
                value, true, hits, List.of(), "sample 직매칭으로 보강 (LLM anchor 누락)");
        newAnchors.add(merged);
      }
      log.debug("[deterministic-anchor] value='{}', locations={}", value, hits);
    }
    return new SchemaBrief(brief.tables(), brief.joinCandidates(), newAnchors, brief.notes());
  }

  private SchemaBrief parseBrief(String content) {
    try {
      // markdown 코드블록 제거
      String trimmed = content.trim();
      if (trimmed.startsWith("```")) {
        int firstNl = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNl > 0 && lastFence > firstNl) {
          trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
        }
      }
      return om.readValue(trimmed, SchemaBrief.class);
    } catch (Exception e) {
      log.warn("SchemaBrief parse failed; raw='{}'", content, e);
      return SchemaBrief.empty();
    }
  }

  private Object invokeTool(String name, Map<String, Object> args) {
    return switch (name) {
      case "search_tables" ->
          nl2SqlClient.searchTables(
              (String) args.get("keyword"),
              args.get("top_k") instanceof Number n ? n.intValue() : null);
      case "get_table_schema" ->
          nl2SqlClient.getTableSchema(
              (String) args.getOrDefault("schema", "public"), (String) args.get("table"));
      case "get_sample_rows" ->
          nl2SqlClient.getSampleRows(
              (String) args.getOrDefault("schema", "public"),
              (String) args.get("table"),
              args.get("limit") instanceof Number n ? Math.min(n.intValue(), 20) : 10);
      default -> Map.of("error", "tool '" + name + "' not allowed for schema-explorer");
    };
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseMap(String json) {
    try {
      return om.readValue(json, Map.class);
    } catch (Exception e) {
      return Map.of();
    }
  }

  private void appendAssistantWithToolCalls(
      List<Map<String, Object>> messages, JsonNode message, JsonNode toolCalls) {
    Map<String, Object> assistantMsg = new LinkedHashMap<>();
    assistantMsg.put("role", "assistant");
    assistantMsg.put("content", message.path("content").asText(null));
    List<Map<String, Object>> tcList = new ArrayList<>();
    for (JsonNode tc : toolCalls) {
      tcList.add(
          Map.of(
              "id", tc.path("id").asText(),
              "type", "function",
              "function",
                  Map.of(
                      "name", tc.path("function").path("name").asText(),
                      "arguments", tc.path("function").path("arguments").asText())));
    }
    assistantMsg.put("tool_calls", tcList);
    messages.add(assistantMsg);
  }

  private List<Map<String, Object>> explorerTools() {
    return List.of(
        fn(
            "search_tables",
            "키워드(한국어 가능)로 관련 테이블/컬럼을 검색.",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "keyword", Map.of("type", "string"),
                        "top_k", Map.of("type", "integer")),
                "required", List.of("keyword"))),
        fn(
            "get_table_schema",
            "단일 테이블의 컬럼/제약/인덱스 정보.",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "schema", Map.of("type", "string"),
                        "table", Map.of("type", "string")),
                "required", List.of("table"))),
        fn(
            "get_sample_rows",
            "테이블 샘플 행 최대 20개.",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "schema", Map.of("type", "string"),
                        "table", Map.of("type", "string"),
                        "limit", Map.of("type", "integer")),
                "required", List.of("table"))));
  }

  private Map<String, Object> fn(String name, String desc, Map<String, Object> params) {
    return Map.of(
        "type",
        "function",
        "function",
        Map.of("name", name, "description", desc, "parameters", params));
  }

  public record ExploreRequest(
      String queryHint,
      String keyword, // 둘 중 하나만
      String table,
      List<String> valueLookups) {
    public static ExploreRequest forKeyword(String hint, String keyword, List<String> values) {
      return new ExploreRequest(hint, keyword, null, values == null ? List.of() : values);
    }

    public static ExploreRequest forTable(String hint, String table, List<String> values) {
      return new ExploreRequest(hint, null, table, values == null ? List.of() : values);
    }
  }
}
