package com.backend.amc_portal.chatbot.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOpenAiClient {

  @Qualifier("azureOpenAiRestClient")
  private final RestClient client;

  @Value("${app.azure.openai.deployment}")
  private String deployment;

  private static final String API_VERSION = "2024-08-01-preview";

  /** Tool calling 가능한 chat completion. tools=null/empty 이면 plain completion. */
  public JsonNode chatCompletion(
      List<Map<String, Object>> messages,
      List<Map<String, Object>> tools,
      boolean jsonObjectResponse) {
    String path =
        "/openai/deployments/" + deployment + "/chat/completions?api-version=" + API_VERSION;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("messages", messages);
    body.put("temperature", 0.1);
    if (tools != null && !tools.isEmpty()) {
      body.put("tools", tools);
      body.put("tool_choice", "auto");
    }
    if (jsonObjectResponse) {
      body.put("response_format", Map.of("type", "json_object"));
    }
    return client.post().uri(path).body(body).retrieve().body(JsonNode.class);
  }

  public JsonNode chatCompletion(
      List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    return chatCompletion(messages, tools, false);
  }
}
