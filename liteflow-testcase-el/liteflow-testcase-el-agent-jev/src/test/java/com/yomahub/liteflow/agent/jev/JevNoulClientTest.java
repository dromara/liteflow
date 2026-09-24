package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.property.agent.JevConfig;
import com.yomahub.liteflow.property.agent.JevProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class JevNoulClientTest {
    private MockJevServer server;
    private JevConfig config;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockJevServer();
        server.answer = request -> MockJevServer.noulResponse(0.93);
        config = new JevConfig();
        config.setBaseUrl(server.baseUrl() + "/");
        config.setApiKey("jev-offline-key");
        config.setModel("jev-test-requested");
        config.setTimeout(Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() { server.close(); }

    @Test
    void sendsTypedNoulToConfiguredEndpointAndPreservesResult() throws Exception {
        Object state = Map.of("message", "我已经反馈三次了，能让我和真人说话吗");
        JevNoulResult result = evaluate(state);
        MockJevServer.Request request = server.requests.poll(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("/gateway/v1/systemone", request.path());
        assertEquals("Bearer jev-offline-key", request.authorization());
        assertEquals("application/json", request.contentType());
        assertEquals(MockJevServer.JSON.valueToTree(state), request.body().path("state"));
        assertEquals("jev-test-requested", request.body().path("model").asText());
        assertEquals("noul", request.body().at("/questions/judge/type").asText());
        assertEquals("客户是否要求人工客服", request.body().at("/questions/judge/instructions").asText());
        assertFalse(request.body().at("/questions/judge").has("criteria"));
        assertEquals("jev-test-resolved", result.model());
        assertEquals(0.93, result.probability());
    }

    @Test
    void usesTheSameEndpointAndModelForOpenRouter() throws Exception {
        config.setProvider(JevProvider.OPENROUTER);
        config.setBaseUrl(server.baseUrl().replace("/gateway/v1", "/api/alpha/"));
        config.setModel(null);
        JevNoulResult result = evaluate("我想退款");
        MockJevServer.Request request = server.requests.poll(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/api/alpha/decisions", request.path());
        assertEquals("typesafe/jev-1.13", request.body().path("model").asText());
        assertEquals("noul", request.body().at("/questions/judge/type").asText());
        assertEquals(0.93, result.probability());
    }

    @Test
    void acceptsTextAndArrayStates() throws Exception {
        evaluate("换货");
        assertTrue(server.requests.take().body().path("state").isTextual());
        evaluate(List.of("客户希望换货", "订单已签收"));
        assertTrue(server.requests.take().body().path("state").isArray());
    }

    @Test
    void httpErrorsAreNotDecisionsAndDoNotExposeResponseBodies() {
        server.status = 429;
        server.answer = request -> "secret response: jev-offline-key";
        JevInvocationException failure = assertThrows(JevInvocationException.class, () -> evaluate("input"));
        assertEquals(429, failure.getStatusCode());
        assertFalse(failure.toString().contains("jev-offline-key"));
        assertEquals(1, server.requests.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{}", "not-json", "{} {}"})
    void rejectsMalformedResponses(String body) {
        server.answer = request -> body;
        assertThrows(JevInvocationException.class, () -> evaluate("input"));
    }

    @ParameterizedTest
    @EnumSource(JevProvider.class)
    void rejectsInvalidNoulAnswers(JevProvider provider) throws Exception {
        config.setProvider(provider);
        List<Consumer<ObjectNode>> mutations = List.of(
                root -> root.remove("model"),
                root -> answer(root).put("type", "choice"),
                root -> answer(root).remove("type"),
                root -> answer(root).remove("noul"),
                root -> answer(root).put("noul", "0.99"),
                root -> answer(root).put("noul", 1.1),
                root -> answer(root).put("noul", -0.1),
                root -> answer(root).put("noul", Double.NaN),
                root -> answer(root).putNull("noul"));
        for (Consumer<ObjectNode> mutation : mutations) {
            ObjectNode root = (ObjectNode) MockJevServer.JSON.readTree(MockJevServer.noulResponse(0.9));
            mutation.accept(root);
            server.answer = request -> root.toString();
            assertThrows(JevInvocationException.class, () -> evaluate("input"), root.toString());
        }
    }

    @Test
    void invalidConfigurationFailsBeforeAnyHttpCall() {
        List<Consumer<JevConfig>> mutations = List.of(
                c -> c.setApiKey(null), c -> c.setApiKey(" "),
                c -> c.setBaseUrl("file:///tmp/test"),
                c -> c.setTimeout(Duration.ZERO));
        for (Consumer<JevConfig> mutation : mutations) {
            JevConfig invalid = new JevConfig();
            invalid.setApiKey("offline-key");
            invalid.setBaseUrl(server.baseUrl());
            mutation.accept(invalid);
            assertThrows(IllegalArgumentException.class,
                    () -> JevNoulClient.evaluate(invalid, "input", "judge"));
        }
        assertThrows(IllegalArgumentException.class, () -> evaluate(null));
        assertThrows(IllegalArgumentException.class, () -> evaluate(42));
        assertTrue(server.requests.isEmpty());
    }

    private JevNoulResult evaluate(Object state) throws InterruptedException {
        return JevNoulClient.evaluate(config, state, "客户是否要求人工客服");
    }

    private static ObjectNode answer(ObjectNode root) { return (ObjectNode) root.at("/answers/judge"); }
}
