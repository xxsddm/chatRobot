package com.example.chat.controller;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;
import com.example.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        // 如果没有提供sessionId，生成一个新的
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        // 调用聊天服务
        String response = chatService.chat(request.getSessionId(), request.getMessage());
        
        // 构建响应
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSessionId(request.getSessionId());
        chatResponse.setHistory(ChatResponse.convertHistory(chatService.getHistory(request.getSessionId())));
        chatResponse.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.ok(chatResponse);
    }
    
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ChatResponse> getHistory(@PathVariable String sessionId) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setSessionId(sessionId);
        chatResponse.setHistory(ChatResponse.convertHistory(chatService.getHistory(sessionId)));
        chatResponse.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.ok(chatResponse);
    }
    
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return ResponseEntity.ok().build();
    }
}