package com.gyt.chat.service;

import com.gyt.chat.model.StreamChatHandler;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    @Resource(name="openAiStreamingChatModel")
    private final StreamingChatModel chatModel;
    
    // 使用内存存储会话历史，实际生产环境可以替换为数据库
    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();

    public Flux<String> chatStream(String sessionId, String message) {
        // 获取或创建会话历史
        List<ChatMessage> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 添加系统消息，确保返回格式化的Markdown
        if (history.isEmpty()) {
            history.add(new SystemMessage("你是一个对话机器人。请始终以Markdown格式回复，特别是代码块要使用正确的语法高亮标记（如```python）。确保代码格式正确，包含适当的缩进和换行。"));
        }
        
        // 添加用户消息
        history.add(new UserMessage(message));
        
        // 创建Sink用于流式输出
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StreamChatHandler handler = new StreamChatHandler(history, sink);

        // 生成AI回复（流式）
        chatModel.chat(history, handler);
        
        return sink.asFlux();
    }
    
//    // 保留原有的非流式聊天方法用于兼容
//    public String chat(String sessionId, String message) {
//        // 获取或创建会话历史
//        List<ChatMessage> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
//
//        // 添加用户消息
//        history.add(new UserMessage(message));
//
//        // 生成AI回复
//        ChatResponse response = chatModel.chat(history);
//        AiMessage aiMessage = response.aiMessage();
//        String responseText = aiMessage.text();
//
//        // 添加AI回复到历史记录
//        history.add(aiMessage);
//
//        return responseText;
//    }
    
    public List<ChatMessage> getHistory(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public void clearHistory(String sessionId) {
        sessionHistory.remove(sessionId);
    }
}