package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.state.AgentState;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Shares Harness' enriched RuntimeContext across every public call in one core continuation. */
final class HarnessRuntimeContextContinuation {

    private final AtomicReference<RuntimeContext> effective = new AtomicReference<>();

    private HarnessRuntimeContextContinuation() {
    }

    static HarnessRuntimeContextContinuation create() {
        return new HarnessRuntimeContextContinuation();
    }

    static MiddlewareBase captureMiddleware(PermissionContextState permissionContext) {
        return new CaptureMiddleware(permissionContext);
    }

    RuntimeContext effectiveOr(RuntimeContext original) {
        RuntimeContext captured = effective.get();
        return captured != null ? captured : original;
    }

    private void capture(RuntimeContext context) {
        effective.compareAndSet(null, context);
    }

    private static final class CaptureMiddleware implements MiddlewareBase {

        private final PermissionContextState permissionContext;

        private CaptureMiddleware(PermissionContextState permissionContext) {
            this.permissionContext = Objects.requireNonNull(permissionContext, "permissionContext");
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent,
                RuntimeContext context,
                AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            return Flux.defer(() -> {
                HarnessRuntimeContextContinuation continuation =
                        context.get(HarnessRuntimeContextContinuation.class);
                if (continuation != null) {
                    continuation.capture(context);
                }
                AgentState state = RuntimeContext.resolveAgentState(context, agent);
                boolean stateBypass = state != null
                        && state.getPermissionContext().getMode() == PermissionMode.BYPASS;
                boolean sessionBypass = agent instanceof ReActAgent reactAgent
                        && reactAgent.getPermissionMode(
                                context.getUserId(), context.getSessionId())
                                == PermissionMode.BYPASS;
                if (stateBypass || sessionBypass) {
                    if (agent instanceof ReActAgent reactAgent) {
                        reactAgent.replacePermissionContext(
                                context.getUserId(), context.getSessionId(), permissionContext);
                    }
                    return Flux.error(new AgentInvocationException(
                            AgentInvocationErrorType.PERMISSION,
                            "Harness permission mode BYPASS is prohibited during invocation"));
                }
                return next.apply(input);
            });
        }
    }
}
