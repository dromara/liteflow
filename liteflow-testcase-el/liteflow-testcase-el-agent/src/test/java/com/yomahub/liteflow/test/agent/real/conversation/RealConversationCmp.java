package com.yomahub.liteflow.test.agent.real.conversation;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.springframework.stereotype.Component;

/**
 * guide §5 多轮对话组件：secretAgent（默认会话身份）、fixedCidAgent（覆写
 * resolveConversationId）、isolatedAgentA/B（验证 agentKey 记忆隔离）。
 */
public final class RealConversationCmp {

    private RealConversationCmp() {
    }

    abstract static class AbstractMemoryAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是记忆测试助手。用户让你记住的信息请记在心里；"
                    + "被问起时只依据对话历史中的信息作答，历史中找不到时只回复 NO_MEMORY。"
                    + "回答尽量不超过一句话。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }
    }

    /** 普通 Agent：conversationId 由 ExecuteOption / Map / 自动生成提供。 */
    @Component("realSecretAgent")
    public static class SecretAgentCmp extends AbstractMemoryAgent {
    }

    /** §5.2 方式四：组件内覆写 resolveConversationId。 */
    @Component("realFixedCidAgent")
    public static class FixedCidAgentCmp extends AbstractMemoryAgent {

        static volatile String FIXED_CID = "";

        @Override
        protected String resolveConversationId(com.yomahub.liteflow.slot.Slot slot) {
            return FIXED_CID;
        }
    }

    /** §5.1 agentKey 隔离：两个组件共享 conversationId 但记忆各自独立。 */
    @Component("realIsolatedAgentA")
    public static class IsolatedAgentACmp extends AbstractMemoryAgent {
    }

    @Component("realIsolatedAgentB")
    public static class IsolatedAgentBCmp extends AbstractMemoryAgent {

        @Override
        protected String systemPrompt() {
            return "你是记忆隔离测试助手。只依据你自己对话历史中的信息作答，"
                    + "历史中找不到时只回复 NO_MEMORY，不要猜测。";
        }
    }
}
