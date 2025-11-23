package com.gyt.chat.service;

import com.gyt.chat.config.MapDbConfig;
import com.gyt.chat.model.ChatSessionEntity;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 简化版MapDB存储服务 - 专注于磁盘存储
 */
@Slf4j
@Service
public class MapDbStorageService {
    
    private final MapDbConfig config;
    private DB db;
    private HTreeMap<String, ChatSessionEntity> sessionMap;
    private HTreeMap<String, Long> lruMap; // 用于LRU管理
    private ScheduledExecutorService scheduler;
    
    public MapDbStorageService(MapDbConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        try {
            // 创建数据目录
            File dataDir = new File(config.getDataPath());
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // 初始化数据库
            String dbPath = config.getDataPath() + File.separator + config.getDbFileName();
            DBMaker.Maker dbMaker = DBMaker.fileDB(dbPath)
                    .fileMmapEnableIfSupported()
                    .closeOnJvmShutdown();

            if (config.isEnableTransactions()) {
                dbMaker.transactionEnable();
            } else {
                dbMaker.concurrencyDisable();
            }

            this.db = dbMaker.make();

            // 创建会话存储Map
            this.sessionMap = db.hashMap("sessions")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .createOrOpen();

            // 创建LRU管理Map
            this.lruMap = db.hashMap("lru")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .createOrOpen();

            // 启动定时同步任务
            startSyncScheduler();

            log.info("MapDB存储服务初始化完成，数据路径: {}", dbPath);
            log.info("当前存储会话数: {}", sessionMap.size());

        } catch (Exception e) {
            log.error("MapDB存储服务初始化失败", e);
            throw new RuntimeException("Failed to initialize MapDB storage", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }

            if (db != null) {
                db.commit();
                db.close();
            }

            log.info("MapDB存储服务关闭完成");
        } catch (Exception e) {
            log.error("MapDB存储服务关闭失败", e);
        }
    }
    
    /**
     * 保存会话
     */
    public boolean saveSession(ChatSessionEntity session) {
        try {
            Long lastAccessTime = lruMap.get(session.getSessionId());
            if (lastAccessTime != null &&
                    session.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() <= lastAccessTime) {
                log.info("会话未更新，无需保存: {}", session.getSessionId());
                return false;
            }

            // 更新访问信息
            session.updateAccessInfo();
            
            // 保存到数据库
            sessionMap.put(session.getSessionId(), session);
            
            // 更新LRU信息
            lruMap.put(session.getSessionId(), session.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
            
            // 修剪消息数量
            trimSessionMessages(session);
            db.commit();
            log.info("sessionId async update finish in MapDB: {}", session.getSessionId());
            
            return true;
        } catch (Exception e) {
            log.error("保存会话失败: {}", session.getSessionId(), e);
        }
        return false;
    }
    
    /**
     * 获取会话
     */
    public ChatSessionEntity getSession(String sessionId) {
        try {
            // 从数据库获取
            ChatSessionEntity session = sessionMap.get(sessionId);
            if (session != null) {
                // 更新访问时间
                log.debug("从数据库获取会话: {}", sessionId);
                return session;
            }

            log.debug("会话不存在: {}", sessionId);
            return null;

        } catch (Exception e) {
            log.error("获取会话失败: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        try {
            ChatSessionEntity removed = sessionMap.remove(sessionId);
            lruMap.remove(sessionId);
            
            if (removed != null) {
                log.info("会话删除成功: {}", sessionId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("删除会话失败: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 获取所有会话ID
     */
    public Set<String> getAllSessionIds() {
        return new HashSet<>(sessionMap.keySet());
    }
    
    /**
     * 清空所有会话
     */
    public void clearAllSessions() {
        sessionMap.clear();
        lruMap.clear();
        log.info("所有会话已清空");
    }
    
    /**
     * 获取会话数量
     */
    public int getSessionCount() {
        return sessionMap.size();
    }
    
    /**
     * 获取所有会话
     */
    public List<ChatSessionEntity> getAllSessions(int nlimit) {
        return sessionMap.values().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(nlimit)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查并清理数据
     */
    private void checkAndCleanup() {
        try {
            // 检查会话数量
            if (sessionMap.size() > config.getMaxSessions()) {
                cleanupByLRU();
            }
            
            // 检查磁盘使用
            if (isDiskUsageExceeded()) {
                cleanupByDiskUsage();
            }
            
        } catch (Exception e) {
            log.error("清理检查失败", e);
        }
    }
    
    /**
     * 根据LRU清理会话
     */
    private void cleanupByLRU() {
        try {
            int targetSize = config.getMaxSessions() * 80 / 100; // 清理到80%
            int currentSize = sessionMap.size();
            
            if (currentSize <= targetSize) {
                return;
            }
            
            // 按访问时间排序，删除最久未访问的
            List<Map.Entry<String, Long>> sortedEntries = lruMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .toList();
            
            int toRemove = currentSize - targetSize;
            for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                String sessionId = sortedEntries.get(i).getKey();
                deleteSession(sessionId);
                log.info("LRU清理会话: {}", sessionId);
            }
            
            log.info("LRU清理完成，从 {} 清理到 {} 个会话", currentSize, sessionMap.size());
            
        } catch (Exception e) {
            log.error("LRU清理失败", e);
        }
    }
    
    /**
     * 根据磁盘使用清理
     */
    private void cleanupByDiskUsage() {
        try {
            // 删除最久未访问的50%会话
            List<Map.Entry<String, Long>> sortedEntries = lruMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toList());
            
            int toRemove = sortedEntries.size() / 2;
            for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                String sessionId = sortedEntries.get(i).getKey();
                deleteSession(sessionId);
                log.info("磁盘清理会话: {}", sessionId);
            }
            
            log.info("磁盘使用清理完成");
            
        } catch (Exception e) {
            log.error("磁盘清理失败", e);
        }
    }
    
    /**
     * 检查磁盘使用是否超限
     */
    private boolean isDiskUsageExceeded() {
        try {
            File dbFile = new File(config.getDataPath(), config.getDbFileName());
            if (!dbFile.exists()) {
                return false;
            }
            
            long fileSizeMb = dbFile.length() / (1024 * 1024);
            return fileSizeMb > config.getDiskThresholdMb();
            
        } catch (Exception e) {
            log.error("检查磁盘使用失败", e);
            return false;
        }
    }
    
    /**
     * 修剪会话消息
     */
    private void trimSessionMessages(ChatSessionEntity session) {
        try {
            List<ChatMessage> messages = session.getMessages();
            if (messages.size() > config.getMaxRoundsPerSession() * 2) { // 每条消息包含用户和AI回复
                // 保留最新的消息，移除旧的
                int toRemove = messages.size() - config.getMaxRoundsPerSession() * 2;
                messages.subList(0, toRemove).clear();
                log.info("修剪会话 {} 的消息，移除 {} 条", session.getSessionId(), toRemove);
            }
        } catch (Exception e) {
            log.error("修剪会话消息失败", e);
        }
    }

    /**
     * 启动定时同步任务
     */
    private void startSyncScheduler() {
        try {
            scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (db != null && !db.isClosed()) {
                        db.commit();
                        log.debug("MapDB数据同步完成");
                    }
                } catch (Exception e) {
                    log.error("MapDB数据同步失败", e);
                }
            }, config.getSyncIntervalSeconds(), config.getSyncIntervalSeconds(), TimeUnit.SECONDS);
            
            log.info("MapDB定时同步任务启动，间隔: {}秒", config.getSyncIntervalSeconds());
            
        } catch (Exception e) {
            log.error("启动定时同步任务失败", e);
        }
    }
}