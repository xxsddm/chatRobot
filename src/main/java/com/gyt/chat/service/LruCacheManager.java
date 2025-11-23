package com.gyt.chat.service;

import com.gyt.chat.config.MapDbConfig;
import com.gyt.chat.model.ChatSessionEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 简化版LRU缓存管理器
 */
@Slf4j
@Service
public class LruCacheManager {
    
    private final MapDbConfig config;
    private final MapDbStorageService storageService;
    
    // 内存缓存 - 使用LinkedHashMap实现LRU
    private final Map<String, ChatSessionEntity> memoryCache;
    
    // 锁机制
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public LruCacheManager(MapDbConfig config, MapDbStorageService storageService) {
        this.config = config;
        this.storageService = storageService;
        
        // 初始化LRU缓存，按访问时间排序
        this.memoryCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ChatSessionEntity> eldest) {
                return size() > config.getMemoryCacheSize();
            }
        });
    }

    /**
     * 应用启动时从磁盘加载会话到内存缓存
     */
    @PostConstruct
    public void loadSessionsFromDisk() {
        try {
            log.info("开始从磁盘加载会话到内存缓存...");
            
            // 获取磁盘中的所有会话
            List<ChatSessionEntity> diskSessions = storageService.getAllSessions(config.getMemoryCacheSize());
            
            if (diskSessions.isEmpty()) {
                log.info("磁盘中没有会话数据需要加载");
                return;
            }

            lock.writeLock().lock();
            try {
                // 将会话加载到内存缓存
                for (ChatSessionEntity session : diskSessions) {
                    memoryCache.put(session.getSessionId(), session);
                }
                
                log.info("成功从磁盘加载 {} 个会话到内存缓存", diskSessions.size());
                log.info("当前内存缓存大小: {}，磁盘总会话数: {}", memoryCache.size(), storageService.getSessionCount());
                
            } finally {
                lock.writeLock().unlock();
            }
            
        } catch (Exception e) {
            log.error("从磁盘加载会话到内存缓存失败", e);
        }
    }
    
    /**
     * 获取会话，优先从内存缓存
     */
    public ChatSessionEntity getSession(String sessionId) {
        lock.readLock().lock();
        try {
            ChatSessionEntity session = memoryCache.get(sessionId);
            
            if (session != null) {
                // 更新访问时间
                log.debug("从内存缓存获取会话: {}", sessionId);
                return session;
            }
            
            // 内存中不存在，从磁盘加载
            session = storageService.getSession(sessionId);
            if (session != null) {
                // 加入内存缓存
                memoryCache.put(sessionId, session);
                log.debug("从磁盘加载并缓存会话: {}", sessionId);
            }
            
            return session;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 保存会话到缓存和磁盘
     */
    public void saveSession(String sessionId, ChatSessionEntity session) {
        lock.writeLock().lock();
        try {
            // 保存到磁盘
            if (!storageService.saveSession(session)) {
                return;
            }

            // 更新内存缓存
            memoryCache.put(sessionId, session);

            log.debug("会话保存到缓存和磁盘: {}", sessionId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        lock.writeLock().lock();
        try {
            // 从内存缓存删除
            memoryCache.remove(sessionId);

            // 从磁盘删除
            boolean result = storageService.deleteSession(sessionId);
            
            log.info("会话删除完成: {}", sessionId);
            return result;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            memoryCache.clear();
            log.info("内存缓存已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("memoryCacheSize", memoryCache.size());
            stats.put("totalSessions", storageService.getSessionCount());
            stats.put("diskUsageMb", getDiskUsageMb());
            stats.put("cacheHitRate", calculateCacheHitRate());
            
            return stats;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    

    
    /**
     * 获取磁盘使用量（MB）
     */
    private long getDiskUsageMb() {
        try {
            java.io.File dbFile = new java.io.File(config.getDataPath(), config.getDbFileName());
            if (dbFile.exists()) {
                return dbFile.length() / (1024 * 1024);
            }
        } catch (Exception e) {
            log.error("获取磁盘使用量失败", e);
        }
        return 0;
    }
    
    /**
     * 计算缓存命中率
     */
    private double calculateCacheHitRate() {
        // 简化计算，基于内存缓存的存在
        if (memoryCache.isEmpty()) {
            return 0.0;
        }
        
        // 这里可以基于实际访问统计来计算，暂时返回一个估算值
        return Math.min(0.95, (double) memoryCache.size() / storageService.getSessionCount());
    }
    
    /**
     * 获取内存缓存中的所有会话
     */
    public List<ChatSessionEntity> getMemoryCacheSessions() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(memoryCache.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ChatSessionEntity> getAllSessions() {
        if (!memoryCache.isEmpty()) {
            return memoryCache.values()
                    .stream()
                    .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                    .toList();
        }
        lock.readLock().lock();
        try {
            List<ChatSessionEntity> sessions = storageService.getAllSessions(MapDbConfig.memoryCacheSize);
            for (ChatSessionEntity session : sessions) {
                memoryCache.put(session.getSessionId(), session);
            }
            return sessions;
        } finally {
            lock.readLock().unlock();
        }
    }


    /**
     * 检查会话是否在内存缓存中
     */
    public boolean isInMemoryCache(String sessionId) {
        return memoryCache.containsKey(sessionId);
    }
}