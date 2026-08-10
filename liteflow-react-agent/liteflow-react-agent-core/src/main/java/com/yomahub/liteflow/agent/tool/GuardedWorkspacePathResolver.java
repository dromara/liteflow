package com.yomahub.liteflow.agent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Trusted-local path guard for per-session workspaces.
 *
 * <p>This is not a sandbox. It rejects lexical and currently observable symlink escapes, but a
 * privileged local process can still race filesystem checks and mutations (TOCTOU). Untrusted
 * execution belongs in the Harness container backend.</p>
 */
public final class GuardedWorkspacePathResolver {

    private static final Pattern WINDOWS_ABSOLUTE =
            Pattern.compile("^[A-Za-z]:[\\\\/].*");

    private final Path root;
    private final long maxFileBytes;

    public GuardedWorkspacePathResolver(Path root, long maxFileBytes) {
        this(root, maxFileBytes, true);
    }

    public GuardedWorkspacePathResolver(Path root, long maxFileBytes, boolean autoCreate) {
        Objects.requireNonNull(root, "root");
        if (maxFileBytes <= 0) {
            throw new IllegalArgumentException("maxFileBytes must be positive");
        }
        Path absolute = root.toAbsolutePath().normalize();
        try {
            if (autoCreate) {
                Files.createDirectories(absolute);
            }
            if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("workspace root must be an existing directory");
            }
            this.root = absolute.toRealPath();
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "unable to initialize guarded local workspace root: " + absolute, failure);
        }
        this.maxFileBytes = maxFileBytes;
    }

    public Path sessionRoot(String runtimeSessionId) {
        if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
            throw new IllegalArgumentException("runtimeSessionId must not be blank");
        }
        Path session = root.resolve("session-" + sha256(runtimeSessionId));
        try {
            Files.createDirectories(session);
            rejectExistingSymlinkComponents(session);
            Path real = session.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!real.startsWith(root)) {
                throw new SecurityException("session workspace escapes guarded local root");
            }
            return real;
        } catch (IOException failure) {
            throw new IllegalStateException("unable to initialize session workspace", failure);
        }
    }

    public Path resolve(String runtimeSessionId, String relativePath) {
        validateRelativePath(relativePath);
        Path session = sessionRoot(runtimeSessionId);
        final Path relative;
        try {
            relative = Path.of(relativePath.replace('\\', '/'));
        } catch (InvalidPathException failure) {
            throw new SecurityException("invalid workspace path", failure);
        }
        Path candidate = session.resolve(relative).normalize();
        if (!candidate.startsWith(session)) {
            throw new SecurityException("path escapes session workspace");
        }
        try {
            rejectExistingSymlinkComponents(candidate);
            Path existing = candidate;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing == null || !existing.toRealPath().startsWith(session)) {
                throw new SecurityException("path escapes real session workspace");
            }
            if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                    && !candidate.toRealPath().startsWith(session)) {
                throw new SecurityException("target escapes real session workspace");
            }
            return candidate;
        } catch (IOException failure) {
            throw new SecurityException("unable to validate workspace path", failure);
        }
    }

    public void validateWriteContent(String content) {
        Objects.requireNonNull(content, "content");
        long bytes = utf8Length(content);
        if (bytes > maxFileBytes) {
            throw new IllegalArgumentException(
                    "content exceeds maxFileBytes: " + bytes + " > " + maxFileBytes);
        }
    }

    public long maxFileBytes() {
        return maxFileBytes;
    }

    private static void validateRelativePath(String path) {
        if (path == null) {
            throw new SecurityException("path is null");
        }
        if (path.indexOf('\0') >= 0) {
            throw new SecurityException("NUL is not allowed in workspace paths");
        }
        if (path.startsWith("/")
                || path.startsWith("\\")
                || path.startsWith("//")
                || WINDOWS_ABSOLUTE.matcher(path).matches()) {
            throw new SecurityException("absolute workspace path denied");
        }
        for (String component : path.split("[\\\\/]", -1)) {
            if ("..".equals(component)) {
                throw new SecurityException("parent traversal is denied");
            }
        }
    }

    private static void rejectExistingSymlinkComponents(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path component : absolute) {
            current = current == null ? component : current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.isSymbolicLink(current)) {
                throw new SecurityException("symlink workspace path component denied: " + component);
            }
        }
    }

    private static long utf8Length(String value) {
        long bytes = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current <= 0x7f) {
                bytes++;
            } else if (current <= 0x7ff) {
                bytes += 2;
            } else if (Character.isHighSurrogate(current)
                    && index + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                bytes += 4;
                index++;
            } else if (Character.isSurrogate(current)) {
                bytes++;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
