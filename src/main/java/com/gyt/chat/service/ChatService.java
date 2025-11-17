package com.gyt.chat.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Resource(name="openAiChatModel")
    private final ChatModel ChatModel;
    
    // 使用内存存储会话历史，实际生产环境可以替换为数据库
    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();

    public String chat(String sessionId, String message) {
        // 获取或创建会话历史
        List<ChatMessage> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 添加用户消息
        history.add(new UserMessage(message));
        
        // 生成AI回复
        ChatResponse response = ChatModel.chat(history);
        AiMessage aiMessage = response.aiMessage();
        String responseText = aiMessage.text();
        
        // 添加AI回复到历史记录
        history.add(aiMessage);
        
        return responseText;
    }
    
    public List<ChatMessage> getHistory(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public void clearHistory(String sessionId) {
        sessionHistory.remove(sessionId);
    }
}