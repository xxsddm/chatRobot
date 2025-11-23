package com.gyt.chat.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StreamChatHandler implements StreamingChatResponseHandler {

    private final Stack<CodeBlockModel> stk = new Stack<>();

    private final List<ChatMessage> history;

    private final Sinks.Many<String> sink;
    
    private final boolean enableThinking;
    
    @Getter
    private final CompletableFuture<String> response = new CompletableFuture<>();
    
    private final StringBuilder responseBuilder = new StringBuilder();

    public StreamChatHandler(List<ChatMessage> history, Sinks.Many<String> sink, boolean enableThinking) {
        this.history = history;
        this.sink = sink;
        this.enableThinking = enableThinking;
    }

    @Override
    public void onPartialResponse(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return;
        }
        // 保留原始文本的换行符，确保Markdown格式正确
        String formattedResponse = originalText;
        sink.tryEmitNext(formattedResponse);
        responseBuilder.append(formattedResponse);
//        log.info("onPartialResponse: \"{}\"", formattedResponse);
        System.out.print(formattedResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        // 添加AI回复到历史记录
        history.add(completeResponse.aiMessage());
        sink.tryEmitComplete();
        
        // 完成响应Future
        String fullResponse = responseBuilder.toString();
        response.complete(fullResponse);
        
        System.out.println();
    }

    @Override
    public void onError(Throwable error) {
        sink.tryEmitError(error);
        response.completeExceptionally(error);
    }
    
    /**
     * 获取响应Mono（用于异步处理）
     */
    public Mono<String> getResponseMono() {
        return Mono.fromFuture(response);
    }
}
