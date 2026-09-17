package com.yomahub.liteflow.test.agent.real.conversationapi;

import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import io.agentscope.core.tool.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static com.yomahub.liteflow.test.agent.real.conversationapi.ConversationStoreProbe.USER;
import static com.yomahub.liteflow.test.agent.real.conversationapi.ConversationStoreProbe.AGENT;

/** Real model + real stores. Runs only in the explicit agent-live suite. */
class ConversationStoreLiveTest {
    @TempDir Path root;

    @AfterEach
    void clearConfiguration() {
        ConversationStoreProbe.closeContainer();
        LiteflowConfigGetter.clean();
    }

    @ParameterizedTest
    @EnumSource(AgentSessionStoreType.class)
    void memoryAndHistorySurviveAnActualJvmRestart(AgentSessionStoreType backend) throws Exception {
        var config = ConversationStoreProbe.configure(backend, root);
        String cid = UUID.randomUUID().toString();
        try (var conversations = AgentConversationService.open(config)) {
            runJvm(backend, cid, "write");
            runJvm(backend, cid, "read");
            assertEquals(4, conversations.messages(cid, 0, 20).items().size());
            assertTrue(conversations.get(cid).isEmpty());
            assertTrue(conversations.agentState(cid, AGENT).isEmpty());
            conversations.delete(cid);
            assertTrue(conversations.get(cid).isEmpty());
            assertTrue(conversations.agentState(cid, AGENT).isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(AgentSessionStoreType.class)
    void deletionWaitsForARealAgentToolAndDoesNotReviveItsHistory(AgentSessionStoreType backend) throws Exception {
        var config = ConversationStoreProbe.configure(backend, root);
        String cid = UUID.randomUUID().toString();
        BlockingTool tool = new BlockingTool();
        var pool = Executors.newFixedThreadPool(2);
        try (var component = new ConversationStoreProbe.LiveComponent(cid,
                "Call wait_for_release exactly once, then reply with the tool's output.", List.of(tool));
             var conversations = AgentConversationService.open(config)) {
            var running = pool.submit(() -> { component.process(); return null; });
            try {
                long toolDeadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
                while (!tool.started.await(100, TimeUnit.MILLISECONDS) && System.nanoTime() < toolDeadline) {
                    if (running.isDone()) {
                        running.get();
                        fail("Real model returned without calling the blocking tool");
                    }
                }
                assertEquals(0, tool.started.getCount(), "Real model must call the blocking tool");
                var deleting = pool.submit(() -> conversations.delete(cid));
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (conversations.get(cid).isPresent() && System.nanoTime() < deadline) {
                    Thread.sleep(20);
                }
                assertTrue(conversations.get(cid).isEmpty(), "Deletion marker must become visible");
                assertFalse(deleting.isDone(), "Deletion must wait for the active Agent state lease");
                tool.release.countDown();
                running.get(120, TimeUnit.SECONDS);
                deleting.get(120, TimeUnit.SECONDS);
                assertTrue(conversations.get(cid).isEmpty());
                assertTrue(conversations.agentState(cid, AGENT).isEmpty());
            } finally {
                tool.release.countDown();
            }
        } finally {
            tool.release.countDown();
            pool.shutdownNow();
        }
    }

    private void runJvm(AgentSessionStoreType backend, String cid, String mode) throws Exception {
        Path output = root.resolve(backend + "-" + mode + ".log");
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, ConversationStoreProbe.class.getName(), backend.name(), root.toString(), cid, mode)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(150, TimeUnit.SECONDS), "Child JVM timed out");
            String log = Files.readString(output);
            String key = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_API_KEY);
            if (!key.isEmpty()) log = log.replace(key, "<redacted>");
            assertEquals(0, child.exitValue(), log);
            assertTrue(log.contains("write".equals(mode) ? "CONVERSATION_WRITE_OK" : "CONVERSATION_RESTART_OK"));
        } finally {
            if (child.isAlive()) child.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }

    public static final class BlockingTool {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Tool(name = "wait_for_release", description = "Wait for the integration test and return its completion marker.")
        public String waitForRelease() throws InterruptedException {
            started.countDown();
            if (!release.await(90, TimeUnit.SECONDS)) throw new IllegalStateException("Tool release timed out");
            return "RELEASE-CHECK-COMPLETED";
        }
    }
}
