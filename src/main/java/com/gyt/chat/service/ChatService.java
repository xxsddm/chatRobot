package com.gyt.chat.service;

import com.gyt.chat.model.ChatSessionEntity;
import com.gyt.chat.model.StreamChatHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel openAiChatModel;

    @Resource(name = "ollamaAiStreamingChatModel")
    private StreamingChatModel ollamaChatModel;

    @Value("${model.primary:openai}")
    private String PRIMARY_MODEL;

    // 使用MapDB存储会话历史，支持持久化和LRU管理
    private final LruCacheManager cacheManager;
    
    // 系统提示词，确保AI以正确的Markdown格式回复
    private static final String SYSTEM_PROMPT = """
        你是一个智能对话助手。请遵循以下规则：
        
        1. **格式要求**：始终以正确的Markdown格式回复
        2. **代码块**：使用```语言名称 的格式，确保代码块完整闭合
        3. **表格**：使用标准的Markdown表格语法，确保格式正确
        4. **换行**：适当使用换行符，确保内容结构清晰
        5. **完整性**：确保所有Markdown语法元素完整，不要截断
        
        示例格式：
        ```python
        def hello():
            print("Hello, World!")
        ```
        
        | 列1 | 列2 |
        |-----|-----|
        | 内容1 | 内容2 |
        
        请以清晰、结构化的方式回复用户的问题。
        """;

    // 思维链模式系统提示词
    private static final String THINKING_PROMPT = """
        你是一个智能对话助手。请遵循以下规则：
        
        1. **思维链要求**：在回答问题时，请先展示你的思考过程，然后给出最终答案
        2. **格式要求**：始终以正确的Markdown格式回复
        3. **思考过程**：使用"思考过程："标题明确标识你的推理步骤
        4. **最终答案**：使用"答案："标题明确标识最终回复
        5. **代码块**：使用```语言名称 的格式，确保代码块完整闭合
        6. **表格**：使用标准的Markdown表格语法，确保格式正确
        
        示例格式：
        
        ### 思考过程：
        1. 首先，我需要理解用户的问题...
        2. 然后，我考虑可能的解决方案...
        3. 最后，我选择最佳的答案...
        
        ### 答案：
        这是最终的回复内容...
        
        ```python
        def hello():
            print("Hello, World!")
        ```
        
        请严格按照这个格式回复，确保思考过程和最终答案都清晰明确。
        """;

    public Flux<String> chatStream(String sessionId, String message, boolean enableThinking) {
        // 获取或创建会话历史
        ChatSessionEntity session = cacheManager.getSession(sessionId);
        List<ChatMessage> history;
        
        if (session == null) {
            // 创建新会话
            session = new ChatSessionEntity();
            session.setSessionId(sessionId);
            session.setTitle(message.length() > 20 ? message.substring(0, 20) + "..." : message);
            session.setCreatedAt(LocalDateTime.now());
            session.setEnableThinking(enableThinking);
            history = new ArrayList<>();
            
            // 添加系统消息
            String systemPrompt = enableThinking ? THINKING_PROMPT : SYSTEM_PROMPT;
            history.add(new SystemMessage(systemPrompt));
        } else {
            // 使用现有会话历史
            history = session.getMessages();
            
            // 更新思维链设置
            session.setEnableThinking(enableThinking);
            
            // 如果历史为空，添加系统消息
            if (history.isEmpty()) {
                String systemPrompt = enableThinking ? THINKING_PROMPT : SYSTEM_PROMPT;
                history.add(new SystemMessage(systemPrompt));
                session.addMessage(new SystemMessage(systemPrompt));
            }
        }

        // 检查并清理会话历史
        checkAndCleanupSession(session);

        // 添加用户消息
        UserMessage userMessage = new UserMessage(message);
        history.add(userMessage);
        session.addMessage(userMessage);
        
        // 创建Sink用于流式输出
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StreamChatHandler handler = new StreamChatHandler(history, sink, enableThinking);

        // 根据配置选择主模型
        StreamingChatModel selectedModel = Objects.equals("ollama", PRIMARY_MODEL) ? ollamaChatModel : openAiChatModel;
        
        log.info("思维链模式: {}, 使用模型: {}, 会话ID: {}, 消息: {}", 
                enableThinking, selectedModel.getClass().getName(), sessionId, message);
        
        // 生成AI回复（流式）
        selectedModel.chat(history, handler);

        asyncUpdateSession(sessionId, handler, session, history);
        
        return sink.asFlux();
    }

    private void asyncUpdateSession(String sessionId, StreamChatHandler handler,
                                    ChatSessionEntity session, List<ChatMessage> history) {
        log.info("sessionId async update start: {}", sessionId);
        // 保存会话到缓存和磁盘（异步）
        handler.getResponseMono().subscribe(
                response -> {
                    // 流式响应完成时保存会话
                    AiMessage aiMessage = new AiMessage(response);
                    history.add(aiMessage);
                    session.addMessage(aiMessage);  // 添加AI消息到会话
                    session.setUpdatedAt(LocalDateTime.now());
                    cacheManager.saveSession(sessionId, session);
                    log.info("sessionId async update finish: {}", sessionId);
                },
                error -> {
                    log.error("流式响应错误: {}", sessionId, error);
                    // 即使出错也要保存会话
                    cacheManager.saveSession(sessionId, session);
                }
        );
    }

    private void checkAndCleanupSession(ChatSessionEntity session) {
        int maxRounds = 100; // 默认最大轮次，后续可以从配置获取
        int currentRounds = session.getMessages().size();
        
        if (currentRounds > maxRounds) {
            log.info("会话 {} 超过最大轮次限制 ({} > {})，开始清理", session.getSessionId(), currentRounds, maxRounds);
            
            // 保留系统消息和最近的消息
            List<ChatMessage> messages = session.getMessages();
            List<ChatMessage> systemMessages = messages.stream()
                    .filter(msg -> msg instanceof SystemMessage)
                    .toList();
            
            // 保留最近的maxRounds - systemMessages.size()条消息
            int keepCount = maxRounds - systemMessages.size();
            if (keepCount > 0) {
                List<ChatMessage> recentMessages = messages.stream()
                        .filter(msg -> !(msg instanceof SystemMessage))
                        .skip(Math.max(0, messages.size() - systemMessages.size() - keepCount))
                        .toList();
                
                // 重建消息列表
                List<ChatMessage> newMessages = new ArrayList<>();
                newMessages.addAll(systemMessages);
                newMessages.addAll(recentMessages);
                
                session.setMessages(newMessages);
                log.info("会话 {} 清理完成，保留 {} 条消息", session.getSessionId(), newMessages.size());
            } else {
                // 如果keepCount <= 0，只保留系统消息
                session.setMessages(new ArrayList<>(systemMessages));
                log.warn("会话 {} 清理完成，只保留 {} 条系统消息", session.getSessionId(), systemMessages.size());
            }
        }
    }
    
    public List<ChatMessage> getHistory(String sessionId) {
        ChatSessionEntity session = cacheManager.getSession(sessionId);
        if (session != null) {
            return session.getMessages();
        }
        return new ArrayList<>();
    }
    
    public void clearHistory(String sessionId) {
        cacheManager.deleteSession(sessionId);
        log.info("会话历史已清除: {}", sessionId);
    }
    
    /**
     * 获取所有会话列表
     */
    public List<ChatSessionEntity> getAllSessions() {
        // 优先从磁盘存储获取所有会话，如果没有则从内存缓存获取
        return cacheManager.getAllSessions();
    }
    
    /**
     * 删除指定会话
     */
    public boolean deleteSession(String sessionId) {
        return cacheManager.deleteSession(sessionId);
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return cacheManager.getCacheStats();
    }
}