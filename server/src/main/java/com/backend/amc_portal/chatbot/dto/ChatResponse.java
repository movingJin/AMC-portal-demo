package com.backend.amc_portal.chatbot.dto;

import java.util.List;

public record ChatResponse(
    String answer,
    String sql,
    List<String> columns,
    List<List<Object>> rows,
    List<TraceStep> trace) {
  /**
   * Sub-agent 실행 추적. agent: 호출된 sub-agent 이름 ("question-decomposer", "schema-explorer", ...) tool :
   * (선택) sub-agent 가 호출한 도구 이름 — sub-agent 자체 호출이면 null
   */
  public record TraceStep(String agent, String tool, Object input, Object output) {}
}
