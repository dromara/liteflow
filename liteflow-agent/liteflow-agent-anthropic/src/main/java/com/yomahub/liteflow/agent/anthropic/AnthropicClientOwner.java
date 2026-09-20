package com.yomahub.liteflow.agent.anthropic;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Makes one extension-owned Anthropic SDK client visible to LiteFlow runtime ownership. */
public final class AnthropicClientOwner implements Model, AutoCloseable {

    private final AnthropicChatModel delegate;
    private final AtomicBoolean closed = new AtomicBoolean();

    private AnthropicClientOwner(AnthropicChatModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    static AnthropicClientOwner own(AnthropicChatModel delegate) {
        AnthropicClientBridge.verifyContract();
        return new AnthropicClientOwner(delegate);
    }

    /** Returns the exact AgentScope extension model without transferring client ownership. */
    public AnthropicChatModel delegate() {
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
            AnthropicClientBridge.close(delegate);
        }
    }
}
