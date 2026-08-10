package com.yomahub.liteflow.agent.testsupport;

import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic AgentScope model used by core runtime tests. */
public final class ScriptedChatModel implements Model {

    private final String responseText;
    private final CountDownLatch firstCallCancellation;
    private final Throwable immediateFailure;
    private final AtomicInteger callCount = new AtomicInteger();
    private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();
    private final List<RuntimeContext> runtimeContexts = new CopyOnWriteArrayList<>();

    public ScriptedChatModel(String responseText) {
        this(responseText, null, null);
    }

    private ScriptedChatModel(
            String responseText,
            CountDownLatch firstCallCancellation,
            Throwable immediateFailure) {
        this.responseText = responseText;
        this.firstCallCancellation = firstCallCancellation;
        this.immediateFailure = immediateFailure;
    }

    public static ScriptedChatModel neverThenReply(
            String responseText, CountDownLatch firstCallCancellation) {
        return new ScriptedChatModel(responseText, firstCallCancellation, null);
    }

    public static ScriptedChatModel immediateFailure(Throwable failure) {
        return new ScriptedChatModel(null, null, failure);
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        int invocation = callCount.incrementAndGet();
        inputs.add(List.copyOf(messages));
        return Flux.deferContextual(contextView -> {
            runtimeContexts.add(contextView.getOrDefault(AgentBase.RUNTIME_CONTEXT_KEY, null));
            if (invocation == 1 && firstCallCancellation != null) {
                return Flux.<ChatResponse>never()
                        .doOnCancel(firstCallCancellation::countDown);
            }
            if (immediateFailure != null) {
                return Flux.error(immediateFailure);
            }
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text(responseText).build()))
                    .finishReason("stop")
                    .build());
        });
    }

    @Override
    public String getModelName() {
        return "scripted-test-model";
    }

    public int callCount() {
        return callCount.get();
    }

    public List<Msg> inputAt(int index) {
        return inputs.get(index);
    }

    public RuntimeContext runtimeContextAt(int index) {
        return runtimeContexts.get(index);
    }
}
