package com.backend.amc_portal.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncConfig {

  /** schema-explorer 등 sub-agent 의 병렬 호출용. 동시 최대 8개. */
  @Bean("subAgentExecutor")
  public Executor subAgentExecutor() {
    return Executors.newFixedThreadPool(
        8,
        r -> {
          Thread t = new Thread(r, "sub-agent-");
          t.setDaemon(true);
          return t;
        });
  }
}
