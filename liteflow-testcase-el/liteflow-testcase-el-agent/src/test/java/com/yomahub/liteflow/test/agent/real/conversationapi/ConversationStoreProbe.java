package com.yomahub.liteflow.test.agent.real.conversationapi;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import com.yomahub.liteflow.spi.spring.SpringAware;
import org.springframework.context.support.StaticApplicationContext;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Invoked in separate JVMs by the live acceptance test; credentials never appear in arguments. */
public final class ConversationStoreProbe {
    static final String USER = "release-check-user";
    static final String AGENT = "releaseCheckAgent";
    private static StaticApplicationContext container;

    private ConversationStoreProbe() {
    }

    static AgentConfig configure(AgentSessionStoreType backend, Path root) {
        closeContainer();
        // The acceptance module includes the Spring starter, so bootstrap the real selected SPI.
        container = new StaticApplicationContext();
        container.refresh();
        new SpringAware().setApplicationContext(container);
        LiteflowConfig liteflow = new LiteflowConfig();
        AgentConfig config = new AgentConfig();
        config.getHarness().getLocal().setWorkspaceRoot(java.nio.file.Path.of("target", "harness-tests", java.util.UUID.randomUUID().toString()).toAbsolutePath().toString());
        config.getSessionStore().setJsonWorkspaceRoot(config.getHarness().getLocal().getWorkspaceRoot() + "/records");
        liteflow.setAgent(config);
        config.setApplicationName("release-conversation-" + backend);
        config.setExecutionTimeout(Duration.ofMinutes(2));
        config.setConversationHistoryEnabled(true);
        config.setExecutionLogEnabled(false);
        config.setMaxIterations(4);
        config.getSessionStore().setType(backend);
        config.getSessionStore().setJsonRoot(root.toString());
        config.getSessionStore().getRedis().setUri("redis://localhost:16379");
        config.getSessionStore().getRedis().setKeyPrefix("liteflow:release:conversation:");
        var mysql = config.getSessionStore().getMysql();
        mysql.setJdbcUrl("jdbc:mysql://localhost:13306/liteflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        mysql.setUsername("root");
        mysql.setPassword("root123456");
        mysql.setDatabaseName("liteflow");
        mysql.setTableName("conversation_release_check");
        mysql.setCreateIfNotExist(true);
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflow, "Conversation API acceptance");
        LiteflowConfigGetter.setLiteflowConfig(liteflow);
        return config;
    }

    static void closeContainer() {
        if (container != null) {
            container.close();
            container = null;
        }
    }

    public static void main(String[] args) throws Exception {
        AgentConfig config = configure(AgentSessionStoreType.valueOf(args[0]), Path.of(args[1]));
        String cid = args[2];
        boolean write = "write".equals(args[3]);
        String token = "TOKEN-" + cid;
        String prompt = write ? "Remember this exact token: " + token + ". Reply only OK."
                : "What is the exact token I asked you to remember earlier? Reply with only that token.";
        try (var component = new LiveComponent(cid, prompt, List.of());
             var conversations = AgentConversationService.open(config)) {
            component.process();
            Object responseData = component.getSlot().getResponseData();
            String reply = String.valueOf(responseData);
            if (!write && !reply.contains(token)) {
                throw new AssertionError("Agent did not restore its memory in the new JVM: " + reply);
            }
            int expected = write ? 2 : 4;
            var messages = conversations.messages(cid, 0, 20).items();
            if (messages.size() != expected || messages.get(expected - 1).content().isBlank()) {
                throw new AssertionError("Conversation history did not survive JVM restart");
            }
            if (conversations.agentState(cid, AGENT).isEmpty()) {
                throw new AssertionError("Persisted Agent state is missing");
            }
            System.out.println(write ? "CONVERSATION_WRITE_OK" : "CONVERSATION_RESTART_OK");
        } finally {
            closeContainer();
        }
    }

    static final class LiveComponent extends HarnessAgentComponent {
        private final Slot slot = new Slot();
        private final String prompt;
        private final List<Object> tools;

        LiveComponent(String cid, String prompt, List<Object> tools) {
            this.prompt = prompt;
            this.tools = tools;
            setNodeId(AGENT);
            slot.setChainId("releaseConversationChain");
            slot.setConversationId(cid);
            slot.putRequestId(UUID.randomUUID().toString());
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { return LiveTestSupport.compatibleCustomModel(); }
        @Override protected String systemPrompt() { return "Follow the user's instructions exactly."; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return prompt; }
        @Override protected List<Object> tools() { return tools; }
    }
}
