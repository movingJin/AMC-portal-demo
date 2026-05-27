package com.backend.amc_portal.chatbot.agent;

import com.backend.amc_portal.chatbot.client.AzureOpenAiClient;
import com.backend.amc_portal.chatbot.dto.QuestionDecomposition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL 메인 오케스트레이터의 "단계 1: 질문 분해" 책임을 LLM 호출로 수행.
 * (스펙상으로는 메인의 내부 사고이나, Java 메인에서는 LLM 호출로 구현.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionDecomposerAgent {

    private final AzureOpenAiClient azureClient;
    private final ObjectMapper om;

    private static final String SYSTEM_PROMPT = """
            당신은 NL2SQL 시스템의 질문 분해기입니다. 사용자의 한국어 질문에서 다음 두 가지를 추출하세요:

            1. **도메인 키워드** (최대 3개, 가장 변별력 있는 것부터): 테이블/주제를 찾기 위한 명사
               예) "암등록", "내원", "수술", "병리", "PSA/혈액", "처방", "전립선암"

            2. **리터럴 값 토큰**: WHERE 절에 그대로 들어갈 값들 (검사명/약품명/진단명/장기명/점수/상태값)
               예) "PSA", "Gleason 9", "위", "전립선", "남성", "유효"

            중요:
            - **3개 초과 금지** — 4개 이상 떠올라도 가장 구체적인 3개만. 키워드 수가 많을수록 비용/지연 증가.
            - **부분문자열 중복 금지** — "전립선암" 을 넣었으면 "전립선" 은 빼라 (긴 쪽이 더 변별력 있음).
            - **일반 토큰의 우선순위 낮춤** — "환자", "사람", "정보", "데이터", "값", "목록" 같이 거의 모든 의료 테이블에
              매칭되는 토큰은 더 구체적인 도메인 명사가 있을 때만 함께 넣고, 더 구체적인 게 없으면 그 자체로 살려라
              (예: "환자 수 보여줘" → keywords=["환자"]).
            - "PSA" 같은 토큰은 도메인 키워드(혈액검사 영역 탐색용)이자 리터럴 값(WHERE 필터용) 양쪽에 들어갈 수 있음.
              해당하면 양쪽 모두에 넣을 것.
            - 시간 표현(예: "지난 30일")은 어디에도 넣지 말 것. SQL 작성 단계에서 처리.
            - DELETE/UPDATE 등 변경 요청은 빈 배열로 응답.

            JSON 객체로만 응답 (절대 다른 텍스트 금지):
            {"keywords": ["..."], "value_lookups": ["..."]}
            """;

    public QuestionDecomposition decompose(String question) {
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", question)
        );
        JsonNode resp = azureClient.chatCompletion(messages, null, true);
        String content = resp.path("choices").path(0).path("message").path("content").asText("");
        try {
            JsonNode parsed = om.readTree(content);
            List<String> keywords = readStringList(parsed.path("keywords"));
            List<String> values = readStringList(parsed.path("value_lookups"));
            return new QuestionDecomposition(keywords, values);
        } catch (Exception e) {
            log.warn("decompose JSON parse failed; raw='{}'", content, e);
            return new QuestionDecomposition(List.of(question), List.of());
        }
    }

    private List<String> readStringList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr.isArray()) for (JsonNode n : arr) {
            String s = n.asText("");
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }
}
