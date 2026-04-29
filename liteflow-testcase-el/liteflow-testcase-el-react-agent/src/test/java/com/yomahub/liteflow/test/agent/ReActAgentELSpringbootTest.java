package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * 走 LiteFlow EL 编排的 ReActAgent 示例测试。
 * <p>
 * 每个用例都通过 {@code flowExecutor.execute2Resp(chainId, ...)} 跑一条 chain，
 * Agent 节点是 {@link com.yomahub.liteflow.agent.component.ReActAgentComponent}
 * 的具体子类，apiKey 通过 {@code application.properties} 中的
 * {@code liteflow.agent.<platform>.api-key} 注入。留空则用例自动 skip。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = ReActAgentELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp"
})
public class ReActAgentELSpringbootTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    /** 真正运行链路并断言成功，回复非空。 */
    private void runChainExpectingReply(String chainId, String question) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, question);
        if (!response.isSuccess() && response.getCause() != null) {
            response.getCause().printStackTrace();
        }
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply, "agent reply must be recorded");
        System.out.println(">>> [" + chainId + "] reply=" + reply);
    }

    private String openaiKey()    { return ApiKeys.resolve("OPENAI_API_KEY",    liteflowConfig.getAgent().getOpenai().getApiKey()); }
    private String anthropicKey() { return ApiKeys.resolve("ANTHROPIC_API_KEY", liteflowConfig.getAgent().getAnthropic().getApiKey()); }
    private String geminiKey()    { return ApiKeys.resolve("GEMINI_API_KEY",    liteflowConfig.getAgent().getGemini().getApiKey()); }
    private String dashscopeKey() { return ApiKeys.resolve("DASHSCOPE_API_KEY", liteflowConfig.getAgent().getDashscope().getApiKey()); }
    private String deepseekKey() {
        return ApiKeys.resolve("DEEPSEEK_API_KEY",
                liteflowConfig.getAgent().getOpenaiCompatible()
                        .getOrDefault("deepseek", new com.yomahub.liteflow.property.agent.PlatformCredential())
                        .getApiKey());
    }

    /** 上下文：在 BeforeAll 里把环境变量回写到 LiteflowConfig，避免 properties 留空时无 key 可用。 */
    private void syncEnvKeys() {
        if (ApiKeys.isPresent(openaiKey()))    liteflowConfig.getAgent().getOpenai().setApiKey(openaiKey());
        if (ApiKeys.isPresent(anthropicKey())) liteflowConfig.getAgent().getAnthropic().setApiKey(anthropicKey());
        if (ApiKeys.isPresent(geminiKey()))    liteflowConfig.getAgent().getGemini().setApiKey(geminiKey());
        if (ApiKeys.isPresent(dashscopeKey())) liteflowConfig.getAgent().getDashscope().setApiKey(dashscopeKey());
        if (ApiKeys.isPresent(deepseekKey())) {
            liteflowConfig.getAgent().getOpenaiCompatible()
                    .computeIfAbsent("deepseek", k -> {
                        com.yomahub.liteflow.property.agent.PlatformCredential c =
                                new com.yomahub.liteflow.property.agent.PlatformCredential();
                        c.setBaseUrl("https://api.deepseek.com/v1");
                        return c;
                    })
                    .setApiKey(deepseekKey());
        }
    }

    /* =====================================================================
     *   单平台单 Agent 链路：THEN(prepare, <agent>, recordReply)
     * ===================================================================== */

    @Test
    public void testDeepSeekChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(deepseekKey()),
                "deepseek api-key 未配置，跳过 deepseekChain");
        runChainExpectingReply("deepseekChain", "用一句话总结 ReAct 模式的核心思想。");
    }

    @Test
    public void testOpenAIChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(openaiKey()),
                "openai api-key 未配置，跳过 openaiChain");
        runChainExpectingReply("openaiChain", "用一句话介绍 LiteFlow。");
    }

    @Test
    public void testAnthropicChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(anthropicKey()),
                "anthropic api-key 未配置，跳过 anthropicChain");
        runChainExpectingReply("anthropicChain", "什么是规则引擎？一句话回答。");
    }

    @Test
    public void testDashScopeChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(dashscopeKey()),
                "dashscope api-key 未配置，跳过 dashscopeChain");
        runChainExpectingReply("dashscopeChain", "用一句话介绍通义千问。");
    }

    @Test
    public void testGeminiChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(geminiKey()),
                "gemini api-key 未配置，跳过 geminiChain");
        runChainExpectingReply("geminiChain", "用一句中文介绍 Gemini 模型。");
    }

    /* =====================================================================
     *   自定义工具：mathChain → mathAgent 注册 CalculatorTool
     * ===================================================================== */

    @Test
    public void testMathChainWithCustomTool() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(deepseekKey()),
                "deepseek api-key 未配置，跳过 mathChain（mathAgent 后端用 deepseek）");
        runChainExpectingReply("mathChain",
                "请用 calculator 工具计算 (123 + 456) * 7 - 89，并用一句话给出答案。");
    }

    /* =====================================================================
     *   IF 路由：routerChain，根据 isMath 选 mathAgent 或 deepseekAgent
     * ===================================================================== */

    @Test
    public void testRouterChainPicksMathAgent() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(deepseekKey()),
                "deepseek api-key 未配置，跳过 routerChain");
        runChainExpectingReply("routerChain", "12*34 等于多少？");
    }

    @Test
    public void testRouterChainPicksChatAgent() {
        syncEnvKeys();
        Assumptions.assumeTrue(ApiKeys.isPresent(deepseekKey()),
                "deepseek api-key 未配置，跳过 routerChain");
        runChainExpectingReply("routerChain", "你好，简单介绍下你自己。");
    }

    /* =====================================================================
     *   WHEN 并行：parallelChain 同时跑 deepseek + dashscope，谁后到 recordReply 取谁
     * ===================================================================== */

    @Test
    public void testParallelChain() {
        syncEnvKeys();
        Assumptions.assumeTrue(
                ApiKeys.isPresent(deepseekKey()) && ApiKeys.isPresent(dashscopeKey()),
                "需要同时配置 deepseek 和 dashscope 的 api-key 才能运行 parallelChain");
        runChainExpectingReply("parallelChain", "用一句话介绍 LiteFlow 的应用场景。");
    }
}
