package com.backend.amc_portal.chatbot.agent;

import com.backend.amc_portal.chatbot.client.AzureOpenAiClient;
import com.backend.amc_portal.chatbot.dto.SqlExecutorResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Sub-agent: 결과를 사용자에게 보여줄 한국어 마크다운으로 합성. 도구 없음. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseComposerAgent {

  private final AzureOpenAiClient azureClient;
  private final ObjectMapper om;

  private static final String SYSTEM_PROMPT =
      """
            당신은 NL2SQL 결과의 **응답 작가** 입니다. 도구는 없습니다.

            입력으로 사용자의 질문, 실행된 SQL, 실행 결과(컬럼/행), 주의사항(caveats) 을 받습니다.
            사용자에게 보여줄 한국어 마크다운을 다음 3블록으로 작성하세요.

            ## 요약
            <2~3문장의 한국어 답변. 숫자 강조. 가정/한계가 있으면 한 문장으로 명시.>

            ## SQL
            ```sql
            <final_sql 그대로>
            ```

            ## 결과 (상위 N건)
            | 컬럼1 | 컬럼2 | ... |
            |------|------|-----|
            | ...  | ...  | ... |

            # 규칙
            - 행이 너무 많으면 상위 10~20 건으로 잘라 표로 보여주고, 전체 개수는 요약에 명시.
            - 컬럼이 한국어 alias 면 그대로 헤더에 사용.
            - 결과 0건: "조건에 맞는 데이터가 없습니다. 가능한 원인: ① 필터 조건 ② 데이터 부재" 안내.
              결과를 절대 지어내지 말 것.
            - s_patno 같은 식별자는 사용자가 명시 요청한 경우에만 그대로, 아니면 집계로.
            - caveats 가 있으면 "## 주의" 블록으로 표 아래에 추가.

            # 금지
            - SQL 을 수정·재작성·재실행 시도 금지.
            - 결과에 없는 숫자/행 인용 금지.
            """;

  public String compose(String question, SqlExecutorResult.Ok result, List<String> caveats) {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("question", question);
    input.put("final_sql", result.executedSql());
    input.put(
        "execution_result",
        Map.of(
            "row_count", result.rowCount(),
            "columns", result.columns(),
            "rows", truncateRows(result.rows(), 20)));
    if (caveats != null && !caveats.isEmpty()) input.put("caveats", caveats);

    String content;
    try {
      content = om.writeValueAsString(input);
    } catch (Exception e) {
      content = question;
    }

    List<Map<String, Object>> messages =
        List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", content));

    JsonNode resp = azureClient.chatCompletion(messages, null);
    return resp.path("choices").path(0).path("message").path("content").asText("");
  }

  private List<List<Object>> truncateRows(List<List<Object>> rows, int limit) {
    return rows.size() <= limit ? rows : rows.subList(0, limit);
  }
}
