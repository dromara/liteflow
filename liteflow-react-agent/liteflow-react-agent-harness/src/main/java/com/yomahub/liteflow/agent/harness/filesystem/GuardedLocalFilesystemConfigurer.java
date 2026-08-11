package com.yomahub.liteflow.agent.harness.filesystem;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.bus.WorkspaceAsyncToolRegistry;
import io.agentscope.harness.agent.bus.WorkspaceMessageBus;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.BakedContextFilesystem;

import java.util.Objects;
import java.util.UUID;

/** Installs the trusted-local guarded filesystem without enabling a host shell. */
public final class GuardedLocalFilesystemConfigurer implements HarnessFilesystemConfigurer {

    @Override
    public void configure(HarnessAgent.Builder builder, HarnessFilesystemContext context) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(context, "context");
        boolean autoCreate = Objects.requireNonNull(
                        context.agentConfig().getWorkspace(), "workspace")
                .isAutoCreate();
        GuardedLocalFilesystem filesystem = new GuardedLocalFilesystem(
                context.workspaceRoot(), context.maxFileBytes(), autoCreate);
        RuntimeContext internalContext = RuntimeContext.builder()
                .sessionId("liteflow-harness-internal-" + UUID.randomUUID())
                .build();
        AbstractFilesystem internalFilesystem =
                new BakedContextFilesystem(filesystem, internalContext);
        builder.abstractFilesystem(filesystem)
                .messageBus(new WorkspaceMessageBus(internalFilesystem, ".agentscope/bus"))
                .asyncToolRegistry(new WorkspaceAsyncToolRegistry(
                        internalFilesystem, ".agentscope/bus/async-tools"));
    }
}
