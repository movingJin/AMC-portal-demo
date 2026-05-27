package com.backend.amc_portal.chatbot.controller;

import com.backend.amc_portal.chatbot.dto.ChatRequest;
import com.backend.amc_portal.chatbot.dto.ChatResponse;
import com.backend.amc_portal.chatbot.service.ChatbotService;
import com.backend.amc_portal.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ApiResponse<ChatResponse> ask(@Valid @RequestBody ChatRequest req) {
        return ApiResponse.ok(chatbotService.ask(req));
    }
}
