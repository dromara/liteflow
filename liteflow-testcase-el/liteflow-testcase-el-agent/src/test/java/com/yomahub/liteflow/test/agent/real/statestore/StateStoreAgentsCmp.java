package com.yomahub.liteflow.test.agent.real.statestore;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.springframework.stereotype.Component;

/**
 * guide §5.3 状态存储测试组件：一个最简记忆 Agent，分别在 Redis / MySQL
 * 后端下验证多轮记忆与落库。
 */
final class StateStoreAgentsCmp {

    private StateStoreAgentsCmp() {
    }

    abstract static class AbstractStateAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是状态存储测试助手。用户让你记住的信息请记在心里；"
                    + "被问起时只依据对话历史作答，找不到时只回复 NO_MEMORY。回答不超过一句话。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }
    }

    @Component("realRedisAgent")
    static class RedisAgentCmp extends AbstractStateAgent {
    }

    @Component("realMysqlAgent")
    static class MysqlAgentCmp extends AbstractStateAgent {
    }
}
