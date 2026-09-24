package com.yomahub.liteflow.agent.jev;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.JevConfig;
import com.yomahub.liteflow.property.agent.JevProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class LayaProviderTest {
    private MockJevServer server;
    private LiteflowConfig config;
    private FlowExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        config = new LiteflowConfig();
        config.setPrintBanner(false);
        config.setPrintExecutionLog(false);
        config.setAgent(new AgentConfig());
        LiteflowConfigGetter.setLiteflowConfig(config);
        FlowBus.cleanCache();
        executor = new FlowExecutor(config);
        server = new MockJevServer();
        JevConfig jev = config.getAgent().getJev();
        jev.setProvider(JevProvider.LAYA);
        // The api-key is deliberately left unset: a local laya-serve has no auth by default.
        jev.setBaseUrl(server.baseUrl());
        jev.setTimeout(Duration.ofSeconds(3));
        FlowBus.addManagedNode("router", new Router());
        FlowBus.addManagedNode("judge", new Judge());
        FlowBus.addManagedNode("refund", new Target());
        FlowBus.addManagedNode("exchange", new Target());
        FlowBus.addManagedNode("manual", new Target());
        chain("switch", "SWITCH(router).to(refund, exchange).DEFAULT(manual)");
        chain("bool", "IF(judge, refund, exchange)");
    }

    @AfterEach
    void tearDown() {
        server.close();
        FlowBus.cleanCache();
        FlowExecutorHolder.clean();
        LiteflowConfigGetter.clean();
    }

    @Test
    void layaProviderDefaultsPointAtLocalServerAndLetTheRouterPickACheckpoint() {
        JevConfig jev = new JevConfig();
        jev.setProvider(JevProvider.LAYA);
        assertEquals("http://127.0.0.1:8000/v1", jev.getBaseUrl());
        assertEquals("auto", jev.getModel());
        assertEquals(URI.create("http://127.0.0.1:8000/v1/systemone"), JevTransport.endpoint(jev));
    }

    @Test
    void blankApiKeyIsAcceptedAndSendsNoAuthorizationHeader() {
        Context context = execute("switch", "想退款");
        assertEquals(List.of("refund"), List.copyOf(context.targets));
        MockJevServer.Request request = server.requests.element();
        assertNull(request.authorization());
        assertEquals("/gateway/v1/systemone", request.path());
        assertEquals("auto", request.body().path("model").asText());
    }

    @Test
    void explicitCheckpointNameIsPassedThroughAsModel() {
        config.getAgent().getJev().setModel("multilingual");
        assertEquals(List.of("refund"), List.copyOf(execute("switch", "想退款").targets));
        assertEquals("multilingual", server.requests.element().body().path("model").asText());
    }

    @Test
    void configuredApiKeyIsSentAsBearerToken() {
        config.getAgent().getJev().setApiKey("laya-local-key");
        assertEquals(List.of("refund"), List.copyOf(execute("switch", "想退款").targets));
        assertEquals("Bearer laya-local-key", server.requests.element().authorization());
    }

    @Test
    void booleanComponentWorksAgainstLayaWithoutApiKey() {
        server.answer = request -> MockJevServer.noulResponse(0.9);
        Context context = execute("bool", "我要找人工客服");
        assertEquals(List.of("refund"), List.copyOf(context.targets));
        assertNull(server.requests.element().authorization());
        assertEquals("auto", server.requests.element().body().path("model").asText());
    }

    @Test
    void malformedApiKeyIsStillRejected() {
        config.getAgent().getJev().setApiKey("bad key");
        assertConfigurationFailure("switch");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void hostedProvidersStillRequireAnApiKey() {
        config.getAgent().getJev().setProvider(JevProvider.TYPESAFE);
        assertConfigurationFailure("switch");
        config.getAgent().getJev().setProvider(JevProvider.OPENROUTER);
        assertConfigurationFailure("switch");
        assertTrue(server.requests.isEmpty());
    }

    private void assertConfigurationFailure(String chainId) {
        Context context = new Context("input");
        LiteflowResponse response = executor.execute2Resp(chainId, null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(IllegalArgumentException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
    }

    private Context execute(String chainId, String input) {
        Context context = new Context(input);
        LiteflowResponse response = executor.execute2Resp(chainId, null, context);
        assertTrue(response.isSuccess(), () -> String.valueOf(response.getCause()));
        return context;
    }

    private static void chain(String id, String el) {
        LiteFlowChainELBuilder.createChain().setChainId(id).setEL(el).build();
    }

    public static class Context {
        final String input;
        final ConcurrentLinkedQueue<String> targets = new ConcurrentLinkedQueue<>();
        Context(String input) { this.input = input; }
    }

    public static class Router extends JevSwitchComponent {
        @Override protected Object state() { return getContextBean(Context.class).input; }
        @Override protected String instructions() { return "选择处理流程"; }
        @Override protected Map<String, String> choices() { return Map.of("refund", "退款", "exchange", "换货"); }
    }

    public static class Judge extends JevBooleanComponent {
        @Override protected Object state() { return getContextBean(Context.class).input; }
        @Override protected String instructions() { return "客户是否要求人工客服"; }
    }

    public static class Target extends NodeComponent {
        @Override public void process() { getContextBean(Context.class).targets.add(getNodeId()); }
    }
}
