package com.yomahub.liteflow.agent.jev;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.NodeTypeEnum;
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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JevBooleanComponentTest {
    private MockJevServer server;
    private LiteflowConfig config;
    private FlowExecutor executor;
    private Judge judge;

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
        server.answer = request -> MockJevServer.noulResponse(0.9);
        JevConfig jev = config.getAgent().getJev();
        jev.setApiKey("jev-offline-key");
        jev.setBaseUrl(server.baseUrl());
        jev.setTimeout(Duration.ofSeconds(3));
        judge = new Judge();
        FlowBus.addManagedNode("judge", judge);
        FlowBus.addManagedNode("yes", new Target());
        FlowBus.addManagedNode("no", new Target());
        FlowBus.addManagedNode("manual", new Target());
        chain("main", "IF(judge, yes, no)");
    }

    @AfterEach
    void tearDown() {
        server.close();
        FlowBus.cleanCache();
        FlowExecutorHolder.clean();
        LiteflowConfigGetter.clean();
    }

    @Test
    void inheritedBooleanTypeSelectsTrueBranchAndRecordsDecision() {
        assertEquals(NodeTypeEnum.BOOLEAN, FlowBus.getNode("judge").getType());
        Context context = execute("main", "我想找人工客服");
        assertEquals(List.of("yes"), List.copyOf(context.targets));
        JevNoulResult decision = context.decisions.element();
        assertEquals(0.9, decision.probability());
        assertEquals("jev-test-resolved", decision.model());
        MockJevServer.Request request = server.requests.element();
        assertEquals("我想找人工客服", request.body().path("state").asText());
        assertEquals("noul", request.body().at("/questions/judge/type").asText());
        assertEquals("客户是否要求人工客服", request.body().at("/questions/judge/instructions").asText());
    }

    @Test
    void lowProbabilitySelectsFalseBranchAndStillExposesRawAnswer() {
        server.answer = request -> MockJevServer.noulResponse(0.07);
        Context context = execute("main", "谢谢，问题解决了");
        assertEquals(List.of("no"), List.copyOf(context.targets));
        assertEquals(0.07, context.decisions.element().probability());
    }

    @Test
    void acceptsThresholdBoundaryAndSupportsComponentOverride() {
        server.answer = request -> MockJevServer.noulResponse(0.5);
        assertEquals(List.of("yes"), List.copyOf(execute("main", "边界").targets));
        config.getAgent().getJev().setNoulThreshold(0.8);
        server.answer = request -> MockJevServer.noulResponse(0.8);
        assertEquals(List.of("yes"), List.copyOf(execute("main", "边界").targets));
        judge.threshold = 0.9;
        assertEquals(List.of("no"), List.copyOf(execute("main", "边界").targets));
    }

    @Test
    void composesWithBooleanLogicOperators() {
        FlowBus.addManagedNode("repeatJudge", new Judge("客户是否之前联系过客服"));
        chain("logic", "IF(AND(judge, NOT(repeatJudge)), yes, no)");
        server.answer = request ->
                MockJevServer.noulResponse(request.body().at("/questions/judge/instructions").asText()
                        .equals("客户是否要求人工客服") ? 0.9 : 0.2);
        Context context = execute("logic", "input");
        assertEquals(List.of("yes"), List.copyOf(context.targets));
        assertEquals(2, context.decisions.size());
    }

    @Test
    void httpAndProtocolFailuresDoNotFallThroughToFalse() {
        server.status = 401;
        Context context = new Context("退款");
        LiteflowResponse response = executor.execute2Resp("main", null, context);
        assertFalse(response.isSuccess());
        assertEquals(401, assertInstanceOf(JevInvocationException.class, response.getCause()).getStatusCode());
        assertTrue(context.targets.isEmpty());
        assertTrue(context.decisions.isEmpty());
        server.status = 200;
        server.answer = request -> MockJevServer.response("refund", 0.99);
        response = executor.execute2Resp("main", null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(JevInvocationException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
    }

    @Test
    void technicalFailuresCanUseExistingCatchOperator() {
        chain("caught", "CATCH(IF(judge, yes, no)).DO(manual)");
        server.status = 529;
        assertEquals(List.of("manual"), List.copyOf(execute("caught", "退款").targets));
    }

    @Test
    void decisionCallbackFailureStopsBranchExecution() {
        FlowBus.addManagedNode("failingCallback", new Judge() {
            @Override protected void onDecision(JevNoulResult result) {
                throw new IllegalStateException("decision recorder failed");
            }
        });
        chain("callback", "IF(failingCallback, yes, no)");
        Context context = new Context("退款");
        LiteflowResponse response = executor.execute2Resp("callback", null, context);
        assertFalse(response.isSuccess());
        assertInstanceOf(IllegalStateException.class, response.getCause());
        assertTrue(context.targets.isEmpty());
    }

    @Test
    void configurationIsCheckedOnlyWhenJevIsUsed() {
        config.getAgent().getJev().setApiKey(null);
        chain("ordinary", "THEN(yes)");
        assertEquals(List.of("yes"), List.copyOf(execute("ordinary", "普通流程").targets));
        assertConfigurationFailure("main");
        config.setAgent(null);
        assertConfigurationFailure("main");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void invalidThresholdAndInstructionsFailBeforeHttp() {
        for (double threshold : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            judge.threshold = threshold;
            assertConfigurationFailure("main");
        }
        judge.threshold = null;
        judge.question = " ";
        assertConfigurationFailure("main");
        assertTrue(server.requests.isEmpty());
    }

    @Test
    void concurrentInvocationsOfOneComponentDoNotShareStateOrDecisions() throws Exception {
        server.answer = request ->
                MockJevServer.noulResponse("yes".equals(request.body().path("state").asText()) ? 0.9 : 0.1);
        ExecutorService callers = Executors.newFixedThreadPool(6);
        try {
            List<Future<Context>> results = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                String input = i % 2 == 0 ? "yes" : "no";
                results.add(callers.submit(() -> execute("main", input)));
            }
            for (Future<Context> future : results) {
                Context context = future.get(10, TimeUnit.SECONDS);
                assertEquals(List.of(context.input), List.copyOf(context.targets));
                assertEquals(1, context.decisions.size());
                double expected = "yes".equals(context.input) ? 0.9 : 0.1;
                assertEquals(expected, context.decisions.element().probability());
            }
        } finally {
            callers.shutdownNow();
        }
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
        final ConcurrentLinkedQueue<JevNoulResult> decisions = new ConcurrentLinkedQueue<>();
        Context(String input) { this.input = input; }
    }

    public static class Judge extends JevBooleanComponent {
        String question;
        Double threshold;
        Judge() { this("客户是否要求人工客服"); }
        Judge(String question) { this.question = question; }
        @Override protected Object state() { return getContextBean(Context.class).input; }
        @Override protected String instructions() { return question; }
        @Override protected double probabilityThreshold() { return threshold == null ? super.probabilityThreshold() : threshold; }
        @Override protected void onDecision(JevNoulResult result) { getContextBean(Context.class).decisions.add(result); }
    }

    public static class Target extends NodeComponent {
        @Override public void process() { getContextBean(Context.class).targets.add(getNodeId()); }
    }
}
