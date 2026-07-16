package com.zhifutong.customer.controller;

import com.zhifutong.customer.application.ChatApplicationService;
import com.zhifutong.customer.dto.ChatRequest;
import com.zhifutong.customer.vo.ApiResponse;
import com.zhifutong.customer.vo.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatApplicationService chatApplicationService;

    public ChatController(ChatApplicationService chatApplicationService) {
        this.chatApplicationService = chatApplicationService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatApplicationService.chat(request.conversationId(), request.question()));
    }
}
