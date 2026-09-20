package com.yomahub.liteflow.test.agent.real.hitl;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * guide §10 人工确认（HITL）的真实模型验证：
 * ASK 规则触发确认、批准放行、拒绝回传模型继续推理、fail-on-denied-tool。
 */
@TestPropertySource("classpath:/real/hitl/application.properties")
@SpringBootTest(classes = HitlLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.hitl")
public class HitlLiveTest extends RealAgentTestBase {

    @BeforeEach
    public void resetHitlFixture() {
        RealHitlCmp.reset();
        liteflowConfig.getAgent().getHitl().setFailOnDeniedTool(false);
    }

    /** §10：批准 → 工具真实执行，回复基于工具结果。 */
    @Test
    public void approvedToolExecutesAfterConfirmation() {
        RealHitlCmp.decision = true;

        LiteflowResponse response = flowExecutor.execute2Resp("realHitlChain",
                "请帮我给订单 20001 办理退款。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertEquals(1, RealHitlCmp.CONFIRM_LOG.size(),
                "confirmation handler must be invoked exactly once");
        Assertions.assertEquals(1, RealHitlCmp.REFUND_CALLS.get(),
                "approved refund tool must execute exactly once");
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(String.valueOf((Object) data).contains("退款"),
                "reply must report the refund, got: " + data);
    }

    /** §10：拒绝（默认 fail-on-denied-tool=false）→ 工具不执行，模型继续推理，链成功。 */
    @Test
    public void deniedToolFeedsBackAndChainStillSucceeds() {
        RealHitlCmp.decision = false;

        LiteflowResponse response = flowExecutor.execute2Resp("realHitlChain",
                "请帮我给订单 20002 办理退款。");

        Assertions.assertTrue(response.isSuccess(),
                "denied tool with fail-on-denied-tool=false must not fail the chain: "
                        + cause(response));
        Assertions.assertEquals(0, RealHitlCmp.REFUND_CALLS.get(),
                "denied refund tool must never execute");
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) data).isBlank(),
                "model must still produce a final answer");
    }

    /** §10：fail-on-denied-tool=true → 拒绝直接让链失败。 */
    @Test
    public void failOnDeniedToolFailsTheChain() {
        RealHitlCmp.decision = false;
        liteflowConfig.getAgent().getHitl().setFailOnDeniedTool(true);

        LiteflowResponse response = flowExecutor.execute2Resp("realHitlChain",
                "请帮我给订单 20003 办理退款。");

        Assertions.assertFalse(response.isSuccess(),
                "fail-on-denied-tool=true must fail the chain on denial");
        Assertions.assertEquals(0, RealHitlCmp.REFUND_CALLS.get());
        String failure = cause(response);
        Assertions.assertTrue(failure.toLowerCase().contains("permission")
                        || failure.contains("denied") || failure.contains("拒绝"),
                "denial must surface as a permission failure, got: " + failure);
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
