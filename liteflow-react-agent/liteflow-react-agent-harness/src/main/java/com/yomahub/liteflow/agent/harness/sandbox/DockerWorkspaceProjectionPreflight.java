package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.exception.AgentConfigException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Rejects link-based host workspace projection before each Docker sandbox acquisition. */
final class DockerWorkspaceProjectionPreflight implements Runnable {

    private static final LinkOption[] NOFOLLOW = {LinkOption.NOFOLLOW_LINKS};

    private final Path sourceRoot;
    private final boolean enabled;
    private final List<String> includeRoots;

    DockerWorkspaceProjectionPreflight(
            Path sourceRoot, boolean enabled, List<String> includeRoots) {
        this.sourceRoot = sourceRoot.toAbsolutePath().normalize();
        this.enabled = enabled;
        this.includeRoots = List.copyOf(includeRoots);
    }

    @Override
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            requireNotLink(sourceRoot);
            if (!Files.isDirectory(sourceRoot, NOFOLLOW)) {
                throw unsafe("source workspace must be an existing directory", sourceRoot);
            }
            Path realRoot = sourceRoot.toRealPath();
            for (String includeRoot : includeRoots) {
                validateIncludeRoot(includeRoot, realRoot);
            }
        }
        catch (IOException | UncheckedIOException failure) {
            throw new AgentConfigException(
                    "Docker workspace projection preflight failed for " + sourceRoot,
                    failure);
        }
    }

    private void validateIncludeRoot(String includeRoot, Path realRoot) throws IOException {
        Path resolved = sourceRoot.resolve(includeRoot).normalize();
        if (!resolved.startsWith(sourceRoot)) {
            throw unsafe("include root escapes the source workspace", resolved);
        }
        validateExistingComponents(resolved);
        if (!Files.exists(resolved, NOFOLLOW)) {
            return;
        }
        validateRealContainment(resolved, realRoot);
        if (Files.isDirectory(resolved, NOFOLLOW)) {
            try (var paths = Files.walk(resolved)) {
                paths.forEach(path -> validateVisitedPath(path, realRoot));
            }
        }
    }

    private void validateExistingComponents(Path resolved) {
        Path current = sourceRoot;
        for (Path segment : sourceRoot.relativize(resolved)) {
            current = current.resolve(segment);
            requireNotLink(current);
            if (!Files.exists(current, NOFOLLOW)) {
                return;
            }
        }
    }

    private void validateVisitedPath(Path path, Path realRoot) {
        requireNotLink(path);
        try {
            validateRealContainment(path, realRoot);
        }
        catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void validateRealContainment(Path path, Path realRoot) throws IOException {
        Path realPath = path.toRealPath();
        if (!realPath.startsWith(realRoot)) {
            throw unsafe("resolved path escapes the source workspace", path);
        }
    }

    private static void requireNotLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            throw unsafe("symbolic links are not allowed", path);
        }
    }

    private static AgentConfigException unsafe(String reason, Path path) {
        return new AgentConfigException(
                "Docker workspace projection " + reason + ": " + path);
    }
}
