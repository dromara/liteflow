package com.yomahub.liteflow.test.agent.real.reliability;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * guide §14 可靠性与错误处理 的真实模型组件。
 */
final class RealReliabilityAgentsCmp {

    private RealReliabilityAgentsCmp() {
    }

    static final List<String> ROUTE_CHOICES = new CopyOnWriteArrayList<>();

    /** 并发观测：同一身份并发调用应被守卫串行化（max concurrent == 1）。 */
    static final AtomicInteger GUARD_ACTIVE = new AtomicInteger();
    static final AtomicInteger GUARD_MAX_ACTIVE = new AtomicInteger();

    static void reset() {
        ROUTE_CHOICES.clear();
        GUARD_ACTIVE.set(0);
        GUARD_MAX_ACTIVE.set(0);
    }

    private static Model realResolvedModel() {
        return RealAgentTestBase.realModel().resolve(LiteflowConfigHolder.config().getAgent());
    }

    /** 测试内获取 AgentConfig 的最简通道（组件构建期之后调用）。 */
    static final class LiteflowConfigHolder {
        private static volatile com.yomahub.liteflow.property.LiteflowConfig config;

        static com.yomahub.liteflow.property.LiteflowConfig config() {
            return config;
        }

        static void set(com.yomahub.liteflow.property.LiteflowConfig value) {
            config = value;
        }
    }

    /** §14.1：主模型凭据无效，maxRetries=1，回退到真实可用模型。 */
    @Component("realFallbackAgent")
    static class FallbackAgentCmp extends AbstractReliabilityAgent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
            return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                    .apiKey("sk-invalid-fallback-test")
                    .baseUrl(LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_BASE_URL))
                    .temperature(0.1)
                    .maxTokens(256);
        }

        @Override
        protected int maxRetries() {
            return 1;
        }

        @Override
        protected Model fallbackModel() {
            return realResolvedModel();
        }
    }

    /** §14.2：多模型路由，按请求 type 选择并记录。 */
    @Component("realRoutingAgent")
    static class RoutingAgentCmp extends AbstractReliabilityAgent {

        private volatile List<Model> cachedCandidates;

        @Override
        protected List<Model> routingModels() {
            List<Model> candidates = cachedCandidates;
            if (candidates == null) {
                candidates = List.of(realResolvedModel(), realResolvedModel());
                cachedCandidates = candidates;
            }
            return candidates;
        }

        @Override
        protected Model routeModel(Model defaultModel, LiteFlowAgentContext context) {
            boolean hard = false;
            Object request = context.getSlot().getChainReqData(context.getSlot().getChainId());
            if (request instanceof Map<?, ?> map) {
                hard = "hard".equals(map.get("type"));
            }
            List<Model> candidates = routingModels();
            Model chosen = hard ? candidates.get(1) : candidates.get(0);
            ROUTE_CHOICES.add(hard ? "strong" : "cheap");
            return chosen;
        }
    }

    /** 超时 / 并发守卫测试组件。 */
    @Component("realTimeoutAgent")
    static class TimeoutAgentCmp extends AbstractReliabilityAgent {
    }

    @Component("realGuardAgent")
    static class GuardAgentCmp extends AbstractReliabilityAgent {

        @Override
        protected List<MiddlewareBase> middlewares() {
            return List.of(new MiddlewareBase() {
                @Override
                public Flux<AgentEvent> onAgent(
                        Agent agent, RuntimeContext context, AgentInput input,
                        Function<AgentInput, Flux<AgentEvent>> next) {
                    int active = GUARD_ACTIVE.incrementAndGet();
                    GUARD_MAX_ACTIVE.accumulateAndGet(active, Math::max);
                    return next.apply(input)
                            .doFinally(ignored -> GUARD_ACTIVE.decrementAndGet());
                }
            });
        }
    }

    abstract static class AbstractReliabilityAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是可靠性测试助手，请用一句极简中文回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            if (reqData instanceof Map<?, ?> map && map.get("text") != null) {
                return map.get("text").toString();
            }
            return reqData == null ? "" : reqData.toString();
        }
    }
}
