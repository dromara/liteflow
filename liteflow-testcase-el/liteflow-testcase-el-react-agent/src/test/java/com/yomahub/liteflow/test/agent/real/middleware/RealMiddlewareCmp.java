package com.yomahub.liteflow.test.agent.real.middleware;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * guide §8 中间件 的真实模型组件。
 */
final class RealMiddlewareCmp {

    private RealMiddlewareCmp() {
    }

    /** 观测中间件：记录 onAgent 前后置、onReasoning、onModelCall 切点。 */
    static final class ObservingMiddleware implements MiddlewareBase {

        static final List<String> TRACE = new CopyOnWriteArrayList<>();

        @Override
        public int order() {
            return 10;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent, RuntimeContext context, AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            TRACE.add("agent:before");
            return next.apply(input).doOnComplete(() -> TRACE.add("agent:after"));
        }

        @Override
        public Flux<AgentEvent> onReasoning(
                Agent agent, RuntimeContext context, ReasoningInput input,
                Function<ReasoningInput, Flux<AgentEvent>> next) {
            TRACE.add("reasoning");
            return next.apply(input);
        }

        @Override
        public Flux<AgentEvent> onModelCall(
                Agent agent, RuntimeContext context, ModelCallInput input,
                Function<ModelCallInput, Flux<AgentEvent>> next) {
            TRACE.add("model-call");
            return next.apply(input);
        }
    }

    /** 阻断中间件：开关打开时直接返回错误（用例在 finally 中恢复）。 */
    static final class BlockingMiddleware implements MiddlewareBase {

        static volatile boolean enabled = false;

        @Override
        public int order() {
            return 5;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent, RuntimeContext context, AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            if (enabled) {
                return Flux.error(new IllegalStateException("middleware blocked this call"));
            }
            return next.apply(input);
        }
    }

    @Component("realMiddlewareAgent")
    static class MiddlewareAgentCmp extends ReActAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是中间件测试助手，请用一句简短中文回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected List<MiddlewareBase> middlewares() {
            return List.of(new ObservingMiddleware(), new BlockingMiddleware());
        }
    }
}
