package com.yomahub.liteflow.agent.harness.filesystem;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.filesystem.local.LocalFilesystem;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileInfo;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.GrepMatch;
import io.agentscope.harness.agent.filesystem.model.GrepResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.filesystem.remote.store.NamespaceFactory;
import io.agentscope.harness.agent.filesystem.util.FilesystemUtils;
import io.agentscope.harness.agent.workspace.LocalFsMode;
import io.agentscope.harness.agent.workspace.PathPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Trusted-local Harness filesystem with per-session path confinement and agent-scoped memory.
 *
 * <p>This guard rejects lexical escapes and symlinks observable at operation time. It is not a
 * multi-tenant sandbox: a concurrent privileged host process can still race the validation and
 * the inherited local filesystem operation. Untrusted execution belongs in the container backend.
 */
public final class GuardedLocalFilesystem extends LocalFilesystem {

    private static final NamespaceFactory SESSION_NAMESPACE_FACTORY = sessionNamespaceFactory();

    private final Path root;
    private final Map<String, ReentrantLock> editLocks = new ConcurrentHashMap<>();

    public GuardedLocalFilesystem(Path root) {
        this(prepareRoot(root));
    }

    private GuardedLocalFilesystem(PreparedRoot prepared) {
        super(
                prepared.path(),
                LocalFsMode.UNRESTRICTED,
                PathPolicy.empty(),
                Integer.MAX_VALUE,
                SESSION_NAMESPACE_FACTORY);
        this.root = prepared.path();
    }

    @Override
    protected Path resolvePath(RuntimeContext runtimeContext, String path) {
        validateRelativePath(path);
        Path relative = canonicalRelativePath(path);
        Path sessionRoot = sessionRoot(runtimeContext, relative);

        Path candidate = sessionRoot.resolve(relative).normalize();
        if (!candidate.startsWith(sessionRoot)) {
            throw new SecurityException("path escapes guarded local session workspace");
        }
        try {
            rejectExistingSymlinkComponents(sessionRoot, candidate);
            Path existing = candidate;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing == null || !existing.toRealPath().startsWith(sessionRoot)) {
                throw new SecurityException("path escapes real guarded local session workspace");
            }
            if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                    && !candidate.toRealPath().startsWith(sessionRoot)) {
                throw new SecurityException("target escapes real guarded local session workspace");
            }
            return candidate;
        }
        catch (IOException failure) {
            throw new SecurityException("unable to validate guarded local workspace path", failure);
        }
    }

    @Override
    public LsResult ls(RuntimeContext runtimeContext, String path) {
        LsResult result = super.ls(runtimeContext, path);
        if (!result.isSuccess()
                || result.entries() == null
                || !isAgentMemoryPath(path)
                || !usesAgentMemoryRoot(runtimeContext)) {
            return result;
        }
        return LsResult.success(result.entries().stream()
                .map(entry -> virtualMemoryEntry(runtimeContext, entry))
                .toList());
    }

    @Override
    public GlobResult glob(RuntimeContext runtimeContext, String pattern, String path) {
        GlobResult result = super.glob(runtimeContext, pattern, path);
        if (!result.isSuccess()
                || result.matches() == null
                || !isAgentMemoryPath(path)
                || !usesAgentMemoryRoot(runtimeContext)) {
            return result;
        }
        return GlobResult.success(result.matches().stream()
                .map(entry -> virtualMemoryEntry(runtimeContext, entry))
                .toList());
    }

    @Override
    public GrepResult grep(
            RuntimeContext runtimeContext, String pattern, String path, String glob) {
        GrepResult result = super.grep(runtimeContext, pattern, path, glob);
        if (!result.isSuccess()
                || result.matches() == null
                || !isAgentMemoryPath(path)
                || !usesAgentMemoryRoot(runtimeContext)) {
            return result;
        }
        return GrepResult.success(result.matches().stream()
                .map(match -> new GrepMatch(
                        virtualMemoryPath(runtimeContext, match.path()),
                        match.line(),
                        match.text()))
                .toList());
    }

    @Override
    public EditResult edit(
            RuntimeContext runtimeContext,
            String filePath,
            String oldString,
            String newString,
            boolean replaceAll) {
        Objects.requireNonNull(oldString, "oldString");
        Objects.requireNonNull(newString, "newString");
        Path initial = resolvePath(runtimeContext, filePath);
        String lockKey = initial.toAbsolutePath().normalize().toString();
        ReentrantLock lock = editLocks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        lock.lock();
        try {
            Path resolved = resolvePath(runtimeContext, filePath);
            if (!resolved.equals(initial)) {
                throw new SecurityException("guarded local edit target changed during validation");
            }
            if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
                return EditResult.fail("Error: File '" + filePath + "' not found");
            }
            String content = normalizeNewlines(Files.readString(resolved, StandardCharsets.UTF_8));
            Object[] replacement = FilesystemUtils.performStringReplacement(
                    content, normalizeNewlines(oldString), normalizeNewlines(newString), replaceAll);
            if (replacement.length == 1) {
                return EditResult.fail((String) replacement[0]);
            }
            String edited = (String) replacement[0];
            Files.writeString(resolved, edited, StandardCharsets.UTF_8);
            return EditResult.ok(filePath, (int) replacement[1]);
        }
        catch (IOException failure) {
            return EditResult.fail("Error editing file '" + filePath + "': " + failure.getMessage());
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public WriteResult move(RuntimeContext runtimeContext, String fromPath, String toPath) {
        resolvePath(runtimeContext, fromPath);
        resolvePath(runtimeContext, toPath);
        return super.move(runtimeContext, fromPath, toPath);
    }

    private Path sessionRoot(RuntimeContext runtimeContext, Path relativePath) {
        List<String> namespace = getNamespaceFactory().getNamespace(runtimeContext);
        if (namespace == null || namespace.size() != 1 || namespace.get(0).isBlank()) {
            throw new IllegalArgumentException("runtimeSessionId must not be blank");
        }
        Path namespaceRoot = root;
        if (isAgentMemoryPath(relativePath)
                && usesAgentMemoryRoot(runtimeContext)) {
            LiteFlowAgentContext liteFlow = runtimeContext.get(LiteFlowAgentContext.class);
            namespaceRoot = root.resolve(namespace.get(0)).resolve(".agentscope").resolve("memory").normalize();
            namespace = List.of(com.yomahub.liteflow.agent.context.AgentInvocationIdentity.requirePathSegment(liteFlow.getAgentKey(), "agentKey"));
        }
        Path session = namespaceRoot.resolve(namespace.get(0)).normalize();
        if (!session.startsWith(root)) {
            throw new SecurityException("session workspace escapes guarded local root");
        }
        try {
            requireStableRoot();
            ensureRealDirectory(root, namespaceRoot);
            if (!Files.exists(session, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectory(session);
                }
                catch (FileAlreadyExistsException concurrentCreate) {
                    // Another call for the same session created it; validate below.
                }
            }
            rejectExistingSymlinkComponents(root, session);
            if (!Files.isDirectory(session, LinkOption.NOFOLLOW_LINKS)) {
                throw new SecurityException("session workspace must be a real directory");
            }
            Path realSession = session.toRealPath();
            if (!realSession.startsWith(root)) {
                throw new SecurityException("session workspace escapes real guarded local root");
            }
            return realSession;
        }
        catch (IOException failure) {
            throw new IllegalStateException("unable to initialize guarded local session", failure);
        }
    }

    private FileInfo virtualMemoryEntry(RuntimeContext runtimeContext, FileInfo entry) {
        String path = virtualMemoryPath(runtimeContext, entry.path());
        return entry.isDirectory()
                ? FileInfo.ofDir(path, entry.modifiedAt())
                : FileInfo.ofFile(path, entry.size(), entry.modifiedAt());
    }

    private String virtualMemoryPath(RuntimeContext runtimeContext, String physicalPath) {
        Path memorySession = sessionRoot(runtimeContext, Path.of("memory"));
        Path physical = sessionRoot(runtimeContext, Path.of(".")).resolve(physicalPath.replace('\\', '/')).normalize();
        if (!physical.startsWith(memorySession)) {
            throw new SecurityException("guarded local memory listing escaped its agent session");
        }
        return memorySession.relativize(physical).toString().replace('\\', '/');
    }

    private static void ensureRealDirectory(Path base, Path directory) throws IOException {
        if (!directory.startsWith(base)) {
            throw new SecurityException("guarded local namespace escapes root");
        }
        Path current = base;
        for (Path segment : base.relativize(directory)) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                try { Files.createDirectory(current); }
                catch (FileAlreadyExistsException concurrentCreate) { /* Validate below. */ }
            }
            if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    || !current.toRealPath().startsWith(base)) {
                throw new SecurityException("guarded local namespace must be a real directory");
            }
        }
    }

    private static Path canonicalRelativePath(String path) {
        final Path relative;
        try {
            relative = Path.of(path.replace('\\', '/')).normalize();
        }
        catch (InvalidPathException failure) {
            throw new SecurityException("invalid guarded local workspace path", failure);
        }
        if (relative.isAbsolute()) {
            throw new SecurityException("absolute guarded local workspace path denied");
        }
        return relative;
    }

    private static boolean isAgentMemoryPath(String path) {
        return path != null && isAgentMemoryPath(canonicalRelativePath(path));
    }

    private static boolean isAgentMemoryPath(Path relativePath) {
        String normalized = relativePath.toString().replace('\\', '/');
        return "MEMORY.md".equals(normalized)
                || "memory".equals(normalized)
                || normalized.startsWith("memory/");
    }

    private static boolean usesAgentMemoryRoot(RuntimeContext runtimeContext) {
        LiteFlowAgentContext liteFlow = runtimeContext != null
                ? runtimeContext.get(LiteFlowAgentContext.class)
                : null;
        return liteFlow != null
                && Objects.equals(liteFlow.getRuntimeSessionId(), runtimeContext.getSessionId());
    }

    private void requireStableRoot() throws IOException {
        if (Files.isSymbolicLink(root)
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || !root.toRealPath().equals(root)) {
            throw new SecurityException("guarded local workspace root changed after initialization");
        }
    }

    private static String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static PreparedRoot prepareRoot(Path configuredRoot) {
        Objects.requireNonNull(configuredRoot, "root");
        Path absolute = configuredRoot.toAbsolutePath().normalize();
        try {
            Files.createDirectories(absolute);
            if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("workspace root must be an existing directory");
            }
            return new PreparedRoot(absolute.toRealPath());
        }
        catch (IOException failure) {
            throw new IllegalArgumentException(
                    "unable to initialize guarded local workspace root: " + absolute, failure);
        }
    }

    private static void validateRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new SecurityException("guarded local workspace path must not be blank");
        }
        if (path.indexOf('\0') >= 0) {
            throw new SecurityException("NUL is not allowed in guarded local workspace paths");
        }
        if (path.startsWith("/") || path.startsWith("\\") || hasWindowsDrivePrefix(path)) {
            throw new SecurityException("absolute guarded local workspace path denied");
        }
        for (String segment : path.split("[\\\\/]+")) {
            if ("..".equals(segment)) {
                throw new SecurityException("parent traversal is denied");
            }
            if (".".equals(segment)) {
                continue;
            }
            String canonical = stripWindowsTrailingDotsAndSpaces(segment);
            if (canonical.isEmpty() || ".".equals(canonical) || "..".equals(canonical)) {
                throw new SecurityException("Win32 path alias is denied");
            }
        }
    }

    private static boolean hasWindowsDrivePrefix(String path) {
        if (path.length() < 2 || path.charAt(1) != ':') {
            return false;
        }
        char drive = path.charAt(0);
        return (drive >= 'A' && drive <= 'Z') || (drive >= 'a' && drive <= 'z');
    }

    private static String stripWindowsTrailingDotsAndSpaces(String segment) {
        int end = segment.length();
        while (end > 0) {
            char trailing = segment.charAt(end - 1);
            if (trailing != ' ' && trailing != '.') {
                break;
            }
            end--;
        }
        return segment.substring(0, end);
    }

    private static void rejectExistingSymlinkComponents(Path base, Path candidate) throws IOException {
        if (Files.isSymbolicLink(base)) {
            throw new SecurityException("symlink guarded local path component denied");
        }
        Path current = base;
        for (Path component : base.relativize(candidate)) {
            current = current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.isSymbolicLink(current)) {
                throw new SecurityException(
                        "symlink guarded local path component denied: " + component);
            }
        }
    }

    private static NamespaceFactory sessionNamespaceFactory() {
        NamespaceFactory upstream = IsolationScope.SESSION.toNamespaceFactory();
        return runtimeContext -> {
            List<String> namespace = upstream.getNamespace(runtimeContext);
            if (namespace == null
                    || namespace.size() != 1
                    || namespace.get(0) == null
                    || namespace.get(0).isBlank()) {
                throw new IllegalArgumentException("runtimeSessionId must not be blank");
            }
            return List.of(com.yomahub.liteflow.agent.context.AgentInvocationIdentity.requirePathSegment(namespace.get(0), "conversationId"));
        };
    }

    private record PreparedRoot(Path path) {
    }
}
