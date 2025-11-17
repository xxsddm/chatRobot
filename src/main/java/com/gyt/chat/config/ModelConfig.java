package com.gyt.chat.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {
    @Bean
    ChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey("5007dd8ce8cb40f69bde8508377c35e8.ESlHwMubfN6Gs8AD")
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .modelName("glm-4.5-flash")
                .build();
//        return OpenAiChatModel.builder()
//                .apiKey("sk-khrctnxwaigqwbihxhwgysmhmzinkxzudvmphuegnxoxssuw")
//                .baseUrl("https://api.siliconflow.cn/v1")
//                .modelName("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B")
//                .build();
    }
}
