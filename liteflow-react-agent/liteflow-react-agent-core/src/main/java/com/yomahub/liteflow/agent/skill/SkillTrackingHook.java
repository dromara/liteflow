package com.yomahub.liteflow.agent.skill;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks skills loaded by agentscope's skill-loading tool during a ReAct session.
 */
public class SkillTrackingHook implements Hook {

    public static final String LOAD_SKILL_TOOL_NAME = "load_skill_through_path";
    private static final String SKILL_ID_INPUT_KEY = "skillId";

    private final Map<String, String> skillIdToName;
    private final Set<String> usedSkills = Collections.synchronizedSet(new LinkedHashSet<>());

    public SkillTrackingHook(Map<String, String> skillIdToName) {
        this.skillIdToName = skillIdToName == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(skillIdToName));
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostActingEvent postActingEvent) {
            recordSkillLoad(postActingEvent.getToolUse(), postActingEvent.getToolResult());
        }
        return Mono.just(event);
    }

    public List<String> getUsedSkills() {
        synchronized (usedSkills) {
            return List.copyOf(usedSkills);
        }
    }

    public void clear() {
        usedSkills.clear();
    }

    private void recordSkillLoad(ToolUseBlock toolUse, ToolResultBlock toolResult) {
        if (toolUse == null || !LOAD_SKILL_TOOL_NAME.equals(toolUse.getName()) || isErrorResult(toolResult)) {
            return;
        }
        Map<String, Object> input = toolUse.getInput();
        if (input == null || !input.containsKey(SKILL_ID_INPUT_KEY)) {
            return;
        }
        Object skillId = input.get(SKILL_ID_INPUT_KEY);
        if (skillId == null) {
            return;
        }
        String skillName = skillIdToName.get(String.valueOf(skillId));
        if (skillName != null) {
            usedSkills.add(skillName);
        }
    }

    private boolean isErrorResult(ToolResultBlock toolResult) {
        if (toolResult == null || toolResult.getOutput() == null) {
            return false;
        }
        for (ContentBlock block : toolResult.getOutput()) {
            if (block instanceof TextBlock textBlock) {
                String text = textBlock.getText();
                if (text != null && text.startsWith("Error:")) {
                    return true;
                }
            }
        }
        return false;
    }
}
