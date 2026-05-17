package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentComponentTest {

    @Test
    void effectiveSystemPromptPrependsFrameworkPromptAndAppendsComponentPrompt() {
        TestAgentComponent component = new TestAgentComponent();

        String prompt = component.effectiveSystemPrompt();

        assertTrue(prompt.contains("使用用户提问所用的语言"));
        assertTrue(prompt.contains("每次调用工具前"));
        assertTrue(prompt.contains("不要展开隐藏思维链"));
        assertTrue(prompt.contains("custom component instruction"));
        assertTrue(prompt.indexOf("使用用户提问所用的语言")
                < prompt.indexOf("custom component instruction"));
    }

    @Test
    void defaultHandleReplyIgnoresNullReplyInsteadOfWritingNullToSlot() {
        Slot slot = new Slot(List.of());
        TestAgentComponent component = new TestAgentComponent(slot);
        component.setNodeId("testAgent");
        slot.setAttachment("_react_agent_ctx_testAgent",
                new ReActAgentContext(slot, "cid", "testAgent", Path.of("target/test-agent")));

        assertDoesNotThrow(() -> component.handleReplyForTest(null));
        assertNull(slot.getResponseData());
    }

    private static class TestAgentComponent extends ReActAgentComponent {

        private final Slot slot;

        private TestAgentComponent() {
            this(new Slot(List.of()));
        }

        private TestAgentComponent(Slot slot) {
            this.slot = slot;
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected ModelSpec<?> model() {
            return null;
        }

        @Override
        protected String systemPrompt() {
            return "custom component instruction";
        }

        @Override
        protected String userPrompt() {
            return "hello";
        }

        void handleReplyForTest(io.agentscope.core.message.Msg reply) {
            handleReply(reply);
        }
    }
}
