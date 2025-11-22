package com.gyt.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * 生产环境日志配置
 * 只在prod profile下生效，日志输出到控制台和文件
 */
@Configuration
@Profile("prod")
@Slf4j
public class ProdLoggingConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("应用启动: {}", applicationName);
        log.info("环境: prod");
        log.info("日志配置: 输出到控制台和 /data/log/service.log");
        log.info("========================================");
    }
}