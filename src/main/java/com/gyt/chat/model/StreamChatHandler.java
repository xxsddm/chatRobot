package com.gyt.chat.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.Sinks;

import java.util.List;

public record StreamChatHandler(List<ChatMessage> history, Sinks.Many<String> sink)
        implements StreamingChatResponseHandler {


    @Override
    public void onPartialResponse(String partialResponse) {
        // 确保换行符正确处理，保留Markdown格式
        String formattedResponse = partialResponse.replace("\r\n", "\n").replace("\r", "\n");
        sink.tryEmitNext(formattedResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        // 添加AI回复到历史记录
        history.add(completeResponse.aiMessage());
        sink.tryEmitComplete();
    }

    @Override
    public void onError(Throwable error) {
        sink.tryEmitError(error);
    }
}
