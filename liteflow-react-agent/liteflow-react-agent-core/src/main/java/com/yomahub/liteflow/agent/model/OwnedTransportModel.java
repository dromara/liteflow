package com.yomahub.liteflow.agent.model;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.model.transport.HttpTransport;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** A model facade that gives one explicitly owned AgentScope transport a deterministic owner. */
public final class OwnedTransportModel implements Model, AutoCloseable {

    private final Model delegate;
    private final HttpTransport ownedTransport;
    private final AtomicBoolean closed = new AtomicBoolean();

    public OwnedTransportModel(Model delegate, HttpTransport ownedTransport) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.ownedTransport = Objects.requireNonNull(ownedTransport, "ownedTransport");
    }

    /** Returns the exact AgentScope extension model without transferring transport ownership. */
    public Model delegate() {
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
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = null;
        if (delegate instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Throwable closeFailure) {
                failure = closeFailure;
            }
        }
        try {
            ownedTransport.close();
        } catch (Throwable closeFailure) {
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        }
        rethrow(failure);
    }

    /** Closes a resource acquired during build while keeping the build failure primary. */
    public static void closeAfterBuildFailure(
            HttpTransport ownedTransport, Throwable buildFailure) {
        if (ownedTransport == null) {
            return;
        }
        try {
            ownedTransport.close();
        } catch (Throwable closeFailure) {
            buildFailure.addSuppressed(closeFailure);
        }
    }

    private static void rethrow(Throwable failure) throws Exception {
        if (failure == null) {
            return;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof Exception exception) {
            throw exception;
        }
        throw new RuntimeException(failure);
    }
}
