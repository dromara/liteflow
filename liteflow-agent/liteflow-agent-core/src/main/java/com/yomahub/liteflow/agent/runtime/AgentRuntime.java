package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.util.List;
import java.util.Objects;

/** Owns one stateless AgentScope agent and the resources used to build it. */
public final class AgentRuntime implements AutoCloseable {

    private final ReActAgent agent;
    private final AgentRuntimeOwnership ownership;

    public AgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<? extends Model> ownedModels) {
        this(agent, stateStore, resolvedStateStore, List.of(), List.of(), ownedModels);
    }

    public AgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<McpClientRegistration> mcpClients,
            List<? extends AgentSkillRepository> ownedSkillRepositories,
            List<? extends Model> ownedModels) {
        this(agent, new AgentRuntimeOwnership(
                stateStore,
                resolvedStateStore,
                mcpClients,
                ownedSkillRepositories,
                ownedModels));
    }

    public AgentRuntime(ReActAgent agent, AgentRuntimeOwnership ownership) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
    }

    public ReActAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return ownership.stateStore();
    }

    /** Close order is Agent, owned MCP/repositories/models, then StateStore wrappers. */
    @Override
    public void close() {
        ownership.close("Agent runtime", agent, List.of());
    }
}
