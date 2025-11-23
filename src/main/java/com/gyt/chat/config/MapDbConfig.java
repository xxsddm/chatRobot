package com.gyt.chat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mapdb")
public class MapDbConfig {
    
    /**
     * 数据文件存储路径
     */
    @Value("${mapdb.dataPath:/data/chat-history}")
    private String dataPath;
    
    /**
     * 数据库文件名
     */
    private String dbFileName = "chat-history.db";
    
    /**
     * 最大会话数量
     */
    private int maxSessions = 1000;
    
    /**
     * 每个会话最大轮次
     */
    private int maxRoundsPerSession = 100;
    
    /**
     * 磁盘使用阈值（MB）
     */
    private long diskThresholdMb = 500;
    
    /**
     * 内存缓存大小
     */
    public static int memoryCacheSize = 100;
    
    /**
     * 是否启用事务
     */
    private boolean enableTransactions = true;
    
    /**
     * 数据同步间隔（秒）
     */
    private int syncIntervalSeconds = 30;
    
    /**
     * 是否启用压缩
     */
    private boolean enableCompression = true;
    
    /**
     * 缓存过期时间（分钟）
     */
    private int cacheExpireMinutes = 60;

    public int getMemoryCacheSize() {
        return memoryCacheSize;
    }
}