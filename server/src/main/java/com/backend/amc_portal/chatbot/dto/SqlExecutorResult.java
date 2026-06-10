package com.backend.amc_portal.chatbot.dto;

import java.util.List;

public sealed interface SqlExecutorResult {

  record Ok(int rowCount, List<String> columns, List<List<Object>> rows, String executedSql)
      implements SqlExecutorResult {}

  record RetryAuthor(PriorError priorError, String executedSql) implements SqlExecutorResult {}

  record Fail(String code, String message, String executedSql) implements SqlExecutorResult {}
}
