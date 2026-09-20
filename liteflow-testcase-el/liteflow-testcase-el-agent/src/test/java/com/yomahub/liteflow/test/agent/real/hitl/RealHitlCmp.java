package com.yomahub.liteflow.test.agent.real.hitl;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * guide §10 人工确认（HITL）的真实模型组件：
 * 高危退款工具标记 ASK，确认处理器按静态开关批准或拒绝。
 */
final class RealHitlCmp {

    private RealHitlCmp() {
    }

    /** 确认决定：true 批准 / false 拒绝。 */
    static volatile boolean decision = true;

    static final AtomicInteger REFUND_CALLS = new AtomicInteger();
    static final List<String> CONFIRM_LOG = new CopyOnWriteArrayList<>();

    static void reset() {
        decision = true;
        REFUND_CALLS.set(0);
        CONFIRM_LOG.clear();
    }

    static class RefundTool {

        @Tool(name = "refund_order", description = "对指定订单执行退款，属于高危操作")
        public String refundOrder(
                @ToolParam(name = "orderId", description = "订单号") String orderId) {
            REFUND_CALLS.incrementAndGet();
            return "订单 " + orderId + " 退款 100 元已执行";
        }
    }

    abstract static class AbstractHitlAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是售后助手。处理退款请求时必须调用 refund_order 工具完成，"
                    + "工具结果不可用时如实告知用户原因，一句话中文作答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected List<Object> tools() {
            return List.of(new RefundTool());
        }

        @Override
        protected int maxIterations() {
            return 6;
        }

        @Override
        protected PermissionContextState permissionContext() {
            return PermissionContextState.builder()
                    .addAskRule("refund_order", new PermissionRule(
                            "refund_order", null, PermissionBehavior.ASK, "退款需人工确认"))
                    .build();
        }

        @Override
        protected AgentConfirmationHandler confirmationHandler() {
            return (event, context) -> {
                CONFIRM_LOG.add("confirm:" + event.getToolCalls().size());
                return Mono.just(event.getToolCalls().stream()
                        .map(tool -> new ConfirmResult(decision, tool))
                        .toList());
            };
        }
    }

    @Component("realHitlAgent")
    static class HitlAgentCmp extends AbstractHitlAgent {
    }
}
