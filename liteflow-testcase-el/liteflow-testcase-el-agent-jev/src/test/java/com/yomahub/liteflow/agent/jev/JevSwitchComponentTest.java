package com.yomahub.liteflow.agent.jev;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.JevConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JevSwitchComponentTest {
    private MockJevServer server;
    private LiteflowConfig config;
    private FlowExecutor executor;
    private Router router;

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
        jev.setApiKey("jev-offline-key");
        jev.setBaseUrl(server.baseUrl());
        jev.setTimeout(Duration.ofSeconds(3));
        router = new Router();
        FlowBus.addManagedNode("router", router);
        FlowBus.addManagedNode("refund", new Target());
        FlowBus.addManagedNode("exchange", new Target());
        FlowBus.addManagedNode("manual", new Target());
        chain("main", "SWITCH(router).to(refund, exchange).DEFAULT(manual)");
    }

    @AfterEach
    void tearDown() {
        server.close();
        FlowBus.cleanCache();
        FlowExecutorHolder.clean();
        LiteflowConfigGetter.clean();
    }

    @Test
    void inheritedSwitchTypeSelectsOnlyTheReturnedIdAndRecordsDecision() {
        assertEquals(NodeTypeEnum.SWITCH, FlowBus.getNode("router").getType());
        Context context = execute("main", "想退款");
        assertEquals(List.of("refund"), List.copyOf(context.targets));
        JevChoiceResult decision = context.decisions.element();
        assertEquals("refund", decision.choice());
        assertEquals("jev-test-resolved", decision.model());
        assertEquals(0.95, decision.confidence());
        assertEquals(Set.of("refund", "exchange", JevSwitchComponent.NO_MATCH), decision.probabilities().keySet());
        assertEquals("想退款", server.requests.element().body().path("state").asText());
    }

    @Test
    void canRouteToAChainId() {
        chain("refundChain", "THEN(refund)");
        router.options = Map.of("refundChain", "处理退款", "exchange", "处理换货");
        chain("nested", "SWITCH(router).to(refundChain, exchange).DEFAULT(manual)");
        server.answer = request -> MockJevServer.response("refundChain", 0.9,
                "refundChain", "exchange", JevSwitchComponent.NO_MATCH);
        assertEquals(List.of("refund"), List.copyOf(execute("nested", "退款").targets));
    }

    @Test
    void lowConfidenceAndNoMatchUseDefaultAndStillExposeRawAnswer() {
        server.answer = request -> MockJevServer.response("refund", 0.59);
        Context uncertain = execute("main", "你们看着处理");
        assertEquals(List.of("manual"), List.copyOf(uncertain.targets));
        assertEquals("refund", uncertain.decisions.element().choice());
        server.answer = request -> MockJevServer.response(JevSwitchComponent.NO_MATCH, 0.99);
        Context noMatch = execute("main", "今天天气如何");
        assertEquals(List.of("manual"), List.copyOf(noMatch.targets));
        assertEquals(JevSwitchComponent.NO_MATCH, noMatch.decisions.element().choice());
    }

    @Test
    void acceptsThresholdBoundaryAndSupportsComponentOverride() {
        config.getAgent().getJev().setMinConfidence(0.85);
        server.answer = request -> MockJevServer.response("refund", 0.85);
        assertEquals(List.of("refund"), List.copyOf(execute("main", "退款").targets));
        router.threshold = 0.9;
        assertEquals(List.of("manual"), List.copyOf(execute("main", "退款").targets));
    }

    @Test
    void missingDefaultUsesExistingNoTargetFailure() {
        chain("noDefault", "SWITCH(router).to(refund, exchange)");
        server.answer = request -> MockJevServer.response(JevSwitchComponent.NO_MATCH, 0.99);
        Context context = new Context("无对应诉求");
        LiteflowResponse response = executor.execute2Resp("noDefault", null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(NoSwitchTargetNodeException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
        assertEquals(1, context.decisions.size());
    }

    @Test
    void httpAndProtocolFailuresDoNotFallThroughToDefault() {
        server.status = 401;
        Context context = new Context("退款");
        LiteflowResponse response = executor.execute2Resp("main", null, context);
        assertFalse(response.isSuccess());
        assertEquals(401, assertInstanceOf(JevInvocationException.class, response.getCause()).getStatusCode());
        assertTrue(context.targets.isEmpty());
        assertTrue(context.decisions.isEmpty());
        server.status = 200;
        server.answer = request -> MockJevServer.response("unregistered", 0.99);
        response = executor.execute2Resp("main", null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(JevInvocationException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
    }

    @Test
    void technicalFailuresCanUseExistingCatchOperator() {
        chain("caught", "CATCH(SWITCH(router).to(refund, exchange)).DO(manual)");
        server.status = 529;
        assertEquals(List.of("manual"), List.copyOf(execute("caught", "退款").targets));
    }

    @Test
    void decisionCallbackFailureStopsTargetExecution() {
        FlowBus.addManagedNode("failingCallback", new Router() {
            @Override protected void onDecision(JevChoiceResult result) {
                throw new IllegalStateException("decision recorder failed");
            }
        });
        chain("callback", "SWITCH(failingCallback).to(refund, exchange).DEFAULT(manual)");
        Context context = new Context("退款");
        LiteflowResponse response = executor.execute2Resp("callback", null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(IllegalStateException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
    }

    @Test
    void validatesCandidatesAgainstEachCurrentSwitchBeforeRequesting() {
        List<Map<String, String>> invalidOptions = new ArrayList<>();
        invalidOptions.add(Map.of());
        invalidOptions.add(Map.of("manual", "未列在 to 中"));
        invalidOptions.add(Map.of("missing", "不存在"));
        invalidOptions.add(Map.of(JevSwitchComponent.NO_MATCH, "保留 ID"));
        invalidOptions.add(Map.of("refund", " "));
        invalidOptions.add(Map.of("refund:tag", "暂不支持标签路由"));
        Map<String, String> withNull = new HashMap<>();
        withNull.put(null, "空 ID");
        invalidOptions.add(withNull);
        for (Map<String, String> options : invalidOptions) {
            router.options = options;
            assertConfigurationFailure("main");
        }
        router.options = null;
        assertConfigurationFailure("main");
        router.options = Map.of("refund", "退款");
        chain("ambiguous", "SWITCH(router).to(refund.tag(\"one\"), refund.tag(\"two\")).DEFAULT(manual)");
        assertConfigurationFailure("ambiguous");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void configurationIsCheckedOnlyWhenJevIsUsed() {
        config.getAgent().getJev().setApiKey(null);
        chain("ordinary", "THEN(refund)");
        assertEquals(List.of("refund"), List.copyOf(execute("ordinary", "普通流程").targets));
        assertConfigurationFailure("main");
        config.setAgent(null);
        assertConfigurationFailure("main");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void invalidThresholdAndInstructionsFailBeforeHttp() {
        for (double threshold : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            router.threshold = threshold;
            assertConfigurationFailure("main");
        }
        router.threshold = null;
        router.question = " ";
        assertConfigurationFailure("main");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void concurrentInvocationsOfOneComponentDoNotShareStateOrDecisions() throws Exception {
        server.answer = request -> MockJevServer.response(request.body().path("state").asText(), 0.9);
        ExecutorService callers = Executors.newFixedThreadPool(6);
        try {
            List<Future<Context>> results = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                String choice = i % 2 == 0 ? "refund" : "exchange";
                results.add(callers.submit(() -> execute("main", choice)));
            }
            for (Future<Context> future : results) {
                Context context = future.get(10, TimeUnit.SECONDS);
                assertEquals(List.of(context.input), List.copyOf(context.targets));
                assertEquals(1, context.decisions.size());
                assertEquals(context.input, context.decisions.element().choice());
            }
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void parallelSwitchesCanReuseAComponentWithDifferentTargetLists() {
        FlowBus.addManagedNode("dynamicRouter", new Router() {
            @Override protected Map<String, String> choices() { return Map.of(getTargetList().get(0), "处理当前分支"); }
        });
        chain("parallel", "WHEN(SWITCH(dynamicRouter).to(refund), SWITCH(dynamicRouter).to(exchange))");
        server.answer = request -> {
            List<String> options = new ArrayList<>();
            request.body().at("/questions/route/criteria").fieldNames().forEachRemaining(options::add);
            String selected = options.stream().filter(id -> !id.equals(JevSwitchComponent.NO_MATCH)).findFirst().orElseThrow();
            return MockJevServer.response(selected, 0.95, options.toArray(new String[0]));
        };
        Context context = execute("parallel", "input");
        assertEquals(Set.of("refund", "exchange"), Set.copyOf(context.targets));
        assertEquals(2, context.decisions.size());
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
        final ConcurrentLinkedQueue<JevChoiceResult> decisions = new ConcurrentLinkedQueue<>();
        Context(String input) { this.input = input; }
    }

    public static class Router extends JevSwitchComponent {
        Map<String, String> options = Map.of("refund", "退款", "exchange", "换货");
        String question = "选择处理流程";
        Double threshold;
        @Override protected Object state() { return getContextBean(Context.class).input; }
        @Override protected String instructions() { return question; }
        @Override protected Map<String, String> choices() { return options; }
        @Override protected double minConfidence() { return threshold == null ? super.minConfidence() : threshold; }
        @Override protected void onDecision(JevChoiceResult result) { getContextBean(Context.class).decisions.add(result); }
    }

    public static class Target extends NodeComponent {
        @Override public void process() { getContextBean(Context.class).targets.add(getNodeId()); }
    }
}
