package com.example.chat.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
}