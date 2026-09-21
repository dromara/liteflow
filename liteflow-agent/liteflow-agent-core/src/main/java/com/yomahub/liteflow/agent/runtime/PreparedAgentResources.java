package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.middleware.ModelRoutingMiddleware;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable resources prepared by the provider-neutral agent component layer. */
public record PreparedAgentResources(
        AgentRuntimeOwnership ownership,
        Model defaultModel,
        Model fallbackModel,
        List<Model> managedModels,
        Toolkit toolkit,
        List<AgentSkillRepository> skillRepositories,
        SkillFilter skillFilter,
        boolean dynamicSkillsEnabled,
        List<MiddlewareBase> coreMiddlewares,
        List<MiddlewareBase> userMiddlewares,
        ModelRoutingMiddleware routingMiddleware,
        Map<String, AgentTool> requiredTools,
        int maxIterations,
        ExecutionConfig modelExecutionConfig,
        ExecutionConfig toolExecutionConfig,
        int maxRetries,
        PermissionContextState permissionContext,
        boolean stopOnReject) {

    public PreparedAgentResources {
        Objects.requireNonNull(ownership, "ownership");
        Objects.requireNonNull(defaultModel, "defaultModel");
        Objects.requireNonNull(managedModels, "managedModels");
        Objects.requireNonNull(toolkit, "toolkit");
        Objects.requireNonNull(skillRepositories, "skillRepositories");
        Objects.requireNonNull(skillFilter, "skillFilter");
        Objects.requireNonNull(coreMiddlewares, "coreMiddlewares");
        Objects.requireNonNull(userMiddlewares, "userMiddlewares");
        Objects.requireNonNull(routingMiddleware, "routingMiddleware");
        Objects.requireNonNull(requiredTools, "requiredTools");
    }
}
