package com.gyt.chat.controller;

import com.gyt.chat.dto.ChatRequest;
import com.gyt.chat.dto.ChatResponse;
import com.gyt.chat.model.ChatSessionEntity;
import com.gyt.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

//    @PostMapping
//    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
//        // 如果没有提供sessionId，生成一个新的
//        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
//            request.setSessionId(UUID.randomUUID().toString());
//        }
//
//        // 调用聊天服务
//        String response = chatService.chat(request.getSessionId(), request.getMessage());
//
//        // 构建响应
//        ChatResponse chatResponse = new ChatResponse();
//        chatResponse.setResponse(response);
//        chatResponse.setSessionId(request.getSessionId());
//        chatResponse.setHistory(ChatResponse.convertHistory(chatService.getHistory(request.getSessionId())));
//        chatResponse.setTimestamp(System.currentTimeMillis());
//
//        return ResponseEntity.ok(chatResponse);
//    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        // 如果没有提供sessionId，生成一个新的
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        // 调用流式聊天服务，传入思维链开关参数
        return chatService.chatStream(request.getSessionId(), request.getMessage(), request.isEnableThinking());
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
    
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatResponse>> getAllSessions() {
        List<ChatSessionEntity> sessions = chatService.getAllSessions();
        List<ChatResponse> responses = sessions.stream()
                .map(session -> {
                    ChatResponse response = new ChatResponse();
                    response.setSessionId(session.getSessionId());
                    response.setHistory(ChatResponse.convertHistory(session.getMessages()));
                    response.setTimestamp(session.getUpdatedAt()
                            .atZone(java.time.ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli());
                    return response;
                })
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp())) // 按时间降序排序
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
}