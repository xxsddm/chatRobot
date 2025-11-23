package com.gyt.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LRU缓存条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LruCacheEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话实体
     */
    private ChatSessionEntity session;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;
    
    /**
     * 访问次数
     */
    private long accessCount;
    
    /**
     * 数据大小（字节）
     */
    private long dataSize;
    
    public LruCacheEntry(String sessionId, ChatSessionEntity session) {
        this.sessionId = sessionId;
        this.session = session;
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount = session.getAccessCount();
        this.dataSize = estimateDataSize();
    }
    
    /**
     * 更新访问信息
     */
    public void updateAccess() {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount++;
        if (session != null) {
            session.updateAccessInfo();
        }
    }
    
    /**
     * 估算数据大小
     */
    private long estimateDataSize() {
        if (session == null) {
            return 0;
        }
        
        long size = 48; // 基础对象大小
        size += sessionId != null ? sessionId.length() * 2 : 0;
        size += session.getTitle() != null ? session.getTitle().length() * 2 : 0;
        size += session.getMessageCount() * 1024; // 估算每条消息平均1KB
        
        return size;
    }
    
    /**
     * 检查是否为热点数据
     */
    public boolean isHotData(int threshold) {
        return accessCount >= threshold;
    }
}