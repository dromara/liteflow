package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.property.agent.JevConfig;
import com.yomahub.liteflow.property.agent.JevProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class JevChoiceClientTest {
    private static final Map<String, String> OPTIONS = Map.of(
            "refund", "退款", "exchange", "换货", JevSwitchComponent.NO_MATCH, "均不适用");
    private MockJevServer server;
    private JevConfig config;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockJevServer();
        config = new JevConfig();
        config.setBaseUrl(server.baseUrl() + "/");
        config.setApiKey("jev-offline-key");
        config.setModel("jev-test-requested");
        config.setTimeout(Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() { server.close(); }

    @Test
    void sendsTypedChoiceToConfiguredEndpointAndPreservesFullResult() throws Exception {
        Object state = Map.of("message", "暂时先不退了，想换一件", "order", Map.of("id", "A-1"));
        JevChoiceResult result = evaluate(state);
        MockJevServer.Request request = server.requests.poll(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("/gateway/v1/systemone", request.path());
        assertEquals("Bearer jev-offline-key", request.authorization());
        assertEquals("application/json", request.contentType());
        assertEquals(MockJevServer.JSON.valueToTree(state), request.body().path("state"));
        assertEquals("jev-test-requested", request.body().path("model").asText());
        assertEquals("choice", request.body().at("/questions/route/type").asText());
        assertEquals("选择处理流程", request.body().at("/questions/route/instructions").asText());
        assertEquals(MockJevServer.JSON.valueToTree(OPTIONS), request.body().at("/questions/route/criteria"));
        assertEquals("refund", result.choice());
        assertEquals("jev-test-resolved", result.model());
        assertEquals(0.95, result.confidence());
        assertEquals(0.8, result.probabilities().get("refund"));
        assertThrows(UnsupportedOperationException.class, () -> result.probabilities().put("refund", 0.0));
    }

    @Test
    void providerDefaultsAndExplicitOverridesDoNotDependOnSetterOrder() {
        JevConfig defaults = new JevConfig();
        assertEquals(JevProvider.TYPESAFE, defaults.getProvider());
        assertEquals("https://api.typesafe.ai/v1/systemone", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("jev-1.13.0", defaults.getModel());
        defaults.setProvider(JevProvider.OPENROUTER);
        assertEquals("https://openrouter.ai/api/alpha/decisions", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("typesafe/jev-1.13", defaults.getModel());

        defaults.setBaseUrl("http://localhost:8089/proxy/api/alpha///");
        defaults.setModel("custom-model");
        defaults.setProvider(JevProvider.TYPESAFE);
        assertEquals("http://localhost:8089/proxy/api/alpha/systemone", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("custom-model", defaults.getModel());
        defaults.setProvider(JevProvider.OPENROUTER);
        assertEquals("http://localhost:8089/proxy/api/alpha/decisions", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("custom-model", defaults.getModel());

        defaults.setBaseUrl("");
        defaults.setModel(" ");
        assertEquals("https://openrouter.ai/api/alpha/decisions", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("typesafe/jev-1.13", defaults.getModel());
        defaults.setProvider(JevProvider.TYPESAFE);
        assertEquals("https://api.typesafe.ai/v1/systemone", JevChoiceClient.endpoint(defaults).toString());
        assertEquals("jev-1.13.0", defaults.getModel());
    }

    @Test
    void openRouterUsesDecisionsAndPreservesProviderScoresAndResolvedModel() throws Exception {
        config.setProvider(JevProvider.OPENROUTER);
        config.setBaseUrl(server.baseUrl().replace("/gateway/v1", "/api/alpha/"));
        config.setModel(null);
        config.setApiKey("openrouter-offline-key");
        // OpenRouter's Decisions envelope includes billing and provider metadata.
        ObjectNode response = (ObjectNode) MockJevServer.JSON.readTree(MockJevServer.response("refund", 0.75));
        response.put("id", "gen-dec-offline");
        response.put("model", "typesafe/jev-1.13-20260917");
        response.put("provider", "TypeSafe");
        response.putObject("usage").put("input_tokens", 476).put("output_tokens", 70).put("cost", 0.000019992);
        server.answer = request -> response.toString();

        JevChoiceResult result = evaluate("我想退款");
        MockJevServer.Request request = server.requests.poll(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("/api/alpha/decisions", request.path());
        assertEquals("Bearer openrouter-offline-key", request.authorization());
        assertEquals("typesafe/jev-1.13", request.body().path("model").asText());
        assertEquals("我想退款", request.body().path("state").asText());
        assertEquals("choice", request.body().at("/questions/route/type").asText());
        assertEquals("选择处理流程", request.body().at("/questions/route/instructions").asText());
        assertEquals(MockJevServer.JSON.valueToTree(OPTIONS), request.body().at("/questions/route/criteria"));
        assertFalse(request.body().has("messages"));
        assertEquals("typesafe/jev-1.13-20260917", result.model());
        assertEquals("refund", result.choice());
        assertEquals(0.75, result.confidence());
        assertEquals(0.8, result.probabilities().get("refund"));
    }

    @Test
    void acceptsTextAndArrayStates() throws Exception {
        evaluate("换货");
        assertTrue(server.requests.take().body().path("state").isTextual());
        evaluate(List.of("客户希望换货", "订单已签收"));
        assertTrue(server.requests.take().body().path("state").isArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 402, 422, 429, 500, 502, 503, 524, 529, 302})
    void httpErrorsAreNotDecisionsAndDoNotExposeResponseBodiesOrFollowRedirects(int status) {
        config.setProvider(JevProvider.OPENROUTER);
        server.status = status;
        server.answer = request -> "secret response: jev-offline-key";
        JevInvocationException failure = assertThrows(JevInvocationException.class, () -> evaluate("input"));
        assertEquals(status, failure.getStatusCode());
        assertFalse(failure.toString().contains("jev-offline-key"));
        assertEquals(1, server.requests.size());
    }

    @Test
    void timesOutWhileReadingTheBodyNotJustTheHeaders() throws Exception {
        evaluate("warmup");
        server.requests.clear();
        server.stallBody = true;
        config.setTimeout(Duration.ofMillis(150));
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                assertThrows(JevInvocationException.class, () -> evaluate("slow body")));
        assertEquals(1, server.requests.size());
    }

    @Test
    void interruptionCancelsWaitingAndRestoresInterruptFlag() throws Exception {
        server.stallBody = true;
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try { evaluate("interrupted"); }
            catch (Throwable error) {
                failure.set(error);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        try {
            caller.start();
            assertNotNull(server.requests.poll(2, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(2_000);
            assertFalse(caller.isAlive());
            assertInstanceOf(InterruptedException.class, failure.get());
            assertTrue(interrupted.get());
        } finally {
            caller.interrupt();
            server.release.countDown();
            caller.join(2_000);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{}", "not-json", "{} {}"})
    void rejectsMalformedResponses(String body) {
        server.answer = request -> body;
        assertThrows(JevInvocationException.class, () -> evaluate("input"));
    }

    @ParameterizedTest
    @EnumSource(JevProvider.class)
    void rejectsInvalidChoiceConfidenceAndDistributions(JevProvider provider) throws Exception {
        config.setProvider(provider);
        List<Consumer<ObjectNode>> mutations = List.of(
                root -> root.remove("model"),
                root -> answer(root).put("type", "score"),
                root -> answer(root).put("choice", "unregistered"),
                root -> answer(root).remove("choice"),
                root -> answer(root).remove("confidence"),
                root -> answer(root).put("confidence", "0.99"),
                root -> answer(root).put("confidence", 1.1),
                root -> answer(root).put("confidence", -0.1),
                root -> answer(root).put("confidence", Double.NaN),
                root -> answer(root).remove("probabilities"),
                root -> probabilities(root).remove("exchange"),
                root -> probabilities(root).put("unknown", 0.0),
                root -> probabilities(root).put("exchange", -0.1),
                root -> probabilities(root).put("exchange", 1.1),
                root -> probabilities(root).put("exchange", "0.1"),
                root -> probabilities(root).put("exchange", 0.5),
                root -> answer(root).put("choice", "exchange"));
        for (Consumer<ObjectNode> mutation : mutations) {
            ObjectNode root = (ObjectNode) MockJevServer.JSON.readTree(MockJevServer.response("refund", 0.9));
            mutation.accept(root);
            server.answer = request -> root.toString();
            assertThrows(JevInvocationException.class, () -> evaluate("input"), root.toString());
        }
    }

    @Test
    void invalidConfigurationFailsBeforeAnyHttpCall() {
        List<Consumer<JevConfig>> mutations = List.of(
                c -> c.setApiKey(null), c -> c.setApiKey(" "), c -> c.setApiKey("key\nInjected: value"),
                c -> c.setApiKey("key\u0000"), c -> c.setApiKey("填写凭据"),
                c -> c.setProvider(null), c -> c.setBaseUrl("file:///tmp/test"),
                c -> c.setBaseUrl("https://user:password@example.org/v1"),
                c -> c.setBaseUrl("https://example.org/v1?key=secret"),
                c -> c.setBaseUrl("https://example.org/v1#fragment"),
                c -> c.setTimeout(null), c -> c.setTimeout(Duration.ZERO),
                c -> c.setTimeout(Duration.ofMillis(-1)), c -> c.setTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
        for (Consumer<JevConfig> mutation : mutations) {
            JevConfig invalid = new JevConfig();
            invalid.setApiKey("offline-key");
            invalid.setBaseUrl(server.baseUrl());
            mutation.accept(invalid);
            assertThrows(IllegalArgumentException.class,
                    () -> JevChoiceClient.evaluate(invalid, "input", "select", OPTIONS));
        }
        assertThrows(IllegalArgumentException.class, () -> evaluate(null));
        assertThrows(IllegalArgumentException.class, () -> evaluate(42));
        assertTrue(server.requests.isEmpty());
    }

    private JevChoiceResult evaluate(Object state) throws InterruptedException {
        return JevChoiceClient.evaluate(config, state, "选择处理流程", OPTIONS);
    }

    private static ObjectNode answer(ObjectNode root) { return (ObjectNode) root.at("/answers/route"); }
    private static ObjectNode probabilities(ObjectNode root) { return (ObjectNode) answer(root).path("probabilities"); }
}
