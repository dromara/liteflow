package com.yomahub.liteflow.test.agent.real.multiagent;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

/**
 * guide §9 多 Agent 编排 的真实模型组件：
 * 串行流水线、IF 路由、WHEN 并行、handleReply 自定义去向、下游结果转存。
 */
final class RealMultiAgentCmp {

    private RealMultiAgentCmp() {
    }

    /** 生成一个 6 位随机风格暗号的 Agent（流水线首站）。 */
    @Component("realPipelineGenAgent")
    static class PipelineGenAgentCmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是暗号生成器。请只输出一个大写单词 GALAXY 或 COMET，不要输出其它内容。";
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            super.handleReply(reply, context);
            context.getSlot().setOutput(getNodeId(), reply.getTextContent());
        }
    }

    /** 依赖上游输出的第二站 Agent。 */
    @Component("realPipelineEchoAgent")
    static class PipelineEchoAgentCmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是复读机。请把【上游】标记后的内容原样复读一遍，格式：ECHO:<内容>。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object upstream = getSlot().getOutput("realPipelineGenAgent");
            return "【上游】" + (upstream == null ? "" : upstream.toString());
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            super.handleReply(reply, context);
            context.getSlot().setOutput(getNodeId(), reply.getTextContent());
        }
    }

    /** 数学分支 Agent（IF 路由）。 */
    @Component("realMathAgent")
    static class MathAgentCmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是计算助手。只输出计算结果本身，不要解释。";
        }
    }

    /** 通用分支 Agent（IF 路由默认分支）。 */
    @Component("realGeneralAgent")
    static class GeneralAgentCmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是通用助手，请用一句话中文回答。";
        }
    }

    /** 路由条件：type=math 走数学 Agent。 */
    @Component("realIsMathRequest")
    static class IsMathRequestCmp extends com.yomahub.liteflow.core.NodeBooleanComponent {

        @Override
        public boolean processBoolean() {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            if (reqData instanceof java.util.Map<?, ?> map) {
                return "math".equals(map.get("type"));
            }
            return false;
        }
    }

    /** WHEN 并行 Agent A：agentKey 带 requestId 实现真正并行。 */
    @Component("realParallelAgentA")
    static class ParallelAgentACmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是并行测试助手 A，只输出：BRANCH-A-DONE";
        }

        @Override
        protected String agentKey() {
            return "parallelA__" + getSlot().getRequestId();
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            super.handleReply(reply, context);
            context.getSlot().setOutput(getNodeId(), reply.getTextContent());
        }
    }

    /** WHEN 并行 Agent B。 */
    @Component("realParallelAgentB")
    static class ParallelAgentBCmp extends AbstractRealAgent {

        @Override
        protected String systemPrompt() {
            return "你是并行测试助手 B，只输出：BRANCH-B-DONE";
        }

        @Override
        protected String agentKey() {
            return "parallelB__" + getSlot().getRequestId();
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            super.handleReply(reply, context);
            context.getSlot().setOutput(getNodeId(), reply.getTextContent());
        }
    }

    /** §9.4 handleReply 覆写：答复写入自定义 output 而不是 responseData。 */
    @Component("realCustomReplyAgent")
    static class CustomReplyAgentCmp extends AbstractRealAgent {

        static final String OUTPUT_KEY = "customAnswer";

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            context.getSlot().setOutput(OUTPUT_KEY, reply.getTextContent());
            // 不调用 super：不再写入 responseData
        }
    }

    /** §9.4 下游普通组件：转存 Agent 输出。 */
    @Component("realSaveResultCmp")
    static class SaveResultCmp extends NodeComponent {

        @Override
        public void process() {
            Object upstream = getSlot().getOutput("realPipelineEchoAgent");
            getSlot().setOutput("savedResult", upstream == null ? "" : upstream.toString());
        }
    }

    abstract static class AbstractRealAgent extends ReActAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是多 Agent 编排测试助手，请用简短中文回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            if (reqData instanceof java.util.Map<?, ?> map && map.get("text") != null) {
                return map.get("text").toString();
            }
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected int maxIterations() {
            return 4;
        }
    }
}
