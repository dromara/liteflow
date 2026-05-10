package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.agent.skill.SkillTrackingHook;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReActAgentSkillTrackingHookTest {

    private static final Agent TEST_AGENT = new Agent() {
        @Override
        public String getAgentId() {
            return "test-agent-id";
        }

        @Override
        public String getName() {
            return "test-agent";
        }

        @Override
        public void interrupt() {
        }

        @Override
        public void interrupt(Msg msg) {
        }

        @Override
        public Mono<Msg> call(List<Msg> messages) {
            return Mono.empty();
        }

        @Override
        public Mono<Msg> call(List<Msg> messages, Class<?> responseType) {
            return Mono.empty();
        }

        @Override
        public Mono<Msg> call(List<Msg> messages, JsonNode schema) {
            return Mono.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> messages, StreamOptions options) {
            return Flux.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> messages, StreamOptions options, Class<?> responseType) {
            return Flux.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> messages, StreamOptions options, JsonNode schema) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> observe(Msg msg) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> observe(List<Msg> messages) {
            return Mono.empty();
        }
    };

    @Test
    public void testTracksLoadSkillToolUseByMappedSkillName() {
        SkillTrackingHook hook = new SkillTrackingHook(new LinkedHashMap<>(Map.of("skill-1", "demo")));

        hook.onEvent(postActingEvent(SkillTrackingHook.LOAD_SKILL_TOOL_NAME, Map.of("skillId", "skill-1"))).block();

        Assertions.assertEquals(List.of("demo"), hook.getUsedSkills());
    }

    @Test
    public void testDeduplicatesAndClearsUsedSkills() {
        Map<String, String> skillIdToName = new LinkedHashMap<>();
        skillIdToName.put("skill-1", "demo");
        skillIdToName.put("skill-2", "research");
        SkillTrackingHook hook = new SkillTrackingHook(skillIdToName);

        hook.onEvent(postActingEvent(SkillTrackingHook.LOAD_SKILL_TOOL_NAME, Map.of("skillId", "skill-1"))).block();
        hook.onEvent(postActingEvent(SkillTrackingHook.LOAD_SKILL_TOOL_NAME, Map.of("skillId", "skill-1"))).block();
        hook.onEvent(postActingEvent(SkillTrackingHook.LOAD_SKILL_TOOL_NAME, Map.of("skillId", "skill-2"))).block();

        Assertions.assertEquals(List.of("demo", "research"), hook.getUsedSkills());

        hook.clear();

        Assertions.assertEquals(List.of(), hook.getUsedSkills());
    }

    @Test
    public void testIgnoresNonSkillTools() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-1", "demo"));

        hook.onEvent(postActingEvent("search", Map.of("skillId", "skill-1"))).block();

        Assertions.assertEquals(List.of(), hook.getUsedSkills());
    }

    @Test
    public void testIgnoresUnknownSkillId() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-1", "demo"));

        Assertions.assertDoesNotThrow(() ->
                hook.onEvent(postActingEvent(SkillTrackingHook.LOAD_SKILL_TOOL_NAME, Map.of("skillId", "unknown-skill"))).block());

        Assertions.assertEquals(List.of(), hook.getUsedSkills());
    }

    @Test
    public void testIgnoresErrorResultForKnownSkillId() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-1", "demo"));

        hook.onEvent(postActingEvent(
                SkillTrackingHook.LOAD_SKILL_TOOL_NAME,
                Map.of("skillId", "skill-1"),
                ToolResultBlock.error("failed to load skill"))).block();

        Assertions.assertEquals(List.of(), hook.getUsedSkills());
    }

    private static PostActingEvent postActingEvent(String toolName, Map<String, Object> input) {
        return postActingEvent(toolName, input, null);
    }

    private static PostActingEvent postActingEvent(
            String toolName, Map<String, Object> input, ToolResultBlock toolResult) {
        ToolUseBlock toolUse = new ToolUseBlock("tool-call-1", toolName, input);
        return new PostActingEvent(TEST_AGENT, new Toolkit(), toolUse, toolResult);
    }
}
