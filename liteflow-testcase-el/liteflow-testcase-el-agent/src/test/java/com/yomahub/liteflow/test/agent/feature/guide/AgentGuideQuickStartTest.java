package com.yomahub.liteflow.test.agent.feature.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.test.agent.support.BaseAgentTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Guide 1 and 2.1: Spring + XML + real provider HTTP/SSE + JSON, with no external model. */
@SpringBootTest(classes = AgentGuideQuickStartTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.guide")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentGuideQuickStartTest extends BaseAgentTest {
    @TempDir static Path stateRoot;
    private static HttpServer server;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private static final CountDownLatch firstDeltaReceived = new CountDownLatch(1);
    private static final AtomicReference<Throwable> serverFailure = new AtomicReference<>();

    @BeforeAll static void startEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                assertEquals("Bearer local-guide-key", exchange.getRequestHeaders().getFirst("Authorization"));
                JsonNode request = JSON.readTree(exchange.getRequestBody());
                requests.add(request);
                assertTrue(request.path("stream").asBoolean());
                assertEquals("deepseek-flash", request.path("model").asText());
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
                exchange.sendResponseHeaders(200, 0);
                String first = "data: {\"id\":\"guide\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"deepseek-flash\","
                        + "\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"你好\"},\"finish_reason\":null}]}\n\n";
                exchange.getResponseBody().write(first.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                assertTrue(firstDeltaReceived.await(5, TimeUnit.SECONDS),
                        "the listener must receive a delta before the endpoint sends the final chunk");
                String end = "data: {\"id\":\"guide\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"deepseek-flash\","
                        + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"，LiteFlow\"},\"finish_reason\":null}]}\n\n"
                        + "data: {\"id\":\"guide\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"deepseek-flash\","
                        + "\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
                exchange.getResponseBody().write(end.getBytes(StandardCharsets.UTF_8));
            } catch (Throwable failure) { serverFailure.set(failure); }
            finally { exchange.close(); }
        });
        server.start();
    }

    @AfterAll static void closeEndpoint() { if (server != null) server.stop(0); }

    @DynamicPropertySource static void configuration(DynamicPropertyRegistry registry) {
        registry.add("spring.application.name", () -> "agent-guide-quickstart");
        registry.add("liteflow.rule-source", () -> "feature/guide/flow.el.xml");
        registry.add("liteflow.agent.openai-compatible.deepseek.api-key", () -> "local-guide-key");
        registry.add("liteflow.agent.openai-compatible.deepseek.base-url",
                () -> "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        registry.add("liteflow.agent.session-store.json-root", () -> stateRoot.toString());
        registry.add("liteflow.agent.execution-timeout", () -> "10s");
        registry.add("liteflow.agent.execution-log-enabled", () -> "false");
    }

    @Test void guideComponentStreamsBeforeCompletionPersistsHistoryAndContinuesOnlyTheSelectedConversation() throws Exception {
        var deltas = new CopyOnWriteArrayList<String>();
        var first = flowExecutor.execute2Resp("guideChatChain", "我叫小明", ExecuteOption.of().autoConversationId()
                .eventListener(event -> {
                    if ("agent.text.delta".equals(event.getType()) && event.getText() != null) {
                        deltas.add(event.getText());
                        firstDeltaReceived.countDown();
                    }
                }));
        assertNull(serverFailure.get(), () -> String.valueOf(serverFailure.get()));
        assertTrue(first.isSuccess(), () -> String.valueOf(first.getCause()));
        assertEquals("你好，LiteFlow", first.getSlot().getResponseData());
        assertEquals(first.getSlot().getResponseData(), String.join("", deltas));
        assertEquals("agent-guide-quickstart", liteflowConfig.getAgent().getApplicationName());
        assertNotNull(first.getConversationId());

        var second = flowExecutor.execute2Resp("guideChatChain", "我叫什么名字？",
                ExecuteOption.of().conversationId(first.getConversationId()));
        assertTrue(second.isSuccess(), () -> String.valueOf(second.getCause()));
        assertTrue(requests.get(1).path("messages").toString().contains("我叫小明"));
        assertTrue(requests.get(1).path("messages").toString().contains("你好，LiteFlow"));
        var separate = flowExecutor.execute2Resp("guideChatChain", "新的对话");
        assertTrue(separate.isSuccess(), () -> String.valueOf(separate.getCause()));
        assertNotEquals(first.getConversationId(), separate.getConversationId());
        assertFalse(requests.get(2).path("messages").toString().contains("我叫小明"));
        assertFalse(requests.get(0).path("tools").toString().contains("\"name\":\"execute\""));
        try (var history = AgentConversationService.open(liteflowConfig.getAgent())) {
            assertEquals(4, history.messages(first.getConversationId(), 0, 10).items().size());
            assertEquals(2, history.messages(separate.getConversationId(), 0, 10).items().size());
        }
        try (var files = Files.walk(stateRoot)) { assertTrue(files.anyMatch(Files::isRegularFile)); }
    }

    @org.springframework.stereotype.Component("guideChatAgent")
    static class ChatAgent extends HarnessAgentComponent {
        @Override protected ModelSpec<?> model() {
            return DeepSeek.of("deepseek-flash").stream(true).contextWindow(64000);
        }
        @Override protected String systemPrompt() { return "你是一个简洁的助手，用中文回答。"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) {
            Object input = getSlot().getChainReqData(getSlot().getChainId());
            return input == null ? "" : input.toString();
        }
        @Override protected boolean enableShellTool() { return false; }
    }
}
