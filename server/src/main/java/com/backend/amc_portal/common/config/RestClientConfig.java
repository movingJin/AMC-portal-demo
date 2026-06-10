package com.backend.amc_portal.common.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean("nl2sqlRestClient")
  public RestClient nl2sqlRestClient(
      @Value("${app.nl2sql.base-url}") String baseUrl,
      @Value("${app.nl2sql.api-keys}") String apiKeys,
      @Value("${app.nl2sql.timeout-seconds}") long timeoutSeconds) {

    String firstKey = apiKeys.split(",")[0].trim();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10));
    factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

    return RestClient.builder()
        .requestFactory(factory)
        .baseUrl(baseUrl)
        .defaultHeader("X-API-Key", firstKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  @Bean("azureOpenAiRestClient")
  public RestClient azureOpenAiRestClient(
      @Value("${app.azure.openai.endpoint}") String endpoint,
      @Value("${app.azure.openai.api-key}") String apiKey) {

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10));
    factory.setReadTimeout(Duration.ofSeconds(120));

    return RestClient.builder()
        .requestFactory(factory)
        .baseUrl(stripTrailingSlash(endpoint))
        .defaultHeader("api-key", apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  private String stripTrailingSlash(String s) {
    return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
  }
}
