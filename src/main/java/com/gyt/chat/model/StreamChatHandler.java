package com.gyt.chat.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Stack;

@Slf4j
public class StreamChatHandler implements StreamingChatResponseHandler {

    private final Stack<CodeBlockModel> stk = new Stack<>();

    private final List<ChatMessage> history;

    private final Sinks.Many<String> sink;
    
    private final boolean enableThinking;

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
//        log.info("onPartialResponse: \"{}\"", formattedResponse);
        System.out.print(formattedResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        // 添加AI回复到历史记录
        history.add(completeResponse.aiMessage());
        sink.tryEmitComplete();
        System.out.println();
    }

    @Override
    public void onError(Throwable error) {
        sink.tryEmitError(error);
    }
}
