package com.yomahub.liteflow.test.agent.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yomahub.liteflow.agent.anthropic.AnthropicCompatible;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.Minimax;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Captures requests from the pinned SDKs using fixture credentials only. */
class MinimaxProtocolTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FLASH = "MiniMax-M3.1-Flash";
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private Model model;

    @AfterEach
    void close() throws Exception {
        try {
            if (model instanceof AutoCloseable closeable) closeable.close();
        } finally {
            if (server != null) server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "default, https://api.minimax.io/v1",
            "https://api.minimax.io/v1, https://api.minimax.io/v1",
            "https://api.minimaxi.com/v1, https://api.minimaxi.com/v1",
            "https://gateway.example/v1, https://gateway.example/v1"
    })
    void defaultAndConfiguredRegionsAreResolvedWithoutNetwork(String configured, String expected)
            throws Exception {
        AgentConfig config = new AgentConfig();
        config.getOpenaiCompatible().put("minimax", credential(
                "default".equals(configured) ? null : configured));
        model = Minimax.of().resolve(config);
        Field field = model.getClass().getDeclaredField("configuredOptions");
        field.setAccessible(true);
        GenerateOptions options = (GenerateOptions) field.get(model);
        assertEquals(expected, options.getBaseUrl());
        assertEquals(FLASH, model.getModelName());
        assertEquals(FLASH, Minimax.of().getModelName());
        assertEquals("MiniMax-M3", Minimax.of("MiniMax-M3").getModelName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "MiniMax-M3", "MiniMax-M2.7", "saved-custom-model"})
    void openAiRequestsPreserveDefaultAndExplicitModelIds(String selected) throws Exception {
        capture(false);
        AgentConfig config = new AgentConfig();
        config.getOpenaiCompatible().put("minimax", credential(baseUrl("/v1")));
        ModelSpec<?> spec = "default".equals(selected) ? Minimax.of() : Minimax.of(selected);
        model = spec.stream(false).maxTokens(128).resolve(config);
        invoke();
        JsonNode body = assertRequest("default".equals(selected) ? FLASH : selected,
                "/v1/chat/completions", "Bearer fixture-key");
        assertFalse(body.has("service_tier"));
        assertFalse(body.has("reasoning_split"));
        assertNotEquals("disabled", body.path("thinking").path("type").asText());
        assertFalse(body.has("reasoning_effort"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"low", "medium", "high", "xhigh", "max"})
    void flashEffortCrossesTheOpenAiRequestBoundary(String effort) throws Exception {
        capture(false);
        model = Minimax.of().apiKey("fixture-key").baseUrl(baseUrl("/v1"))
                .enableThinking(true).stream(false).maxTokens(128)
                .generateOptions(GenerateOptions.builder().reasoningEffort(effort).build())
                .resolve(new AgentConfig());
        invoke();
        JsonNode body = assertRequest(FLASH, "/v1/chat/completions", "Bearer fixture-key");
        assertEquals(effort, body.path("reasoning_effort").asText());
        assertEquals("adaptive", body.path("thinking").path("type").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MiniMax-M3.1-Flash", "MiniMax-M3", "MiniMax-M2.7", "saved-custom-model"})
    void anthropicRequestsPreserveExplicitModelsAndAdaptiveThinking(String selected) throws Exception {
        capture(true);
        AgentConfig config = new AgentConfig();
        config.getAnthropicCompatible().put("minimax", credential(baseUrl("/anthropic")));
        ModelSpec<?> spec = AnthropicCompatible.custom("minimax", selected).stream(false).maxTokens(128);
        if (FLASH.equals(selected)) {
            spec.additionalBodyParam("thinking", Map.of("type", "adaptive"))
                    .additionalBodyParam("output_config", Map.of("effort", "high"));
        }
        model = spec.resolve(config);
        invoke();
        JsonNode body = assertRequest(selected, "/anthropic/v1/messages", "fixture-key");
        assertFalse(body.has("service_tier"));
        if (FLASH.equals(selected)) {
            assertEquals("adaptive", body.path("thinking").path("type").asText());
            assertEquals("high", body.path("output_config").path("effort").asText());
        } else {
            assertFalse(body.has("thinking"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void m3DisabledThinkingSurvivesBothProtocols(boolean anthropic) throws Exception {
        capture(anthropic);
        ModelSpec<?> spec = anthropic
                ? AnthropicCompatible.custom("minimax", "MiniMax-M3")
                        .additionalBodyParam("thinking", Map.of("type", "disabled"))
                : Minimax.of("MiniMax-M3").enableThinking(false);
        model = spec.apiKey("fixture-key")
                .baseUrl(baseUrl(anthropic ? "/anthropic" : "/v1"))
                .stream(false).maxTokens(128).resolve(new AgentConfig());
        invoke();
        JsonNode body = assertRequest("MiniMax-M3",
                anthropic ? "/anthropic/v1/messages" : "/v1/chat/completions",
                anthropic ? "fixture-key" : "Bearer fixture-key");
        assertEquals("disabled", body.path("thinking").path("type").asText());
    }

    private void capture(boolean anthropic) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                requests.add(new Request(exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst(anthropic ? "x-api-key" : "Authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                String response = anthropic
                        ? """
                          {"id":"msg_fixture","type":"message","role":"assistant","model":"fixture",
                           "content":[{"type":"text","text":"fixture-reply"}],"stop_reason":"end_turn",
                           "usage":{"input_tokens":2,"output_tokens":3}}
                          """
                        : """
                          {"id":"fixture","object":"chat.completion","created":1,"model":"fixture",
                           "choices":[{"index":0,"message":{"role":"assistant","content":"fixture-reply"},
                           "finish_reason":"stop"}],"usage":{"prompt_tokens":2,"completion_tokens":3,"total_tokens":5}}
                          """;
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private void invoke() {
        ToolSchema tool = ToolSchema.builder().name("fixture_lookup").description("Fixture lookup")
                .parameters(Map.of("type", "object", "properties", Map.of())).build();
        List<ChatResponse> responses = model.stream(
                        List.of(Msg.builder().role(MsgRole.USER).textContent("request-marker").build()),
                        List.of(tool), GenerateOptions.builder().executionConfig(ExecutionConfig.builder()
                                .maxAttempts(1).timeout(Duration.ofSeconds(8)).build()).build())
                .collectList().block(Duration.ofSeconds(10));
        assertNotNull(responses);
        assertTrue(responses.stream().flatMap(response -> response.getContent().stream())
                .filter(TextBlock.class::isInstance).map(TextBlock.class::cast)
                .anyMatch(block -> "fixture-reply".equals(block.getText())));
    }

    private JsonNode assertRequest(String selected, String path, String authorization) throws Exception {
        assertEquals(1, requests.size());
        Request request = requests.get(0);
        assertEquals(path, request.path());
        assertEquals(authorization, request.authorization());
        JsonNode body = JSON.readTree(request.body());
        assertEquals(selected, body.path("model").asText());
        assertTrue(body.path("messages").toString().contains("request-marker"));
        assertTrue(body.path("tools").toString().contains("fixture_lookup"));
        return body;
    }

    private String baseUrl(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static PlatformCredential credential(String baseUrl) {
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey("fixture-key");
        credential.setBaseUrl(baseUrl);
        return credential;
    }

    private record Request(String path, String authorization, String body) {}
}
