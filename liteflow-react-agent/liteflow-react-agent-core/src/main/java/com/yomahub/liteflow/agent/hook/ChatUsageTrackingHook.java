package com.yomahub.liteflow.agent.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatUsage;
import reactor.core.publisher.Mono;

/**
 * 累加单次 {@code process()} 调用内所有 reasoning step 的 token 用量。
 *
 * <p>底层 agentscope 每次 {@code reasoning(iter)} 都新建一个 {@link io.agentscope.core.agent.accumulator.ReasoningContext
 * ReasoningContext}，因此 {@link PostReasoningEvent#getReasoningMessage()} 的 metadata 中携带的
 * {@link ChatUsage} 只是本步 LLM call 的累计（流式聚合），不是跨多步 ReAct 循环的累计。
 * 本 hook 在每次 PostReasoningEvent 触发时把当步 usage 累加到内部计数器，
 * 暴露整次调用累计后的 {@link #snapshot()}。
 *
 * <p>实例与缓存 ReActAgent 同生命周期；每次 {@code process()} 开始前必须调用 {@link #reset()}
 * 清零，避免上次调用的余量被带入。
 */
public class ChatUsageTrackingHook implements Hook {

    private int inputTokens;
    private int outputTokens;
    private double time;
    private int steps;

    @Override
    public synchronized <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostReasoningEvent e) {
            Msg msg = e.getReasoningMessage();
            if (msg != null) {
                ChatUsage usage = msg.getChatUsage();
                if (usage != null) {
                    inputTokens += usage.getInputTokens();
                    outputTokens += usage.getOutputTokens();
                    time += usage.getTime();
                    steps++;
                }
            }
        }
        return Mono.just(event);
    }

    public synchronized void reset() {
        this.inputTokens = 0;
        this.outputTokens = 0;
        this.time = 0;
        this.steps = 0;
    }

    /**
     * 返回到目前为止累计的 token 用量；若尚未观察到任何 usage，返回 {@code null}。
     */
    public synchronized ChatUsage snapshot() {
        if (steps == 0) {
            return null;
        }
        return ChatUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .time(time)
                .build();
    }

    /**
     * 已经累加过 usage 的 reasoning step 次数。
     */
    public synchronized int getSteps() {
        return steps;
    }
}
