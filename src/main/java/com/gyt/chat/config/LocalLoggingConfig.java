package com.gyt.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * 本地环境日志配置
 * 只在local profile下生效，日志只输出到控制台
 */
@Configuration
@Profile("local")
@Slf4j
public class LocalLoggingConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("应用启动: {}", applicationName);
        log.info("环境: local");
        log.info("日志配置: 只输出到控制台");
        log.info("========================================");
    }
}