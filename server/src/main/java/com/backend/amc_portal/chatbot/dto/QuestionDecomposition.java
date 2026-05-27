package com.backend.amc_portal.chatbot.dto;

import java.util.List;

public record QuestionDecomposition(List<String> keywords, List<String> valueLookups) {
    public QuestionDecomposition {
        keywords = keywords != null ? keywords : List.of();
        valueLookups = valueLookups != null ? valueLookups : List.of();
    }
}
