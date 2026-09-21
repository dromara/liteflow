package com.yomahub.liteflow.test.agent.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.anthropic.AnthropicCompatible;
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.*;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Exercise each real SDK at its HTTP boundary; all responses and credentials belong to this fixture. */
class ProviderProtocolTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String KEY = "mock-provider-key";
    private static final String MODEL = "mock-model";
    private static final List<Msg> MESSAGES = List.of(
            Msg.builder().role(MsgRole.USER).textContent("request-marker").build());
    private static final ToolSchema TOOL = ToolSchema.builder().name("lookup")
            .description("Look up a fixture value")
            .parameters(Map.of("type", "object", "properties", Map.of("city", Map.of("type", "string")),
                    "required", List.of("city"))).build();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private Model model;

    enum Provider { OPENAI, COMPATIBLE, DEEPSEEK, KIMI, GLM, MINIMAX, ANTHROPIC, ANTHROPIC_COMPATIBLE, GEMINI, DASHSCOPE }

    @AfterEach
    void close() throws Exception {
        try {
            if (model instanceof AutoCloseable closeable) closeable.close();
        } finally {
            if (server != null) server.stop(0);
        }
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    void textAndUsageAreDecodedFromTheMockEndpoint(Provider provider) throws Exception {
        open(provider, false, 200, response(provider, false));
        List<ChatResponse> replies = call(List.of());
        assertEquals("mock-reply", text(replies));
        ChatUsage usage = replies.stream().map(ChatResponse::getUsage).filter(java.util.Objects::nonNull)
                .reduce((first, last) -> last).orElseThrow();
        assertEquals(11, usage.getInputTokens());
        assertEquals(3, usage.getOutputTokens());
        assertRequest(provider);
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    void toolSchemaAndArgumentsCrossTheProviderBoundary(Provider provider) throws Exception {
        open(provider, false, 200, response(provider, true));
        List<ToolUseBlock> calls = call(List.of(TOOL)).stream().flatMap(r -> r.getContent().stream())
                .filter(ToolUseBlock.class::isInstance).map(ToolUseBlock.class::cast).toList();
        assertEquals(1, calls.size());
        assertEquals("lookup", calls.get(0).getName());
        if (provider == Provider.DASHSCOPE) {
            // DashScope exposes raw argument fragments; the Agent runtime assembles them.
            assertEquals("Shanghai", JSON.readTree(calls.get(0).getContent()).path("city").asText());
        } else {
            assertEquals("Shanghai", calls.get(0).getInput().get("city"));
        }
        assertRequest(provider);
        JsonNode body = JSON.readTree(requests.get(0).body());
        JsonNode schemas = provider == Provider.DASHSCOPE ? body.path("parameters").path("tools") : body.path("tools");
        assertTrue(schemas.toString().contains("lookup"), body::toString);
        assertTrue(schemas.toString().contains("city"), body::toString);
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    void streamingTextIsDecodedFromMockSse(Provider provider) throws Exception {
        open(provider, true, 200, streamResponse(provider));
        assertEquals("mock-reply", text(call(List.of())));
        assertRequest(provider);
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    void authenticationFailureIsPropagatedWithoutARealCredential(Provider provider) throws Exception {
        open(provider, false, 401, """
                {"error":{"code":401,"type":"authentication_error","message":"mock-auth-denied","status":"UNAUTHENTICATED"},
                 "code":"InvalidApiKey","message":"mock-auth-denied"}
                """);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> call(List.of()));
        assertTrue(causes(failure).contains(provider == Provider.DASHSCOPE ? "401" : "mock-auth-denied"),
                () -> causes(failure));
        assertRequest(provider);
    }

    private void open(Provider provider, boolean stream, int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                requests.add(new Request(exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("x-api-key"),
                        exchange.getRequestHeaders().getFirst("x-goog-api-key"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", stream ? "text/event-stream" : "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        ModelSpec<?> spec = switch (provider) {
            case OPENAI -> OpenAI.of(MODEL);
            case COMPATIBLE -> OpenAICompatible.custom("mock", MODEL);
            case DEEPSEEK -> DeepSeek.of(MODEL);
            case KIMI -> Kimi.of(MODEL);
            case GLM -> GLM.of(MODEL);
            case MINIMAX -> Minimax.of(MODEL);
            case ANTHROPIC -> Anthropic.of(MODEL);
            case ANTHROPIC_COMPATIBLE -> AnthropicCompatible.custom("mock", MODEL);
            case GEMINI -> Gemini.of(MODEL);
            case DASHSCOPE -> DashScope.of(MODEL);
        };
        model = spec.apiKey(KEY).baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .stream(stream).temperature(0.25).maxTokens(64).resolve(new AgentConfig());
    }

    private List<ChatResponse> call(List<ToolSchema> tools) {
        return model.stream(MESSAGES, tools, GenerateOptions.builder().build())
                .collectList().block(Duration.ofSeconds(8));
    }

    private void assertRequest(Provider provider) throws Exception {
        assertEquals(1, requests.size(), "exactly one request must reach the local mock");
        Request request = requests.get(0);
        JsonNode body = JSON.readTree(request.body());
        assertTrue(request.body().contains("request-marker"));
        switch (provider) {
            case ANTHROPIC, ANTHROPIC_COMPATIBLE -> {
                assertEquals(KEY, request.anthropicKey());
                assertEquals("/v1/messages", request.path());
                assertEquals(64, body.path("max_tokens").asInt());
            }
            case GEMINI -> {
                assertEquals(KEY, request.googleKey());
                assertTrue(request.path().contains("models/" + MODEL + ":"), request.path());
                assertEquals(64, body.path("generationConfig").path("maxOutputTokens").asInt());
            }
            default -> {
                assertEquals("Bearer " + KEY, request.authorization());
                assertEquals(provider == Provider.DASHSCOPE
                        ? "/api/v1/services/aigc/text-generation/generation" : "/v1/chat/completions", request.path());
                JsonNode options = provider == Provider.DASHSCOPE ? body.path("parameters") : body;
                assertEquals(0.25, options.path("temperature").asDouble());
            }
        }
        if (provider != Provider.GEMINI) assertEquals(MODEL, body.path("model").asText());
    }

    private static String text(List<ChatResponse> responses) {
        return responses.stream().flatMap(r -> r.getContent().stream()).filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast).map(TextBlock::getText).collect(java.util.stream.Collectors.joining());
    }

    private static String causes(Throwable failure) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) text.append(current.getMessage());
        return text.toString();
    }

    private static String response(Provider provider, boolean tool) {
        String message = tool
                ? "{\"role\":\"assistant\",\"content\":\"\",\"tool_calls\":[{\"id\":\"call_mock\",\"type\":\"function\",\"function\":{\"name\":\"lookup\",\"arguments\":\"{\\\"city\\\":\\\"Shanghai\\\"}\"}}]}"
                : "{\"role\":\"assistant\",\"content\":\"mock-reply\"}";
        return switch (provider) {
            case ANTHROPIC, ANTHROPIC_COMPATIBLE -> """
                    {"id":"msg_mock","type":"message","role":"assistant","model":"mock-model",
                     "content":[%s],"stop_reason":"%s","stop_sequence":null,"usage":{"input_tokens":11,"output_tokens":3}}
                    """.formatted(tool ? "{\"type\":\"tool_use\",\"id\":\"call_mock\",\"name\":\"lookup\",\"input\":{\"city\":\"Shanghai\"}}"
                            : "{\"type\":\"text\",\"text\":\"mock-reply\"}", tool ? "tool_use" : "end_turn");
            case GEMINI -> """
                    {"candidates":[{"content":{"role":"model","parts":[%s]},"finishReason":"STOP","index":0}],
                     "usageMetadata":{"promptTokenCount":11,"candidatesTokenCount":3,"totalTokenCount":14},"modelVersion":"mock-model"}
                    """.formatted(tool ? "{\"functionCall\":{\"name\":\"lookup\",\"args\":{\"city\":\"Shanghai\"}}}"
                            : "{\"text\":\"mock-reply\"}");
            case DASHSCOPE -> """
                    {"request_id":"mock","output":{"choices":[{"message":%s,"finish_reason":"%s"}]},
                     "usage":{"input_tokens":11,"output_tokens":3,"total_tokens":14}}
                    """.formatted(message, tool ? "tool_calls" : "stop");
            default -> """
                    {"id":"mock","object":"chat.completion","created":1,"model":"mock-model",
                     "choices":[{"index":0,"message":%s,"finish_reason":"%s"}],
                     "usage":{"prompt_tokens":11,"completion_tokens":3,"total_tokens":14}}
                    """.formatted(message, tool ? "tool_calls" : "stop");
        };
    }

    private static String streamResponse(Provider provider) {
        if (provider == Provider.ANTHROPIC || provider == Provider.ANTHROPIC_COMPATIBLE) {
            return """
                    event: message_start
                    data: {"type":"message_start","message":{"id":"msg_mock","type":"message","role":"assistant","model":"mock-model","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":11,"output_tokens":0}}}

                    event: content_block_start
                    data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                    event: content_block_delta
                    data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"mock-"}}

                    event: content_block_delta
                    data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"reply"}}

                    event: content_block_stop
                    data: {"type":"content_block_stop","index":0}

                    event: message_delta
                    data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":3}}

                    event: message_stop
                    data: {"type":"message_stop"}

                    """;
        }
        if (provider == Provider.GEMINI || provider == Provider.DASHSCOPE) {
            return "data: " + response(provider, false).replace("\n", "") + "\n\n";
        }
        return """
                data: {"id":"mock","object":"chat.completion.chunk","created":1,"model":"mock-model","choices":[{"index":0,"delta":{"role":"assistant","content":"mock-"},"finish_reason":null}]}

                data: {"id":"mock","object":"chat.completion.chunk","created":1,"model":"mock-model","choices":[{"index":0,"delta":{"content":"reply"},"finish_reason":null}]}

                data: {"id":"mock","object":"chat.completion.chunk","created":1,"model":"mock-model","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":11,"completion_tokens":3,"total_tokens":14}}

                data: [DONE]

                """;
    }

    private record Request(String path, String authorization, String anthropicKey, String googleKey, String body) { }
}
