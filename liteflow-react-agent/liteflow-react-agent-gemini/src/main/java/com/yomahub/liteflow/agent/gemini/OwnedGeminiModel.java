package com.yomahub.liteflow.agent.gemini;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runtime-owned facade for AgentScope's closeable-but-not-AutoCloseable Gemini model. */
final class OwnedGeminiModel implements Model, AutoCloseable {

    private final GeminiChatModel delegate;
    private final AtomicBoolean closed = new AtomicBoolean();

    OwnedGeminiModel(GeminiChatModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    GeminiChatModel delegate() {
        return delegate;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        return delegate.stream(messages, tools, options);
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return delegate.supportsNativeStructuredOutput();
    }

    @Override
    public boolean supportsNativeStructuredOutputWithTools() {
        return delegate.supportsNativeStructuredOutputWithTools();
    }

    @Override
    public int getContextWindowSize() {
        return delegate.getContextWindowSize();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            delegate.close();
        }
    }
}
