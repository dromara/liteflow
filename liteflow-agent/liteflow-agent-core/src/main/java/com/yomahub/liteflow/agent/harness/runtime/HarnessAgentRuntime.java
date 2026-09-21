package com.yomahub.liteflow.agent.harness.runtime;

import com.yomahub.liteflow.agent.runtime.AgentRuntimeOwnership;
import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.conversation.AgentConversationResourceRegistry;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.harness.sandbox.SessionSandboxRegistry;
import io.agentscope.core.agent.RuntimeContext;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.harness.agent.HarnessAgent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Owns one component-level HarnessAgent and its LiteFlow-managed resources. */
public final class HarnessAgentRuntime implements AutoCloseable {

    private final HarnessAgent agent;
    private final AgentRuntimeOwnership ownership;
    private final List<? extends AutoCloseable> ownedHarnessResources;
    private final SandboxCallGate sandboxCallGate;
    private final PermissionContextState permissionContext;
    private final SessionSandboxRegistry sandboxRegistry;

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources) {
        this(agent, ownership, ownedHarnessResources, null, null);
    }

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources,
            SandboxCallGate sandboxCallGate) {
        this(agent, ownership, ownedHarnessResources, sandboxCallGate, null);
    }

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources,
            SandboxCallGate sandboxCallGate,
            PermissionContextState permissionContext) {
        this(agent, ownership, ownedHarnessResources, sandboxCallGate, permissionContext, null);
    }

    public HarnessAgentRuntime(
            HarnessAgent agent, AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources, SandboxCallGate sandboxCallGate,
            PermissionContextState permissionContext, SessionSandboxRegistry sandboxRegistry) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.ownedHarnessResources = List.copyOf(Objects.requireNonNull(
                ownedHarnessResources, "ownedHarnessResources"));
        this.sandboxCallGate = sandboxCallGate;
        this.permissionContext = permissionContext;
        this.sandboxRegistry = sandboxRegistry;
    }

    public HarnessAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return ownership.stateStore();
    }

    public PermissionContextState permissionContext() {
        return permissionContext;
    }

    /** Runs one full logical invocation under the Docker middleware gate, when configured. */
    public <T> Mono<T> executeSandboxCall(Supplier<Mono<T>> invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return sandboxCallGate == null
                ? Mono.defer(invocation)
                : sandboxCallGate.execute(invocation);
    }

    public <T> Mono<T> executeSandboxCall(AgentInvocationIdentity identity, RuntimeContext context,
                                        Supplier<Mono<T>> invocation) {
        return executeSandboxCall(() -> {
            if (sandboxRegistry != null) return sandboxRegistry.execute(identity, context, invocation);
            if (sandboxCallGate != null) {
                AgentConversationResourceRegistry.release(AgentInvocationKey.workspace(identity));
            }
            return invocation.get();
        });
    }

    @Override
    public void close() {
        if (sandboxCallGate != null) {
            sandboxCallGate.close();
        }
        ownership.close("Harness Agent runtime", agent, ownedHarnessResources);
    }
}
