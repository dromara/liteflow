package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Tracks successful calls to AgentScope's real dynamic skill-loading tool. */
public final class SkillTrackingMiddleware implements MiddlewareBase {

    public static final String LOAD_SKILL_TOOL_NAME = "load_skill_through_path";
    private static final String SKILL_ID_INPUT_KEY = "skillId";

    private final Map<String, String> skillIdToName;

    public SkillTrackingMiddleware(Map<String, String> skillIdToName) {
        this.skillIdToName = skillIdToName == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(skillIdToName));
    }

    @Override
    public int order() {
        return AgentMiddlewareOrder.USAGE_SKILL;
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext runtimeContext,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            LiteFlowAgentContext context = requireContext(runtimeContext);
            Map<String, String> pendingSkills = findPendingSkills(input);
            return next.apply(input).doOnNext(event -> {
                if (event instanceof ToolResultEndEvent end
                        && end.getState() == ToolResultState.SUCCESS
                        && LOAD_SKILL_TOOL_NAME.equals(end.getToolCallName())) {
                    String skill = pendingSkills.get(end.getToolCallId());
                    if (skill != null) {
                        context.recordUsedSkill(skill);
                    }
                }
            });
        });
    }

    private Map<String, String> findPendingSkills(ActingInput input) {
        Map<String, String> pending = new LinkedHashMap<>();
        if (input == null || input.toolCalls() == null) {
            return pending;
        }
        for (ToolUseBlock toolCall : input.toolCalls()) {
            if (toolCall == null
                    || toolCall.getId() == null
                    || !LOAD_SKILL_TOOL_NAME.equals(toolCall.getName())) {
                continue;
            }
            Object skillId = toolCall.getInput().get(SKILL_ID_INPUT_KEY);
            if (skillId == null || skillId.toString().isBlank()) {
                continue;
            }
            String id = skillId.toString();
            pending.put(toolCall.getId(), skillIdToName.getOrDefault(id, id));
        }
        return pending;
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for skill tracking");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException("LiteFlowAgentContext is required for skill tracking");
        }
        return context;
    }
}
