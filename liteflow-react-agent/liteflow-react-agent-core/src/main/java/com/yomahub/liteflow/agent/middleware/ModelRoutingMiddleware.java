package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.Model;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Selects one already-built, runtime-owned model for each model call. */
public final class ModelRoutingMiddleware implements MiddlewareBase {

    private final Model defaultModel;
    private final Set<Model> managedModels;
    private final ModelRouter router;

    public ModelRoutingMiddleware(
            Model defaultModel, List<? extends Model> additionalManagedModels, ModelRouter router) {
        this.defaultModel = Objects.requireNonNull(defaultModel, "defaultModel");
        if (additionalManagedModels == null) {
            throw new AgentConfigException("managed models must not be null");
        }
        Set<Model> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        identities.add(defaultModel);
        for (Model model : additionalManagedModels) {
            if (model == null) {
                throw new AgentConfigException("managed models must not contain null");
            }
            identities.add(model);
        }
        this.managedModels = Collections.unmodifiableSet(identities);
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext runtimeContext,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            if (input == null) {
                return Flux.error(new AgentConfigException("ModelCallInput must not be null"));
            }
            LiteFlowAgentContext context = requireContext(runtimeContext);
            Model selectedModel = router.route(defaultModel, context);
            if (selectedModel == null) {
                return Flux.error(new AgentConfigException("routeModel must not return null"));
            }
            if (!managedModels.contains(selectedModel)) {
                return Flux.error(new AgentConfigException(
                        "routeModel must return a runtime-managed model instance"));
            }
            Model effectiveModel = selectedModel == defaultModel
                    ? input.model()
                    : selectedModel;
            return next.apply(new ModelCallInput(
                    input.messages(), input.tools(), input.options(), effectiveModel));
        });
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for model routing");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException("LiteFlowAgentContext is required for model routing");
        }
        return context;
    }

    @FunctionalInterface
    public interface ModelRouter {
        Model route(Model defaultModel, LiteFlowAgentContext context);
    }
}
