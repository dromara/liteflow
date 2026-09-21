package com.yomahub.liteflow.test.agent.support;

import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Queue-driven AgentScope model for deterministic, network-free integration tests. */
public final class ScriptedChatModel implements Model {

    private final List<Step> steps;
    private final boolean nativeStructuredOutput;
    private final Consumer<List<Msg>> callObserver;
    private final AtomicInteger calls = new AtomicInteger();
    private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();
    private final List<RuntimeContext> contexts = new CopyOnWriteArrayList<>();

    private ScriptedChatModel(
            List<Step> steps, boolean nativeStructuredOutput, Consumer<List<Msg>> callObserver) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        this.steps = List.copyOf(steps);
        this.nativeStructuredOutput = nativeStructuredOutput;
        this.callObserver = callObserver;
    }

    public static Builder builder() {
        return new Builder(false);
    }

    public static Builder nativeStructured() {
        return new Builder(true);
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        int index = calls.getAndIncrement();
        inputs.add(List.copyOf(messages));
        callObserver.accept(List.copyOf(messages));
        return Flux.deferContextual(view -> {
            contexts.add(view.getOrDefault(AgentBase.RUNTIME_CONTEXT_KEY, null));
            Step step = steps.get(Math.min(index, steps.size() - 1));
            if (step.failure != null) {
                return Flux.error(step.failure);
            }
            if (step.requiredTool != null && tools.stream()
                    .noneMatch(schema -> step.requiredTool.equals(schema.getName()))) {
                return Flux.error(new AssertionError("missing tool " + step.requiredTool));
            }
            return Flux.just(ChatResponse.builder()
                    .content(step.content)
                    .usage(step.usage)
                    .finishReason(step.finishReason)
                    .build());
        });
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return nativeStructuredOutput;
    }

    @Override
    public String getModelName() {
        return "scripted-offline-model";
    }

    public int callCount() {
        return calls.get();
    }

    public List<Msg> inputAt(int index) {
        return inputs.get(index);
    }

    public RuntimeContext runtimeContextAt(int index) {
        return contexts.get(index);
    }

    private static final class Step {
        private final List<ContentBlock> content;
        private final String finishReason;
        private final String requiredTool;
        private final Throwable failure;
        private final ChatUsage usage;

        private Step(List<ContentBlock> content, String finishReason,
                     String requiredTool, Throwable failure, ChatUsage usage) {
            this.content = content;
            this.finishReason = finishReason;
            this.requiredTool = requiredTool;
            this.failure = failure;
            this.usage = usage;
        }
    }

    public static final class Builder {
        private final boolean nativeStructuredOutput;
        private final List<Step> steps = new ArrayList<>();
        private Consumer<List<Msg>> callObserver = ignored -> { };

        private Builder(boolean nativeStructuredOutput) {
            this.nativeStructuredOutput = nativeStructuredOutput;
        }

        public Builder reply(String text) {
            ContentBlock block = TextBlock.builder().text(text).build();
            steps.add(new Step(List.of(block), "stop", null, null, null));
            return this;
        }

        public Builder reply(String text, ChatUsage usage) {
            ContentBlock block = TextBlock.builder().text(text).build();
            steps.add(new Step(List.of(block), "stop", null, null,
                    Objects.requireNonNull(usage, "usage")));
            return this;
        }

        public Builder tool(ToolUseBlock tool) {
            Objects.requireNonNull(tool, "tool");
            steps.add(new Step(List.of(tool), "tool_calls", tool.getName(), null, null));
            return this;
        }

        public Builder fail(Throwable failure) {
            steps.add(new Step(List.of(), null, null,
                    Objects.requireNonNull(failure, "failure"), null));
            return this;
        }

        public Builder observeCalls(Consumer<List<Msg>> observer) {
            callObserver = Objects.requireNonNull(observer, "observer");
            return this;
        }

        public ScriptedChatModel build() {
            return new ScriptedChatModel(steps, nativeStructuredOutput, callObserver);
        }
    }
}
