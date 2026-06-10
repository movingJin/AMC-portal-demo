package com.backend.amc_portal.chatbot.client;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class Nl2SqlClient {

  @Qualifier("nl2sqlRestClient")
  private final RestClient client;

  public Map<String, Object> listTables() {
    return post("/v1/tools/list_tables", Map.of());
  }

  public Map<String, Object> searchTables(String keyword, Integer topK) {
    Map<String, Object> body =
        topK != null ? Map.of("keyword", keyword, "limit", topK) : Map.of("keyword", keyword);
    return post("/v1/tools/search_tables", body);
  }

  public Map<String, Object> getTableSchema(String schema, String table) {
    return post("/v1/tools/get_table_schema", Map.of("table", qualify(schema, table)));
  }

  public Map<String, Object> getSampleRows(String schema, String table, Integer limit) {
    String qualified = qualify(schema, table);
    Map<String, Object> body =
        limit != null
            ? Map.of("table", qualified, "limit", Math.min(limit, 20))
            : Map.of("table", qualified);
    return post("/v1/tools/get_sample_rows", body);
  }

  /** schema 가 null/blank 이거나 table 이 이미 'schema.x' 형식이면 table 그대로, 아니면 'schema.table' 로 결합. */
  private static String qualify(String schema, String table) {
    if (table == null) return null;
    if (table.contains(".")) return table;
    if (schema == null || schema.isBlank()) return table;
    return schema + "." + table;
  }

  public Map<String, Object> executeSql(String sql, Integer maxRows) {
    Map<String, Object> body =
        maxRows != null
            ? Map.of("query", sql, "max_rows", Math.min(maxRows, 1000))
            : Map.of("query", sql);
    return post("/v1/tools/execute_sql", body);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> post(String path, Map<String, Object> body) {
    try {
      return client.post().uri(path).body(body).retrieve().body(Map.class);
    } catch (Exception e) {
      log.warn("NL2SQL API error on {}: {}", path, e.getMessage());
      return Map.of("error", e.getMessage());
    }
  }
}
