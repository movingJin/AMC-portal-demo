package com.backend.amc_portal.chatbot.dto;

import java.util.List;

public sealed interface SqlAuthorResult {

  record Authored(String intent, String schemaCitation, String sql) implements SqlAuthorResult {}

  record ValueUnconfirmed(String value, List<String> candidateColumns, String requestMessage)
      implements SqlAuthorResult {}

  record SchemaInsufficient(String reason) implements SqlAuthorResult {}
}
