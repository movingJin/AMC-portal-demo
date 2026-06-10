package com.backend.amc_portal.common.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditConfig {

  /**
   * Spring Data Auditing 의 기본 provider 는 LocalDateTime 을 반환하는데, 우리 BaseTimeEntity 의
   * createdAt/updatedAt 필드는 OffsetDateTime 이라 변환 단계에서 실패한다 (Spring Data 지원 destination 목록에
   * OffsetDateTime 미포함).
   *
   * <p>OffsetDateTime 을 직접 반환해 변환 자체를 건너뛰도록 한다.
   */
  @Bean
  public DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
  }
}
