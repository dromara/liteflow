package com.yomahub.liteflow.agent.harness.storage;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

/** Disposable static input staging. Never used to store Agent records or sandbox archives. */
public final class StaticWorkspaceStaging implements AutoCloseable {
    private final Path root;
    private StaticWorkspaceStaging(Path root) { this.root = root; }
    public Path root() { return root; }
    public static StaticWorkspaceStaging create(AgentConfig config) {
        boolean docker = config.getHarness().getFilesystemBackend()
                == com.yomahub.liteflow.property.agent.HarnessFilesystemBackend.DOCKER;
        if (docker) config.getHarness().getDocker().validate();
        StaticWorkspaceStaging staging = null;
        try {
            staging = new StaticWorkspaceStaging(Files.createTempDirectory("liteflow-static-"));
            for (String projected : docker ? config.getHarness().getDocker().getWorkspaceProjectionRoots() : java.util.List.<String>of()) {
                String relative = Path.of(projected).normalize().toString();
                if (relative.isEmpty() || relative.equals(".") || ManagedSandboxFilesystem.isManaged(relative)) {
                    throw new AgentConfigException("Database storage only projects static inputs; invalid projection root: " + projected);
                }
            }
            return staging;
        } catch (IOException | RuntimeException failure) {
            if (staging != null) try { staging.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw new AgentConfigException("Cannot prepare static workspace inputs", failure);
        }
    }
    @Override public void close() throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
