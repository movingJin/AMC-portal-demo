package com.backend.amc_portal.chatbot.service;

import com.backend.amc_portal.chatbot.agent.QuestionDecomposerAgent;
import com.backend.amc_portal.chatbot.agent.ResponseComposerAgent;
import com.backend.amc_portal.chatbot.agent.SchemaExplorerAgent;
import com.backend.amc_portal.chatbot.agent.SchemaExplorerAgent.ExploreRequest;
import com.backend.amc_portal.chatbot.agent.SqlAuthorAgent;
import com.backend.amc_portal.chatbot.agent.SqlExecutorAgent;
import com.backend.amc_portal.chatbot.client.Nl2SqlClient;
import com.backend.amc_portal.chatbot.dto.*;
import com.backend.amc_portal.chatbot.dto.ChatResponse.TraceStep;
import com.backend.amc_portal.common.exception.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Main Orchestrator (Java).
 *
 * <p>WORKFLOW-DEEP-AGENT.md 의 메인 오케스트레이터 책임: 1) 안전·범위 체크 2) 질문 분해 (keywords + value_lookups) 3)
 * schema-explorer 병렬 호출 → SchemaBrief 병합 4) sql-author 호출 + citation/anchor 검증 5) sql-executor 호출 +
 * RETRY_AUTHOR 분기 처리 6) response-composer 호출
 *
 * <p>상한 (스펙 9.4): - author 재호출 총 2회 - executor 재실행 총 2회 - schema-explorer 추가 호출 총 2회 (junction /
 * value 보강) - 한 질문당 sub-agent 호출 총 12회 이내
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

  private final QuestionDecomposerAgent decomposer;
  private final SchemaExplorerAgent schemaExplorer;
  private final SqlAuthorAgent sqlAuthor;
  private final SqlExecutorAgent sqlExecutor;
  private final ResponseComposerAgent responseComposer;
  private final Nl2SqlClient nl2SqlClient;

  private static final int MAX_AUTHOR_RETRIES = 2;
  private static final int MAX_EXECUTOR_RETRIES = 2;
  private static final int MAX_EXTRA_SCHEMA_CALLS = 2;
  private static final int MAX_INITIAL_EXPLORERS = 4;
  // per-component 상한 합: decomp(1) + explorer(4) + anchor 보강(2) + author(3) + executor(3) +
  // composer(1) = 14.
  // 도메인 함정 대응 여유 1~2슬롯 포함하여 16으로 설정.
  private static final int MAX_TOTAL_SUBAGENT_CALLS = 16;

  /** response-composer 슬롯은 항상 예약 — 루프는 이 슬롯을 침범하지 않음. */
  private static final int RESPONSE_COMPOSER_RESERVED = 1;

  private static final Pattern UNSAFE_REQUEST =
      Pattern.compile("(?i)\\b(delete|update|insert|drop|truncate|alter|grant|revoke)\\b");

  /** "사용 가능한 테이블 목록", "어떤 테이블이 있어?", "list tables" 등 메타 질문 패턴. */
  private static final Pattern META_LIST_TABLES =
      Pattern.compile(
          "(?i)(테이블\\s*(목록|리스트|들|이\\s*뭐|있나|있어|보여)"
              + "|어떤\\s*테이블"
              + "|사용\\s*가능(한|할\\s*수\\s*있는)?\\s*테이블"
              + "|스키마\\s*(보여|목록)"
              + "|list\\s+tables|show\\s+tables|available\\s+tables|what\\s+tables)");

  public ChatResponse ask(ChatRequest req) {
    String question = req.question();
    List<TraceStep> trace = new ArrayList<>();
    Counter calls = new Counter();

    // Step 0 — 안전 체크
    if (UNSAFE_REQUEST.matcher(question).find()) {
      throw ApiException.badRequest("이 서비스는 읽기 전용입니다. 변경 요청은 처리할 수 없습니다.");
    }

    // Step 0.5 — 메타 질문: 테이블 목록을 직접 list_tables 로 응답
    if (META_LIST_TABLES.matcher(question).find()) {
      return handleListTables(question, trace);
    }

    // Step 1 — 질문 분해
    QuestionDecomposition decomp = decomposer.decompose(question);
    calls.inc();
    trace.add(new TraceStep("question-decomposer", null, question, decomp));
    log.debug("decomp: keywords={}, value_lookups={}", decomp.keywords(), decomp.valueLookups());

    // Step 2 — schema-explorer 병렬 호출
    SchemaBrief brief = exploreInParallel(question, decomp, trace, calls);

    // Step 2.5 — value anchor 확인. 누락된 값 있으면 1회 보강.
    brief =
        ensureValueAnchors(
            question,
            brief,
            decomp.valueLookups(),
            trace,
            calls,
            new int[] {0}); // extraSchemaCalls counter via single-cell array

    // Step 3-4 — sql-author + sql-executor 루프
    int authorRetries = 0;
    int executorRetries = 0;
    int extraSchemaCalls = 0;
    PriorError priorError = null;
    SqlExecutorResult.Ok finalOk = null;
    String failMessage = null;
    List<String> caveats = new ArrayList<>();
    // 같은 value 에 대한 candidateColumns 기반 anchor 합성을 1회로 제한 (무한루프 방지).
    Set<String> synthesizedAnchorValues = new java.util.HashSet<>();

    loop:
    while (true) {
      // response-composer 슬롯은 예약 — 루프 내부 호출 시 reserved 만큼은 비워둠.
      if (calls.value >= MAX_TOTAL_SUBAGENT_CALLS - RESPONSE_COMPOSER_RESERVED) {
        failMessage = "sub-agent 호출 상한(" + MAX_TOTAL_SUBAGENT_CALLS + ") 초과";
        break;
      }

      // sql-author 호출
      SqlAuthorResult authorResult = sqlAuthor.write(question, brief, priorError);
      calls.inc();
      trace.add(
          new TraceStep(
              "sql-author",
              null,
              summarizeAuthorInput(question, priorError),
              summarizeAuthor(authorResult)));

      switch (authorResult) {
        case SqlAuthorResult.ValueUnconfirmed vu -> {
          // Fallback A: author 가 candidateColumns 를 명시했고 아직 합성 안 한 value 라면,
          // schema-explorer LLM 의 anchor 누락을 우회하여 후보들로 anchor 를 합성 → 재시도.
          // (위험: 후보가 틀리면 executor 가 ZERO_ROWS 로 RETRY_AUTHOR — 그때 ruleOutAnchorColumn 으로 다음 후보로
          // 넘김.)
          if (!vu.candidateColumns().isEmpty()
              && !synthesizedAnchorValues.contains(vu.value())
              && brief.findAnchor(vu.value()).isEmpty()) {
            SchemaBrief synthesized =
                synthesizeAnchorFromCandidates(vu.value(), vu.candidateColumns());
            brief = brief.mergeWith(synthesized);
            synthesizedAnchorValues.add(vu.value());
            trace.add(
                new TraceStep(
                    "anchor-synthesizer",
                    null,
                    "value=" + vu.value() + " candidates=" + vu.candidateColumns(),
                    "anchors_added=" + synthesized.valueAnchors().size()));
            // schema-explorer 슬롯 소비 없이 author 재호출
            continue;
          }

          // value_anchors 보강 후 author 재호출
          if (extraSchemaCalls >= MAX_EXTRA_SCHEMA_CALLS) {
            failMessage = "값 '" + vu.value() + "' 의 저장 컬럼을 식별하지 못함";
            break loop;
          }
          SchemaBrief extra =
              schemaExplorer.explore(
                  ExploreRequest.forKeyword(question, vu.value(), List.of(vu.value())));
          calls.inc();
          extraSchemaCalls++;
          trace.add(
              new TraceStep(
                  "schema-explorer",
                  null,
                  "value-reconfirm: " + vu.value(),
                  summarizeBrief(extra)));
          brief = brief.mergeWith(extra);
          // priorError 유지하지 않음 — 새 anchor 로 재시도
          continue;
        }
        case SqlAuthorResult.SchemaInsufficient si -> {
          failMessage = "sql-author: " + si.reason();
          break loop;
        }
        case SqlAuthorResult.Authored authored -> {
          // 메인의 검증: citation 컬럼이 brief 에 있는가
          List<String> missingCitations = findMissingCitations(authored.schemaCitation(), brief);
          if (!missingCitations.isEmpty()) {
            if (authorRetries >= MAX_AUTHOR_RETRIES) {
              failMessage = "SchemaCitation 검증 실패: " + missingCitations;
              break loop;
            }
            authorRetries++;
            priorError =
                PriorError.of(
                    "CITATION_INVALID",
                    "다음 컬럼이 schema_brief 에 없음: " + missingCitations,
                    authored.sql());
            continue;
          }

          // 메인의 검증: SQL 의 모든 리터럴 값 필터가 anchor 에 매칭되는가
          String unanchoredValue = findUnanchoredLiteralValue(authored.sql(), brief);
          if (unanchoredValue != null) {
            if (extraSchemaCalls >= MAX_EXTRA_SCHEMA_CALLS) {
              // 더 보강 못 함 — 그래도 시도해보자 (executor 가 0건이면 fail 로)
              log.warn("Unanchored value '{}' but no more schema-explorer slots", unanchoredValue);
            } else {
              SchemaBrief extra =
                  schemaExplorer.explore(
                      ExploreRequest.forKeyword(
                          question, unanchoredValue, List.of(unanchoredValue)));
              calls.inc();
              extraSchemaCalls++;
              trace.add(
                  new TraceStep(
                      "schema-explorer",
                      null,
                      "anchor-validate: " + unanchoredValue,
                      summarizeBrief(extra)));
              brief = brief.mergeWith(extra);
              continue;
            }
          }

          // sql-executor 호출
          SqlExecutorResult execResult = sqlExecutor.execute(authored.sql());
          calls.inc();
          trace.add(
              new TraceStep(
                  "sql-executor", "execute_sql", authored.sql(), summarizeExec(execResult)));

          switch (execResult) {
            case SqlExecutorResult.Ok ok -> {
              finalOk = ok;
              break loop;
            }
            case SqlExecutorResult.Fail fail -> {
              failMessage = "execute_sql 실패: " + fail.message();
              break loop;
            }
            case SqlExecutorResult.RetryAuthor retry -> {
              if (executorRetries >= MAX_EXECUTOR_RETRIES) {
                failMessage = "executor 재시도 한도 초과: " + retry.priorError().message();
                break loop;
              }
              executorRetries++;
              priorError = retry.priorError();
              // ZERO_ROWS_WITH_VALUE_FILTER 처리
              if ("ZERO_ROWS_WITH_VALUE_FILTER".equals(priorError.code())
                  && priorError.suspectFilter() != null) {
                String susValue = priorError.suspectFilter().value();
                String susColumn = priorError.suspectFilter().column();
                // 합성 anchor 의 location 중 실패한 컬럼을 ruled_out 으로 이동.
                // → 다음 author 호출에서 남은 후보 location 으로 시도하게 됨.
                if (susColumn != null && synthesizedAnchorValues.contains(susValue)) {
                  brief = ruleOutAnchorColumn(brief, susValue, susColumn, "ZERO_ROWS 로 실패한 컬럼");
                  trace.add(
                      new TraceStep(
                          "anchor-synthesizer",
                          null,
                          "rule-out " + susValue + "→" + susColumn,
                          "remaining_locations="
                              + brief
                                  .findAnchor(susValue)
                                  .map(a -> a.locations().size())
                                  .orElse(0)));
                }
                // 그 다음 schema-explorer 추가 (slot 있으면)
                if (extraSchemaCalls < MAX_EXTRA_SCHEMA_CALLS) {
                  SchemaBrief extra =
                      schemaExplorer.explore(
                          ExploreRequest.forKeyword(question, susValue, List.of(susValue)));
                  calls.inc();
                  extraSchemaCalls++;
                  trace.add(
                      new TraceStep(
                          "schema-explorer",
                          null,
                          "zero-rows-reexplore: " + susValue,
                          summarizeBrief(extra)));
                  brief = brief.mergeWith(extra);
                }
              }
              // 다시 author 호출
            }
          }
        }
      }
    }

    if (finalOk == null) {
      // 실패 경로 — trace 를 한 줄에 dump 해서 사후 진단 가능하도록.
      log.warn("[chatbot] FAIL: {} | trace_steps={}", failMessage, summarizeTrace(trace));
      throw ApiException.badRequest(failMessage != null ? failMessage : "쿼리 처리 실패");
    }

    // Step 5 — response-composer
    String answer = responseComposer.compose(question, finalOk, caveats);
    calls.inc();
    trace.add(
        new TraceStep(
            "response-composer", null, "rows=" + finalOk.rowCount(), abbreviate(answer, 200)));

    return new ChatResponse(
        answer, finalOk.executedSql(), finalOk.columns(), finalOk.rows(), trace);
  }

  /** 메타 질문(테이블 목록 조회) 전용 fast path — sub-agent 루프를 건너뛰고 list_tables 결과를 그대로 반환. */
  @SuppressWarnings("unchecked")
  private ChatResponse handleListTables(String question, List<TraceStep> trace) {
    Map<String, Object> resp = nl2SqlClient.listTables();
    trace.add(
        new TraceStep("meta-list-tables", "list_tables", question, "tables=" + sizeOfTables(resp)));

    Object tablesObj = resp.get("tables");
    if (!(tablesObj instanceof List<?> tableList)) {
      String err = resp.get("error") instanceof String s ? s : "테이블 목록 조회 실패";
      throw ApiException.badRequest(err);
    }

    List<String> columns = List.of("schema", "name", "description", "row_estimate");
    List<List<Object>> rows = new ArrayList<>();
    for (Object t : tableList) {
      if (t instanceof Map<?, ?> m) {
        Object schema = m.get("schema");
        Object name = m.get("name");
        Object desc = m.get("description");
        Object rowEst = m.get("row_estimate");
        rows.add(
            List.of(
                schema == null ? "" : String.valueOf(schema),
                name == null ? "" : String.valueOf(name),
                desc == null ? "" : String.valueOf(desc),
                rowEst == null ? "" : String.valueOf(rowEst)));
      }
    }
    String answer = "사용 가능한 테이블 " + rows.size() + "개입니다. 우측 그리드를 참고하세요.";
    return new ChatResponse(answer, null, columns, rows, trace);
  }

  private static int sizeOfTables(Map<String, Object> resp) {
    return resp.get("tables") instanceof List<?> l ? l.size() : 0;
  }

  /**
   * 모든 테이블에 광범위하게 매칭돼서 noise 가 되는 키워드. schema-explorer 호출을 낭비하므로 드롭. 예) "환자" 는 거의 모든 의료 테이블에 매칭 →
   * 키워드로는 부적합 (value_lookups 에 들어가는 것은 OK).
   */
  private static final Set<String> NOISE_KEYWORDS =
      Set.of(
          "환자",
          "환자들",
          "사람",
          "사람들",
          "정보",
          "데이터",
          "값",
          "목록",
          "건수",
          "수치",
          "patient",
          "patients",
          "people",
          "person",
          "data",
          "value",
          "count");

  /** 단계 2: schema-explorer 들을 같은 turn 에 병렬 호출. */
  private SchemaBrief exploreInParallel(
      String question, QuestionDecomposition decomp, List<TraceStep> trace, Counter calls) {
    // 1) 키워드 정제:
    //    (a) 빈 토큰 제거
    //    (b) 부분문자열 겹침 제거 ("전립선암" 있으면 "전립선" 드롭)
    //    (c) noise 토큰은 *다른 specific 키워드가 함께 있을 때만* 드롭.
    //        단독 (예: "환자 수 보여줘") 으로 있을 땐 살려야 fallback 정확도가 유지됨.
    List<String> nonBlank =
        decomp.keywords().stream().filter(k -> k != null && !k.isBlank()).toList();
    List<String> deduped = dedupeBySubsumption(nonBlank);
    List<String> specific =
        deduped.stream().filter(k -> !NOISE_KEYWORDS.contains(k.toLowerCase())).toList();
    List<String> kwClean = specific.isEmpty() ? deduped : specific;

    Set<String> seen = new LinkedHashSet<>();
    List<ExploreRequest> requests = new ArrayList<>();

    for (String kw : kwClean) {
      if (seen.add(kw)) {
        // 해당 키워드와 관련된 value_lookups 만 동봉 (단순화: 키워드와 같은 토큰만)
        List<String> related =
            decomp.valueLookups().stream()
                .filter(v -> v.equals(kw) || kw.contains(v) || v.contains(kw))
                .toList();
        requests.add(ExploreRequest.forKeyword(question, kw, related));
      }
      if (requests.size() >= MAX_INITIAL_EXPLORERS) break;
    }
    // value_lookups 중 키워드와 매칭 안 된 값은 별도 explorer (anchor 강제) — noise 토큰도 anchor 용으로는 살림
    for (String v : decomp.valueLookups()) {
      if (v == null || v.isBlank()) continue;
      // 이미 더 긴 키워드에 포함된 value 는 굳이 또 안 돌림
      boolean coveredByKeyword = kwClean.stream().anyMatch(k -> !k.equals(v) && k.contains(v));
      if (coveredByKeyword) continue;
      if (seen.add(v) && requests.size() < MAX_INITIAL_EXPLORERS) {
        requests.add(ExploreRequest.forKeyword(question, v, List.of(v)));
      }
    }
    if (requests.isEmpty()) {
      // 키워드 추출 실패 — 질문 그대로 한 번 탐색
      requests.add(ExploreRequest.forKeyword(question, question, decomp.valueLookups()));
    }

    // 병렬 실행
    List<CompletableFuture<SchemaBrief>> futures = new ArrayList<>();
    for (ExploreRequest r : requests) {
      futures.add(schemaExplorer.exploreAsync(r));
    }
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    SchemaBrief merged = SchemaBrief.empty();
    for (int i = 0; i < futures.size(); i++) {
      SchemaBrief b = futures.get(i).getNow(SchemaBrief.empty());
      calls.inc();
      trace.add(new TraceStep("schema-explorer", null, requests.get(i), summarizeBrief(b)));
      merged = merged.mergeWith(b);
    }
    return merged;
  }

  /**
   * brief 의 anchor 에서 특정 (table?,column) 를 location 에서 제거하고 ruled_out 으로 옮김. suspectColumn 은
   * "table.column" 또는 "column" 형태일 수 있어 둘 다 매칭.
   */
  private static SchemaBrief ruleOutAnchorColumn(
      SchemaBrief brief, String value, String suspectColumn, String reason) {
    Optional<SchemaBrief.ValueAnchor> opt =
        brief.valueAnchors().stream().filter(a -> a.value().equals(value)).findFirst();
    if (opt.isEmpty()) return brief;
    SchemaBrief.ValueAnchor a = opt.get();
    String susCol =
        suspectColumn.contains(".")
            ? suspectColumn.substring(suspectColumn.lastIndexOf('.') + 1)
            : suspectColumn;
    List<SchemaBrief.Location> remaining = new ArrayList<>();
    List<SchemaBrief.RuledOut> ruled = new ArrayList<>(a.ruledOut());
    for (SchemaBrief.Location loc : a.locations()) {
      if (loc.column().equalsIgnoreCase(susCol)) {
        ruled.add(new SchemaBrief.RuledOut(loc.table(), loc.column(), reason));
      } else {
        remaining.add(loc);
      }
    }
    boolean found = !remaining.isEmpty();
    SchemaBrief.ValueAnchor updated =
        new SchemaBrief.ValueAnchor(
            a.value(),
            found,
            remaining,
            ruled,
            a.note() == null ? reason : a.note() + " | " + reason);
    // brief 를 새로 만들되 다른 anchor 는 유지
    List<SchemaBrief.ValueAnchor> newAnchors = new ArrayList<>();
    for (SchemaBrief.ValueAnchor x : brief.valueAnchors()) {
      newAnchors.add(x.value().equals(value) ? updated : x);
    }
    return new SchemaBrief(brief.tables(), brief.joinCandidates(), newAnchors, brief.notes());
  }

  /**
   * sql-author 가 ValueUnconfirmed 로 명시한 candidateColumns 들을 location 으로 가지는 ValueAnchor 를 합성.
   * schema-explorer LLM 이 anchor 채움을 누락하는 경우의 우회로. 입력 컬럼 포맷: "table.column" 또는
   * "schema.table.column".
   */
  private static SchemaBrief synthesizeAnchorFromCandidates(
      String value, List<String> candidateColumns) {
    List<SchemaBrief.Location> locations = new ArrayList<>();
    for (String c : candidateColumns) {
      String[] parts = c.split("\\.");
      String table =
          parts.length >= 2
              ? String.join(".", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1))
              : c;
      String column = parts[parts.length - 1];
      locations.add(new SchemaBrief.Location(table, column, "(author-candidate)"));
    }
    SchemaBrief.ValueAnchor anchor =
        new SchemaBrief.ValueAnchor(
            value, true, locations, List.of(), "author candidateColumns 기반 합성 — sample 미검증");
    return new SchemaBrief(List.of(), List.of(), List.of(anchor), "");
  }

  /**
   * 부분문자열로 다른 토큰에 포함되는 항목 제거. 예) ["전립선암","전립선","수술","PSA"] → ["전립선암","수술","PSA"]. 입력 순서를 최대한 보존하되 더
   * 긴 토큰이 우선.
   */
  private static List<String> dedupeBySubsumption(List<String> tokens) {
    List<String> sortedDesc = new ArrayList<>(tokens);
    sortedDesc.sort((a, b) -> Integer.compare(b.length(), a.length()));
    List<String> kept = new ArrayList<>();
    for (String t : sortedDesc) {
      boolean subsumed = kept.stream().anyMatch(k -> !k.equals(t) && k.contains(t));
      if (!subsumed) kept.add(t);
    }
    // 입력 순서 유지하여 반환
    List<String> result = new ArrayList<>();
    for (String orig : tokens) if (kept.contains(orig) && !result.contains(orig)) result.add(orig);
    return result;
  }

  /** 모든 value_lookups 가 brief 에 anchor 됐는지 확인. 누락분은 1회 보강. */
  private SchemaBrief ensureValueAnchors(
      String question,
      SchemaBrief brief,
      List<String> valueLookups,
      List<TraceStep> trace,
      Counter calls,
      int[] extraCounter) {
    if (valueLookups.isEmpty()) return brief;
    SchemaBrief current = brief;
    for (String v : valueLookups) {
      if (current.findAnchor(v).isPresent()) continue;
      if (extraCounter[0] >= MAX_EXTRA_SCHEMA_CALLS) break;
      if (calls.value >= MAX_TOTAL_SUBAGENT_CALLS - RESPONSE_COMPOSER_RESERVED) break;
      SchemaBrief extra =
          schemaExplorer.explore(ExploreRequest.forKeyword(question, v, List.of(v)));
      calls.inc();
      extraCounter[0]++;
      trace.add(new TraceStep("schema-explorer", null, "anchor-fill: " + v, summarizeBrief(extra)));
      current = current.mergeWith(extra);
    }
    return current;
  }

  /** SchemaCitation 에서 'schema.table.column' 패턴 추출 후 brief 에 존재하는지 검사. */
  private List<String> findMissingCitations(String citation, SchemaBrief brief) {
    if (citation == null) return List.of();
    List<String> missing = new ArrayList<>();
    Matcher m = Pattern.compile("\\b(\\w+\\.\\w+\\.\\w+)\\b").matcher(citation);
    Set<String> checked = new LinkedHashSet<>();
    while (m.find()) {
      String ref = m.group(1);
      if (!checked.add(ref)) continue;
      if (!brief.hasColumn(ref)) missing.add(ref);
    }
    return missing;
  }

  /** SQL 의 `col = '값'` / `col ILIKE '%값%'` 중 value_anchors 에 없는 첫 번째 값 반환. */
  private String findUnanchoredLiteralValue(String sql, SchemaBrief brief) {
    Matcher m = Pattern.compile("(?i)\\b(\\w+)\\s*(?:=|ILIKE)\\s*'%?([^%']+?)%?'").matcher(sql);
    while (m.find()) {
      String val = m.group(2);
      if (val.matches("[YN]") || val.matches("\\d+")) continue; // *_yn / 숫자는 anchor 불필요
      if (brief.findAnchor(val).isEmpty()) return val;
    }
    return null;
  }

  private Object summarizeBrief(SchemaBrief b) {
    return "tables="
        + b.tables().size()
        + ", joins="
        + b.joinCandidates().size()
        + ", anchors="
        + b.valueAnchors().size();
  }

  private Object summarizeAuthor(SqlAuthorResult r) {
    return switch (r) {
      case SqlAuthorResult.Authored a ->
          "Authored(sql_len=" + (a.sql() == null ? 0 : a.sql().length()) + ")";
      case SqlAuthorResult.ValueUnconfirmed v -> "ValueUnconfirmed(" + v.value() + ")";
      case SqlAuthorResult.SchemaInsufficient s -> "SchemaInsufficient";
    };
  }

  private Object summarizeAuthorInput(String question, PriorError priorError) {
    return priorError == null
        ? "question only"
        : "question + prior_error(" + priorError.code() + ")";
  }

  private Object summarizeExec(SqlExecutorResult r) {
    return switch (r) {
      case SqlExecutorResult.Ok ok -> "OK rows=" + ok.rowCount();
      case SqlExecutorResult.RetryAuthor ra -> "RETRY " + ra.priorError().code();
      case SqlExecutorResult.Fail f -> "FAIL " + f.code();
    };
  }

  private String abbreviate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "...";
  }

  /** 실패 시 사후 진단용 trace 요약 (한 줄). */
  private String summarizeTrace(List<TraceStep> trace) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < trace.size(); i++) {
      TraceStep s = trace.get(i);
      if (i > 0) sb.append(" → ");
      sb.append(s.agent());
      if (s.output() != null)
        sb.append('[').append(abbreviate(String.valueOf(s.output()), 80)).append(']');
    }
    return sb.toString();
  }

  private static final class Counter {
    int value = 0;

    void inc() {
      value++;
    }
  }
}
