package com.gyt.chat.model;

import dev.langchain4j.data.message.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话数据实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionEntity implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 聊天消息列表（使用可序列化的包装类）
     */
    private List<SerializableChatMessage> serializableMessages = new ArrayList<>();
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 访问时间（用于LRU）
     */
    private LocalDateTime accessedAt;
    
    /**
     * 访问次数（用于热点识别）
     */
    private long accessCount = 0;
    
    /**
     * 是否启用思维链
     */
    private boolean enableThinking = false;
    
    /**
     * 获取聊天消息列表（转换为 ChatMessage）
     */
    public List<ChatMessage> getMessages() {
        if (serializableMessages == null) {
            return new ArrayList<>();
        }
        return serializableMessages.stream()
                .map(SerializableChatMessage::toChatMessage)
                .collect(Collectors.toList());
    }
    
    /**
     * 设置聊天消息列表（从 ChatMessage 转换）
     */
    public void setMessages(List<ChatMessage> messages) {
        if (messages == null) {
            this.serializableMessages = new ArrayList<>();
        } else {
            this.serializableMessages = messages.stream()
                    .map(SerializableChatMessage::from)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 获取消息数量（轮次数）
     */
    public int getMessageCount() {
        return serializableMessages != null ? serializableMessages.size() : 0;
    }
    
    /**
     * 添加消息并更新访问时间
     */
    public void addMessage(ChatMessage message) {
        if (serializableMessages == null) {
            serializableMessages = new ArrayList<>();
        }
        serializableMessages.add(SerializableChatMessage.from(message));
        updatedAt = LocalDateTime.now();
        accessedAt = LocalDateTime.now();
        accessCount++;
    }
    
    /**
     * 移除指定索引的消息
     */
    public ChatMessage removeMessage(int index) {
        if (serializableMessages != null && index >= 0 && index < serializableMessages.size()) {
            SerializableChatMessage removed = serializableMessages.remove(index);
            updatedAt = LocalDateTime.now();
            return removed.toChatMessage();
        }
        return null;
    }
    
    /**
     * 清空消息
     */
    public void clearMessages() {
        if (serializableMessages != null) {
            serializableMessages.clear();
        }
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新访问信息
     */
    public void updateAccessInfo() {
        accessedAt = LocalDateTime.now();
        accessCount++;
    }
}