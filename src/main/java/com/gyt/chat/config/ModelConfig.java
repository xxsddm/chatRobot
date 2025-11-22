package com.gyt.chat.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ModelConfig {
    @Value("${model.openai.api-key}")
    private String OPENAI_API_KEY;

    @Value("${model.openai.base-url}")
    private String OPENAI_BASE_URL;

    @Value("${model.openai.model-name}")
    private String OPENAI_MODEL_NAME;

    @Value("${model.ollama.base-url:http://localhost:11434}")
    private String OLLAMA_BASE_URL;

    @Value("${model.ollama.model-name:qwen3:1.7b}")
    private String OLLAMA_MODEL_NAME;

    @Value("${model.temperature:0.7}")
    private double TEMPERATURE;

    @Value("${model.timeout.second:30}")
    private int TIMEOUT;


    @Bean
    StreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .baseUrl(OPENAI_BASE_URL)
                .modelName(OPENAI_MODEL_NAME)
                .temperature(TEMPERATURE)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .returnThinking(true)
                .build();
    }

    @Bean
    StreamingChatModel ollamaAiStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .modelName(OLLAMA_MODEL_NAME)
                .temperature(TEMPERATURE)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .returnThinking(true)
                .build();
    }
}
