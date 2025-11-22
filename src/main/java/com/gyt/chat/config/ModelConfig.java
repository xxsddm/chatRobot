package com.gyt.chat.config;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {
    @Value("${model.openai.api-key}")
    private String OPENAI_API_KEY;

    @Value("${model.openai.base-url}")
    private String OPENAI_BASE_URL;

    @Value("${model.openai.model-name}")
    private String OPENAI_MODEL_NAME;


    @Bean
    StreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .baseUrl(OPENAI_BASE_URL)
                .modelName(OPENAI_MODEL_NAME)
                .build();
//        return OpenAiStreamingChatModel.builder()
//                .apiKey("sk-khrctnxwaigqwbihxhwgysmhmzinkxzudvmphuegnxoxssuw")
//                .baseUrl("https://api.siliconflow.cn/v1")
//                .modelName("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B")
//                .build();
    }
}
