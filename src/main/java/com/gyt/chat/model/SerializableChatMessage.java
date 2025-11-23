package com.gyt.chat.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 可序列化的聊天消息包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerializableChatMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER,
        AI,
        SYSTEM
    }
    
    /**
     * 从 ChatMessage 转换为 SerializableChatMessage
     */
    public static SerializableChatMessage from(ChatMessage message) {
        if (message instanceof UserMessage) {
            return new SerializableChatMessage(MessageType.USER, ((UserMessage) message).singleText());
        } else if (message instanceof AiMessage) {
            return new SerializableChatMessage(MessageType.AI, ((AiMessage) message).text());
        } else if (message instanceof SystemMessage) {
            return new SerializableChatMessage(MessageType.SYSTEM, ((SystemMessage) message).text());
        } else {
            throw new IllegalArgumentException("不支持的消息类型: " + message.getClass().getName());
        }
    }
    
    /**
     * 从 SerializableChatMessage 转换为 ChatMessage
     */
    public ChatMessage toChatMessage() {
        switch (type) {
            case USER:
                return new UserMessage(content);
            case AI:
                return new AiMessage(content);
            case SYSTEM:
                return new SystemMessage(content);
            default:
                throw new IllegalArgumentException("不支持的消息类型: " + type);
        }
    }
}