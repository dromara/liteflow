package com.yomahub.liteflow.test.agent.real.basic;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.model.Model;

/**
 * §2 快速开始 / §3 接入模型平台 的一组真实模型组件。
 *
 * <ul>
 *   <li>{@code realChatAgent}：指南 §2.3 的最小示例（三抽象方法）；</li>
 *   <li>{@code realParamsAgent}：§3.2 通用链式参数（temperature/topP/maxTokens/
 *       additionalHeader/additionalBodyParam）；</li>
 *   <li>{@code realExplicitCredentialAgent}：§3.1 代码里显式 apiKey/baseUrl 优先于配置；</li>
 *   <li>{@code realBuildModelAgent}：§3.4 buildModel 逃生舱装配真实模型。</li>
 * </ul>
 */
public final class BasicAgentsCmp {

    private BasicAgentsCmp() {
    }

    public static class AbstractRealAgent extends ReActAgentComponent {

        @Override
        protected ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是 LiteFlow ReAct Agent 的真实模型测试助手。除非用户另有要求，"
                    + "请用不超过两句的简短中文回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }
    }

    /** §2.3 最小组件：bean 名 realChatAgent。 */
    @org.springframework.stereotype.Component("realChatAgent")
    public static class ChatAgentCmp extends AbstractRealAgent {
    }

    /** §3.2 全量通用参数：bean 名 realParamsAgent。 */
    @org.springframework.stereotype.Component("realParamsAgent")
    public static class ParamsAgentCmp extends AbstractRealAgent {

        @Override
        protected ModelSpec<?> model() {
            String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
            return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                    .temperature(0.2)
                    .topP(0.9)
                    .maxTokens(256)
                    .additionalHeader("X-LiteFlow-Test", "real-params")
                    .additionalBodyParam("user", "liteflow-real-test");
        }
    }

    /**
     * §3.1 凭据优先级：代码显式设置 apiKey/baseUrl 应覆盖配置文件值。
     * 测试会把配置段改成无效值，仅当显式值生效时调用才能成功。
     */
    @org.springframework.stereotype.Component("realExplicitCredentialAgent")
    public static class ExplicitCredentialAgentCmp extends AbstractRealAgent {

        @Override
        protected ModelSpec<?> model() {
            String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
            return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                    .apiKey(LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_API_KEY))
                    .baseUrl(LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_BASE_URL))
                    .temperature(0.1)
                    .maxTokens(256);
        }
    }

    /** §3.4 逃生舱：不覆写 model() 的解析路径，直接 buildModel 返回真实模型实例。 */
    @org.springframework.stereotype.Component("realBuildModelAgent")
    public static class BuildModelAgentCmp extends AbstractRealAgent {

        @Override
        protected ModelSpec<?> model() {
            throw new UnsupportedOperationException("use buildModel");
        }

        @Override
        protected Model buildModel() {
            return RealAgentTestBase.realModel().resolve(agentConfig());
        }
    }
}
