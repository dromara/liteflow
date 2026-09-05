package com.yomahub.liteflow.test.agent.feature.middleware;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Component("middlewareAgent")
public class MiddlewareAgentCmp extends AgentComponent {

    private static final List<String> OBSERVATIONS = new CopyOnWriteArrayList<>();

    static void reset() {
        OBSERVATIONS.clear();
    }

    static List<String> observations() {
        return List.copyOf(OBSERVATIONS);
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        return ScriptedChatModel.builder()
                .observeCalls(ignored -> OBSERVATIONS.add("model"))
                .reply("middleware-ok")
                .build();
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        return List.of(new RecordingMiddleware(10), new RecordingMiddleware(20));
    }

    @Override
    protected String systemPrompt() {
        return "offline middleware contract";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return "offline";
    }

    private static final class RecordingMiddleware implements MiddlewareBase {
        private final int order;

        private RecordingMiddleware(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent,
                RuntimeContext context,
                AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            return Flux.defer(() -> {
                OBSERVATIONS.add(order + "-enter");
                return next.apply(input).doOnComplete(() -> OBSERVATIONS.add(order + "-exit"));
            });
        }
    }
}

@Component("failingMiddlewareAgent")
final class FailingMiddlewareAgentCmp extends AgentComponent {

    private static final AtomicInteger MODEL_CALLS = new AtomicInteger();

    static void reset() {
        MODEL_CALLS.set(0);
    }

    static int modelCalls() {
        return MODEL_CALLS.get();
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        return ScriptedChatModel.builder()
                .observeCalls(ignored -> MODEL_CALLS.incrementAndGet())
                .reply("must-not-run")
                .build();
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        return List.of(new MiddlewareBase() {
            @Override
            public int order() {
                return 10;
            }

            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent,
                    RuntimeContext context,
                    AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                return Flux.error(new IllegalStateException("middleware rejected"));
            }
        });
    }

    @Override
    protected String systemPrompt() {
        return "offline middleware failure";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return "offline";
    }
}
