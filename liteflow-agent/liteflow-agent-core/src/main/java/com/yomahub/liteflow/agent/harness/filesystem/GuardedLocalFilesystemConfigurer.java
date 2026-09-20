package com.yomahub.liteflow.agent.harness.filesystem;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.bus.WorkspaceAsyncToolRegistry;
import io.agentscope.harness.agent.bus.WorkspaceMessageBus;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.BakedContextFilesystem;

import java.util.Objects;

/** Installs the session-scoped local filesystem and optional host execution. */
public final class GuardedLocalFilesystemConfigurer implements HarnessFilesystemConfigurer {

    private static final String INTERNAL_SESSION_DOMAIN =
            "_internal-";

    private final String internalSessionId;
    private final boolean shellEnabled;

    public GuardedLocalFilesystemConfigurer(String agentNamespace) {
        this(agentNamespace, false);
    }

    public GuardedLocalFilesystemConfigurer(String agentNamespace, boolean shellEnabled) {
        this.shellEnabled = shellEnabled;
        if (agentNamespace == null || agentNamespace.isBlank()) {
            throw new IllegalArgumentException("agentNamespace must not be blank");
        }
        this.internalSessionId = INTERNAL_SESSION_DOMAIN + agentNamespace;
    }

    public static java.nio.file.Path persistentRoot(HarnessFilesystemContext context) {
        var storage = context.agentConfig().getSessionStore();
        String configured = storage.getJsonWorkspaceRoot();
        return (configured == null || configured.isBlank()
                ? java.nio.file.Path.of(storage.getJsonRoot()).resolve("workspace")
                : java.nio.file.Path.of(configured)).toAbsolutePath().normalize().resolve(context.agentConfig().getApplicationName());
    }

    @Override
    public void configure(HarnessAgent.Builder builder, HarnessFilesystemContext context) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(context, "context");
        new GuardedLocalFilesystem(context.workspaceRoot().resolve(context.agentConfig().getApplicationName()));
        AbstractFilesystem filesystem = new GuardedLocalFilesystem(
                persistentRoot(context));
        if (shellEnabled) {
            filesystem = new LocalExecutionFilesystem(filesystem, context.workspaceRoot(),
                    context.agentConfig().getHarness().getShell(),
                    context.agentConfig().getApplicationName());
        }
        RuntimeContext internalContext = RuntimeContext.builder()
                .sessionId(internalSessionId)
                .build();
        AbstractFilesystem internalFilesystem =
                new BakedContextFilesystem(filesystem, internalContext);
        builder.abstractFilesystem(filesystem)
                .transcriptStore(new io.agentscope.harness.agent.transcript.ObjectStoreTranscriptStore(
                        filesystem, RuntimeContext.empty(), ".agentscope/transcripts"))
                .messageBus(new WorkspaceMessageBus(internalFilesystem, ".agentscope/bus"))
                .asyncToolRegistry(new WorkspaceAsyncToolRegistry(
                        internalFilesystem, ".agentscope/bus/async-tools"));
    }
}
