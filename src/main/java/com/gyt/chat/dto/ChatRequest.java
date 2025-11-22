package com.gyt.chat.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private boolean enableThinking; // 思维链开关，默认false
}