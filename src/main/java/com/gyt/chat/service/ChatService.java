package com.gyt.chat.service;

import com.gyt.chat.model.StreamChatHandler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

    // 使用内存存储会话历史，实际生产环境可以替换为数据库
    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();
    
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
        List<ChatMessage> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 添加系统消息，确保返回格式化的Markdown
        if (history.isEmpty()) {
            String systemPrompt = enableThinking ? THINKING_PROMPT : SYSTEM_PROMPT;
            history.add(new SystemMessage(systemPrompt));
        }

        // 添加用户消息
        history.add(new UserMessage(message));
        
        // 创建Sink用于流式输出
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StreamChatHandler handler = new StreamChatHandler(history, sink, enableThinking);

        // 根据配置选择主模型
        StreamingChatModel selectedModel = Objects.equals("ollama", PRIMARY_MODEL) ? ollamaChatModel : openAiChatModel;
        
        log.info("思维链模式: {}, 使用模型: {}, 消息: {}", enableThinking, selectedModel.getClass().getName(), message);
        
        // 生成AI回复（流式）
        selectedModel.chat(history, handler);
        
        return sink.asFlux();
    }
    
    public List<ChatMessage> getHistory(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public void clearHistory(String sessionId) {
        sessionHistory.remove(sessionId);
    }
}