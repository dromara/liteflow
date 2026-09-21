package com.yomahub.liteflow.agent.harness.compaction;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.catalog.ModelContextResolver;
import com.yomahub.liteflow.agent.model.catalog.ModelLimits;
import com.yomahub.liteflow.agent.model.catalog.ModelMetadata;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.harness.agent.memory.MemoryFlushManager;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ConversationCompactor;
import io.agentscope.harness.agent.middleware.HarnessRuntimeMiddleware;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/** Applies the percentage policy at the actual model call, after routing and again on fallback. */
public final class AdaptiveCompactionMiddleware implements HarnessRuntimeMiddleware {
    private final Supplier<WorkspaceManager> workspace;
    private final Model primary;
    private final Model fallback;
    private final Set<Model> managed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ModelContextResolver contexts;
    private final double threshold;
    private final double fallbackThreshold;

    public AdaptiveCompactionMiddleware(Supplier<WorkspaceManager> workspace, Model primary,
            Model fallback, List<Model> models, double threshold, int fallbackWindow, double fallbackThreshold) {
        if (!Double.isFinite(threshold) || threshold <= 0 || threshold >= 1
                || !Double.isFinite(fallbackThreshold) || fallbackThreshold <= 0 || fallbackThreshold >= 1) {
            throw new AgentConfigException("compaction-threshold must be between 0 and 1 (exclusive)");
        }
        this.workspace = workspace;
        this.primary = primary;
        this.fallback = fallback;
        this.managed.addAll(models);
        this.contexts = new ModelContextResolver(models, fallbackWindow);
        this.threshold = threshold;
        this.fallbackThreshold = fallbackThreshold;
        RequestTokenEstimator.initialize();
    }

    @Override public int order() { return 0; }

    @Override
    public Flux<AgentEvent> onModelCall(Agent agent, RuntimeContext context, ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        // AgentScope 2.0.3 wraps the primary in an opaque Model when fallback is configured.
        // Reproduce its switch-on-first-error behavior inside the guard, so the fallback request
        // gets its own limits/options and compaction rather than reusing the primary's budget.
        boolean automaticFallback = !managed.contains(input.model()) && fallback != null
                && primary.getModelName().equals(input.model().getModelName());
        Model selected = automaticFallback ? primary : input.model();
        if (!managed.contains(selected)) {
            return Flux.error(new AgentConfigException("Compaction cannot resolve an unmanaged model"));
        }
        AtomicReference<Model> active = new AtomicReference<>(selected);
        Model guarded = new Model() {
            @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                return Flux.defer(() -> {
                    active.set(selected);
                    AtomicReference<List<Msg>> preparedMessages = new AtomicReference<>(messages);
                    Flux<ChatResponse> result = prepare(agent, context, selected, messages, tools, options)
                            .flatMapMany(request -> {
                                preparedMessages.set(request.messages());
                                return selected.stream(request.messages(), request.tools(), request.options());
                            });
                    if (!automaticFallback) return result;
                    return result.switchOnFirst((signal, flux) -> {
                        if (!signal.isOnError()) return flux;
                        active.set(fallback);
                        return prepare(agent, context, fallback, preparedMessages.get(), tools, options)
                                .flatMapMany(request -> fallback.stream(request.messages(), request.tools(), request.options()));
                    });
                });
            }
            @Override public String getModelName() { return active.get().getModelName(); }
            @Override public boolean supportsNativeStructuredOutput() { return active.get().supportsNativeStructuredOutput(); }
            @Override public boolean supportsNativeStructuredOutputWithTools() { return active.get().supportsNativeStructuredOutputWithTools(); }
            @Override public int getContextWindowSize() {
                ModelLimits limits = contexts.limits(active.get());
                return limits.context() != null ? limits.context() : limits.input();
            }
        };
        return next.apply(new ModelCallInput(input.messages(), input.tools(), input.options(), guarded));
    }

    private Mono<ModelCallInput> prepare(Agent agent, RuntimeContext runtime, Model model,
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        return Mono.defer(() -> {
            if (options != null && options.getModelName() != null
                    && !options.getModelName().equals(model.getModelName())) {
                return Mono.error(new AgentConfigException("Select a managed model with routeModel; "
                        + "per-request modelName overrides have no prepared context limits"));
            }
            ModelContextResolver.Context resolved = contexts.context(model);
            ModelLimits limits = resolved.limits();
            Budget budget = budget(limits, options, contexts.outputBudget(model));
            GenerateOptions effectiveOptions = outputOptions(model, options, budget.output());
            long estimate = RequestTokenEstimator.estimate(messages, tools, effectiveOptions);
            double ratio = resolved.fallback() ? fallbackThreshold : threshold;
            long trigger = Math.max(1, (long) (budget.input() * ratio));
            if (estimate < trigger) return Mono.just(new ModelCallInput(messages, tools, effectiveOptions, model));
            if (!(agent instanceof ReActAgent react)) {
                return Mono.error(new AgentConfigException("Adaptive compaction requires a ReActAgent"));
            }
            List<Msg> system = messages.stream().filter(msg -> msg.getRole() == MsgRole.SYSTEM).toList();
            List<Msg> conversation = messages.stream().filter(msg -> msg.getRole() != MsgRole.SYSTEM).toList();
            long fixed = RequestTokenEstimator.estimate(system, tools, effectiveOptions);
            if (fixed >= trigger) return Mono.error(tooLarge("System prompt and tool definitions exhaust the input budget"));
            int keep = (int) Math.max(1, Math.min(8000, (trigger - fixed) / 4));
            AtomicReference<Throwable> summaryFailure = new AtomicReference<>();
            Model summaryModel = summaryModel(model, budget, summaryFailure);
            CompactionConfig config = CompactionConfig.builder()
                    .triggerMessages(0).triggerTokens(1).keepTokens(0)
                    .keepMessages(tailMessages(conversation, keep))
                    .flushBeforeCompact(false).offloadBeforeCompact(true).prune(null).build();
            ConversationCompactor compactor = new ConversationCompactor(summaryModel,
                    new MemoryFlushManager(workspace.get(), summaryModel));
            String session = runtime == null || runtime.getSessionId() == null ? "default" : runtime.getSessionId();
            return compactor.compactIfNeeded(runtime, conversation, config, agent.getName(), session)
                    .flatMap(compacted -> {
                        if (summaryFailure.get() != null) return Mono.error(summaryFailure.get());
                        if (compacted.isEmpty()) {
                            if (estimate > budget.input()) return Mono.error(tooLarge("No history can be safely compacted"));
                            return Mono.just(new ModelCallInput(messages, tools, effectiveOptions, model));
                        }
                        List<Msg> updated = new ArrayList<>(system);
                        updated.addAll(compacted.get());
                        long after = RequestTokenEstimator.estimate(updated, tools, effectiveOptions);
                        if (after >= trigger) return Mono.error(tooLarge("Compacted history still exceeds the trigger budget"));
                        var state = RuntimeContext.resolveAgentState(runtime, react);
                        if (state == null) return Mono.error(new AgentConfigException("Missing agent state for compaction"));
                        state.contextMutable().clear();
                        state.contextMutable().addAll(compacted.get());
                        return Mono.just(new ModelCallInput(updated, tools, effectiveOptions, model));
                    });
        });
    }

    private static int tailMessages(List<Msg> messages, int budget) {
        int kept = 0;
        long tokens = 0;
        for (int index = messages.size() - 1; index >= 0; index--) {
            long next = RequestTokenEstimator.estimate(List.of(messages.get(index)), List.of(), null);
            if (kept > 0 && tokens + next > budget) break;
            tokens += next;
            kept++;
        }
        // ConversationCompactor subsequently adjusts this boundary to keep tool pairs intact.
        return Math.max(1, kept);
    }

    private Model summaryModel(Model delegate, Budget budget, AtomicReference<Throwable> failure) {
        return new Model() {
            @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                return Flux.defer(() -> {
                    int output = Math.max(1, Math.min(budget.output(), Math.min(2048, budget.input() / 8)));
                    GenerateOptions bounded = outputOptions(delegate, null, output);
                    if (RequestTokenEstimator.estimate(messages, tools, bounded) > budget.input()) {
                        return Flux.error(tooLarge("Summary input exceeds the model budget; reduce the oversized input/tool result"));
                    }
                    return delegate.stream(messages, tools, bounded);
                }).doOnError(failure::set);
            }
            @Override public String getModelName() { return delegate.getModelName(); }
        };
    }

    static Budget budget(ModelLimits limits, GenerateOptions options, Integer configuredOutput) {
        int capacity = limits.context() != null ? limits.context() : limits.input();
        Integer requested = options == null ? null : options.getMaxCompletionTokens();
        if (requested == null && options != null) requested = options.getMaxTokens();
        if (requested == null) requested = configuredOutput;
        int output = requested != null ? requested : Math.max(1, Math.min(8192, capacity / 10));
        if (output <= 0 || (limits.output() != null && output > limits.output())) {
            if (requested != null) throw new AgentConfigException("Configured output budget exceeds model limits");
            output = limits.output();
        }
        long input = limits.input() != null ? limits.input() : capacity;
        if (limits.context() != null) input = Math.min(input, (long) limits.context() - output);
        // Leave room for formatter overhead and local token estimation error.
        input -= Math.max(32, input / 10);
        if (input <= 0) throw new AgentConfigException("Output reservation leaves no usable model input context");
        return new Budget((int) input, output);
    }

    private GenerateOptions outputOptions(Model model, GenerateOptions original, int output) {
        ModelMetadata metadata = ModelMetadata.of(model);
        boolean completion = original != null && original.getMaxCompletionTokens() != null;
        if (original == null || (original.getMaxTokens() == null && original.getMaxCompletionTokens() == null)) {
            completion = metadata != null && (metadata.completionTokens() != null
                    ? metadata.completionTokens() : "openai".equals(metadata.provider()));
        }
        GenerateOptions cap = completion ? GenerateOptions.builder().maxCompletionTokens(output).build()
                : GenerateOptions.builder().maxTokens(output).build();
        return GenerateOptions.mergeOptions(cap, original);
    }

    private static AgentConfigException tooLarge(String detail) {
        return new AgentConfigException("Cannot fit model context: " + detail);
    }

    record Budget(int input, int output) { }
}
