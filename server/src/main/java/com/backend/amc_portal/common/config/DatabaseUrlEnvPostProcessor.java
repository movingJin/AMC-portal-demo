package com.backend.amc_portal.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * .env 의 DATABASE_URL (libpq 형식: {@code postgresql://user:pass@host:port/db}) 을
 * 표준 Spring 프로퍼티로 분해해 등록한다:
 *   - spring.datasource.url       = jdbc:postgresql://host:port/db
 *   - spring.datasource.username  = user
 *   - spring.datasource.password  = pass
 *
 * PostgreSQL JDBC 드라이버는 URL 에 자격증명을 박는 형식을 지원하지 않아 별도 프로퍼티가 필요하다.
 *
 * 실행 순서: spring-dotenv (HIGHEST_PRECEDENCE) 가 .env 를 먼저 로드 → 본 처리기 (LOWEST_PRECEDENCE) 가
 * 그 결과를 읽어 분해. 등록: {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 *
 * spring.datasource.url 이 이미 명시되어 있으면 건드리지 않는다.
 */
public class DatabaseUrlEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        System.out.println("[DatabaseUrlEnvPostProcessor] invoked. property sources: " + env.getPropertySources());
        if (env.getProperty("spring.datasource.url") != null) {
            System.out.println("[DatabaseUrlEnvPostProcessor] spring.datasource.url 이미 설정됨 — skip");
            return;
        }

        String raw = env.getProperty("DATABASE_URL");
        System.out.println("[DatabaseUrlEnvPostProcessor] DATABASE_URL resolved? " + (raw != null ? "yes (len=" + raw.length() + ")" : "NULL"));
        if (raw == null || raw.isBlank()) return;

        try {
            URI uri = URI.create(raw);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();

            if (host == null || path.isBlank()) {
                System.err.println("[DatabaseUrlEnvPostProcessor] DATABASE_URL 형식이 올바르지 않음 (host/db 누락): " + maskPassword(raw));
                return;
            }

            Map<String, Object> props = new HashMap<>();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            props.put("spring.datasource.url", jdbcUrl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                props.put("spring.datasource.username", urlDecode(parts[0]));
                if (parts.length > 1) {
                    props.put("spring.datasource.password", urlDecode(parts[1]));
                }
            }

            env.getPropertySources().addFirst(new MapPropertySource("databaseUrlParsed", props));
            System.out.println("[DatabaseUrlEnvPostProcessor] DATABASE_URL → " + jdbcUrl
                    + " (user=" + props.get("spring.datasource.username") + ")");
        } catch (Exception e) {
            System.err.println("[DatabaseUrlEnvPostProcessor] DATABASE_URL 파싱 실패: " + e.getMessage());
        }
    }

    private static String urlDecode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8); } catch (Exception e) { return s; }
    }

    private static String maskPassword(String s) {
        return s.replaceAll(":([^:@/]+)@", ":***@");
    }
}
