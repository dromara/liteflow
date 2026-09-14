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
        config.getHarness().getDocker().validate();
        StaticWorkspaceStaging staging = null;
        try {
            staging = new StaticWorkspaceStaging(Files.createTempDirectory("liteflow-static-"));
            String configured = config.getWorkspace().getRoot();
            Path source = configured == null || configured.isBlank() ? null : Path.of(configured).toAbsolutePath().normalize();
            if (!config.getHarness().getDocker().isWorkspaceProjectionEnabled()) return staging;
            if (source != null) {
                com.yomahub.liteflow.agent.harness.sandbox.DockerSandboxConfigurer.workspaceProjectionPreflight(
                        new com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext(source,
                                config.getWorkspace().getMaxFileBytes(), config.getShell().getTimeout(), config)).run();
            }
            for (String projected : config.getHarness().getDocker().getWorkspaceProjectionRoots()) {
                String relative = Path.of(projected).normalize().toString();
                if (relative.isEmpty() || relative.equals(".") || ManagedSandboxFilesystem.isManaged(relative)) {
                    throw new AgentConfigException("Database storage only projects static inputs; invalid projection root: " + projected);
                }
                if (source == null || !Files.exists(source.resolve(relative), LinkOption.NOFOLLOW_LINKS)) continue;
                try (var paths = Files.walk(source.resolve(relative))) {
                    for (Path file : paths.toList()) {
                        if (Files.isSymbolicLink(file)) throw new AgentConfigException("Static staging does not accept symlinks: " + file);
                        Path destination = staging.root.resolve(source.relativize(file));
                        if (Files.isDirectory(file)) Files.createDirectories(destination);
                        else if (Files.isRegularFile(file)) {
                            if (Files.size(file) > config.getWorkspace().getMaxFileBytes()) throw new AgentConfigException("Static input exceeds workspace.maxFileBytes: " + file);
                            Files.createDirectories(destination.getParent());
                            Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                        } else throw new AgentConfigException("Static staging accepts only regular files: " + file);
                    }
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
