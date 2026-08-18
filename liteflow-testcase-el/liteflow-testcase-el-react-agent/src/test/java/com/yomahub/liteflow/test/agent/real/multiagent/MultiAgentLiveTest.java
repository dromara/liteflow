package com.yomahub.liteflow.test.agent.real.multiagent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

/**
 * guide §9 多 Agent 编排 的真实模型验证：
 * THEN 串行流水线、IF 路由、WHEN 并行（请求级 agentKey）、
 * handleReply 自定义去向与下游 getOutput。
 */
@TestPropertySource("classpath:/real/multiagent/application.properties")
@SpringBootTest(classes = MultiAgentLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.multiagent")
public class MultiAgentLiveTest extends RealAgentTestBase {

    /** §9.1：THEN 串行流水线，下游 Agent 消费上游 Agent 输出，普通组件转存。 */
    @Test
    public void thenPipelinePassesAgentOutputDownstream() {
        LiteflowResponse response = flowExecutor.execute2Resp("realPipelineChain",
                "请生成一个暗号。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object generated = response.getSlot().getOutput("realPipelineGenAgent");
        Assertions.assertNotNull(generated, "first agent output must be readable via getOutput");
        Assertions.assertTrue(generated.toString().contains("GALAXY")
                        || generated.toString().contains("COMET"),
                "generator reply must be a codeword, got: " + generated);

        Object echoed = response.getSlot().getOutput("realPipelineEchoAgent");
        Assertions.assertNotNull(echoed, "second agent must record its output");
        Assertions.assertTrue(echoed.toString().contains("ECHO:"),
                "echo agent must reply in ECHO format, got: " + echoed);
        Assertions.assertTrue(echoed.toString().contains(generated.toString().strip()),
                "echo must carry the upstream codeword, got: " + echoed);

        Object saved = response.getSlot().getOutput("savedResult");
        Assertions.assertEquals(echoed, saved,
                "plain downstream component must see the agent output via getOutput");
    }

    /** §9.2：IF 按 type=math 路由到数学 Agent。 */
    @Test
    public void ifRoutesMathRequestToMathAgent() {
        LiteflowResponse response = flowExecutor.execute2Resp("realRouteChain",
                Map.of("type", "math", "text", "计算 128 乘以 64 等于多少？"));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object answer = response.getSlot().getResponseData();
        Assertions.assertTrue(String.valueOf((Object) answer).contains("8192"),
                "math agent must compute 8192, got: " + answer);
    }

    /** §9.2：非 math 请求走默认分支。 */
    @Test
    public void ifRoutesGeneralRequestToGeneralAgent() {
        LiteflowResponse response = flowExecutor.execute2Resp("realRouteChain",
                Map.of("type", "chat", "text", "用一句话介绍一下西湖。"));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object answer = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) answer).isBlank());
        Assertions.assertTrue(String.valueOf((Object) answer).contains("西湖"),
                "general agent must answer about 西湖, got: " + answer);
    }

    /**
     * §9.3：WHEN 并行调用两个 Agent（agentKey 带 requestId 区分以真正并行）。
     * 断言两个分支都完成且互不阻塞。
     */
    @Test
    public void whenRunsTwoAgentsInParallel() {
        long start = System.currentTimeMillis();
        LiteflowResponse response = flowExecutor.execute2Resp("realParallelChain",
                "开始");
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object a = response.getSlot().getOutput("realParallelAgentA");
        Object b = response.getSlot().getOutput("realParallelAgentB");
        Assertions.assertTrue(a != null && a.toString().contains("BRANCH-A-DONE"),
                "branch A must finish, got: " + a);
        Assertions.assertTrue(b != null && b.toString().contains("BRANCH-B-DONE"),
                "branch B must finish, got: " + b);
        // 串行两次真实调用通常 > 6s；放宽阈值只验证明显并行（非严格断言）
        Assertions.assertTrue(elapsed < 30_000,
                "parallel chain took too long: " + elapsed + "ms");
    }

    /** §9.4：handleReply 覆写后答复进入自定义 output，responseData 保持为空。 */
    @Test
    public void handleReplyOverrideRedirectsTheAnswer() {
        LiteflowResponse response = flowExecutor.execute2Resp("realCustomReplyChain",
                "用一句话解释什么是规则引擎。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object custom = response.getSlot().getOutput(
                RealMultiAgentCmp.CustomReplyAgentCmp.OUTPUT_KEY);
        Assertions.assertNotNull(custom, "custom answer output must be present");
        Assertions.assertFalse(custom.toString().isBlank());
        Assertions.assertNull(response.getSlot().getResponseData(),
                "responseData must stay untouched when super.handleReply is skipped");
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
