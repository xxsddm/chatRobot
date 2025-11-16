package com.example.chat.dto;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ChatResponse {
    private String response;
    private String sessionId;
    private List<ChatMessageDto> history;
    private Long timestamp;
    
    @Data
    public static class ChatMessageDto {
        private String role;
        private String content;
        
        public static ChatMessageDto from(ChatMessage message) {
            ChatMessageDto dto = new ChatMessageDto();
            if (message instanceof UserMessage) {
                dto.setRole("user");
                dto.setContent(((UserMessage) message).singleText());
            } else if (message instanceof AiMessage) {
                dto.setRole("assistant");
                dto.setContent(((AiMessage) message).text());
            }
            return dto;
        }
    }
    
    public static List<ChatMessageDto> convertHistory(List<ChatMessage> history) {
        if (history == null) {
            return new ArrayList<>();
        }
        return history.stream()
                .map(ChatMessageDto::from)
                .collect(Collectors.toList());
    }
}