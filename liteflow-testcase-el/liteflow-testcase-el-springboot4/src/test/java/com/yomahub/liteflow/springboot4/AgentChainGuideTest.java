package com.yomahub.liteflow.springboot4;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Guide 1.1: Boot 4's starter must actually discover and execute an Agent, not only bind properties. */
@SpringBootTest(classes = {AgentChainGuideTest.class, AgentChainGuideTest.ChatAgent.class})
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentChainGuideTest {
    @TempDir static Path root;
    @Autowired FlowExecutor executor;
    @Autowired LiteflowConfig config;
    @Autowired ChatAgent agent;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.application.name", () -> "boot4-agent-guide");
        registry.add("liteflow.rule-source", () -> "agent-guide/flow.el.xml");
        registry.add("liteflow.agent.session-store.json-root", () -> root.toString());
        registry.add("liteflow.agent.execution-log-enabled", () -> "false");
    }

    @Test void boot4StarterDiscoversTheComponentAndContinuesTheSameConversation() {
        var first = executor.execute2Resp("boot4GuideChain", "first", ExecuteOption.of().autoConversationId());
        assertTrue(first.isSuccess(), () -> String.valueOf(first.getCause()));
        assertEquals("boot4-reply", first.getSlot().getResponseData());
        assertEquals("boot4-agent-guide", config.getAgent().getApplicationName());
        var second = executor.execute2Resp("boot4GuideChain", "second", ExecuteOption.of().conversationId(first.getConversationId()));
        assertTrue(second.isSuccess(), () -> String.valueOf(second.getCause()));
        assertEquals(first.getConversationId(), second.getConversationId());
        assertEquals(2, agent.inputs.size());
        assertTrue(agent.inputs.get(1).stream().anyMatch(message -> "first".equals(message.getTextContent())));
        assertTrue(agent.inputs.get(1).stream().anyMatch(message -> "boot4-reply".equals(message.getTextContent())));
    }

    @Component("boot4GuideAgent")
    static class ChatAgent extends HarnessAgentComponent {
        final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();
        @Override protected ModelSpec<?> model() { throw new AssertionError("deterministic model"); }
        @Override protected boolean enableShellTool() { return false; }
        @Override protected String systemPrompt() { return "Reply briefly"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) {
            Object input = getSlot().getChainReqData(getSlot().getChainId());
            return input == null ? "" : input.toString();
        }
        @Override protected Model buildModel() {
            return new Model() {
                @Override public String getModelName() { return "boot4-guide"; }
                @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                    inputs.add(List.copyOf(messages));
                    return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder()
                            .text("boot4-reply").build())).finishReason("stop").build());
                }
            };
        }
    }
}
