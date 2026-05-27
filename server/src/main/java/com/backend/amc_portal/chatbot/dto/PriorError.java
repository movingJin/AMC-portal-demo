package com.backend.amc_portal.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriorError(String code, String message, SuspectFilter suspectFilter, String executedSql) {
    public record SuspectFilter(String column, String value) {}

    public static PriorError of(String code, String message, String sql) {
        return new PriorError(code, message, null, sql);
    }
    public static PriorError zeroRows(String column, String value, String sql) {
        return new PriorError("ZERO_ROWS_WITH_VALUE_FILTER",
                "값 기반 필터가 있는데 0건 — 컬럼 선택 의심",
                new SuspectFilter(column, value), sql);
    }
}
