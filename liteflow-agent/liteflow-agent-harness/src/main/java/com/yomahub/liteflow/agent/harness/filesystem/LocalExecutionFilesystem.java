package com.yomahub.liteflow.agent.harness.filesystem;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.coding.CommandValidator;
import io.agentscope.core.tool.coding.UnixCommandValidator;
import io.agentscope.core.tool.coding.WindowsCommandValidator;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.local.LocalFilesystemWithShell;
import io.agentscope.harness.agent.filesystem.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Opt-in host execution over the existing session filesystem. Files are materialized for each
 * command in a stable directory under workspace.root; changes are uploaded before returning.
 * The caller must hold the Harness workspace lease. This is not an OS sandbox: commands inherit
 * the application's permissions and environment. Background services are not supported.
 *
 * Extending the SDK's local-shell type also enables its native execute tool and Skill path hints.
 * All file operations remain delegated to the authoritative, identity-aware filesystem.
 */
public final class LocalExecutionFilesystem extends LocalFilesystemWithShell {
    private static final int MAX_OUTPUT_BYTES = 100_000;
    private final AbstractFilesystem files;
    private final long timeoutMillis;

    private final Set<String> whitelist;
    private final CommandValidator validator;

    public LocalExecutionFilesystem(AbstractFilesystem files, Path workspace, ShellConfig shell,
                                    String namespace) {
        super(workspace.resolve(com.yomahub.liteflow.agent.context.AgentInvocationIdentity.requirePathSegment(namespace, "applicationName")));
        new GuardedLocalFilesystem(getCwd());
        this.files = Objects.requireNonNull(files, "files");
        Objects.requireNonNull(shell, "shell");
        if (shell.getMode() != ShellMode.WHITELIST || shell.getWhitelist() == null
                || shell.getWhitelist().isEmpty()
                || shell.getWhitelist().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Local Shell requires WHITELIST mode and a non-empty command whitelist");
        }
        this.whitelist = Set.copyOf(shell.getWhitelist());
        this.validator = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? new WindowsCommandValidator() : new UnixCommandValidator();
        this.timeoutMillis = Objects.requireNonNull(shell.getTimeout(), "timeout").toMillis();
        if (timeoutMillis < 1) throw new IllegalArgumentException("Shell timeout must be positive");
    }

    @Tool(name = "execute", description = "Execute an allowed host Shell/Python/Node command in the current session workspace. "
            + "Files persist between calls. Commands must follow the configured whitelist; use a script for multiple operations.")
    public String executeCommand(
            RuntimeContext context,
            @ToolParam(name = "command", description = "Command to execute") String command,
            @ToolParam(name = "working_directory", description = "Existing directory relative to the session workspace", required = false) String workingDirectory,
            @ToolParam(name = "timeout", description = "Timeout in seconds, capped by the server", required = false) Integer timeoutSeconds) {
        ExecuteResponse result = executeInDirectory(context, command, workingDirectory, timeoutSeconds);
        return "Exit code: " + result.exitCode() + "\n" + result.output()
                + (result.truncated() ? "\n(output was truncated)" : "");
    }

    @Override
    public ExecuteResponse execute(RuntimeContext context, String command, Integer timeoutSeconds) {
        return executeInDirectory(context, command, null, timeoutSeconds);
    }

    private ExecuteResponse executeInDirectory(RuntimeContext context, String command, String workingDirectory, Integer timeoutSeconds) {
        if (context == null || context.getSessionId() == null || context.getSessionId().isBlank()) {
            return new ExecuteResponse("Host execution requires a session identity", 1, false);
        }
        if (command == null || command.isBlank()) return new ExecuteResponse("Command must not be blank", 1, false);
        var policy = validator.validate(command, whitelist);
        if (!policy.isAllowed()) return new ExecuteResponse("Command rejected: " + policy.getReason(), 1, false);
        long timeout = timeoutSeconds == null || timeoutSeconds <= 0
                ? timeoutMillis : Math.min(timeoutMillis, timeoutSeconds.longValue() * 1000);
        try {
            Path directory = executionDirectory(context);
            Path pending = directory.resolve(".agentscope/execution.pending");
            if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Unsynchronized files from a previous command remain in " + directory
                        + "; recover those files before removing " + pending + " and retrying");
            }
            Map<String, byte[]> before = materialize(context, directory);
            Path cwd = commandDirectory(directory, workingDirectory);
            Files.createDirectories(pending.getParent());
            Files.writeString(pending, "Command changes have not been fully synchronized.\n", StandardOpenOption.CREATE_NEW);
            ExecuteResponse result = run(command, cwd, timeout);
            synchronize(context, directory, before);
            Files.delete(pending);
            return result;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return new ExecuteResponse("Host command interrupted; the session directory is retained for recovery", 130, false);
        } catch (Exception failure) {
            return new ExecuteResponse("Host execution or workspace synchronization failed: " + failure.getMessage(), 1, false);
        }
    }

    /** Refresh the same session workspace used by file tools and commands. */
    public void refresh(RuntimeContext context) throws IOException {
        Path directory = executionDirectory(context);
        if (Files.exists(directory.resolve(".agentscope/execution.pending")))
            throw new IOException("Unsynchronized command changes remain in " + directory);
        materialize(context, directory);
    }

    /** Stable for an application/user/session, shared only under the Harness workspace lease. */
    public Path executionDirectory(RuntimeContext context) throws IOException {
        Path root = getCwd().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root)) throw new IOException("Workspace root must not be a symbolic link");
        Path current = root.toRealPath();
        for (String segment : List.of(com.yomahub.liteflow.agent.context.AgentInvocationIdentity.requirePathSegment(context.getSessionId(), "conversationId"))) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                try { Files.createDirectory(current); }
                catch (FileAlreadyExistsException concurrentCreation) { /* Validate it below. */ }
            }
            if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Execution directory must be a real directory: " + current);
            }
        }
        return current;
    }

    private static Path commandDirectory(Path root, String workingDirectory) throws IOException {
        if (workingDirectory == null || workingDirectory.isBlank() || workingDirectory.equals(".")) return root;
        String portable = workingDirectory.replace('\\', '/');
        if (portable.startsWith("/") || portable.startsWith("~") || portable.matches("^[A-Za-z]:.*")) {
            throw new IOException("working_directory must be relative to the session workspace");
        }
        AbstractFilesystem.validatePath(portable);
        Path resolved = root.resolve(portable).normalize();
        if (!resolved.startsWith(root) || !Files.isDirectory(resolved) || !resolved.toRealPath().startsWith(root)) {
            throw new IOException("working_directory must exist within the session workspace");
        }
        return resolved;
    }

    private Map<String, byte[]> materialize(RuntimeContext context, Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                if (Files.isSymbolicLink(path) || (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))) {
                    throw new IOException("Execution cache contains a link or special file: " + path);
                }
            }
        }
        Map<String, byte[]> before = new LinkedHashMap<>();
        Deque<String> pending = new ArrayDeque<>(List.of("."));
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String folder = pending.removeFirst();
            if (!visited.add(folder)) continue;
            LsResult listing = files.ls(context, folder);
            if (!listing.isSuccess()) throw new IOException("Cannot list " + folder + ": " + listing.error());
            for (FileInfo item : listing.entries()) {
                String relative = relative(item.path());
                if (com.yomahub.liteflow.agent.harness.storage.ManagedSandboxFilesystem.isManaged(relative)) continue;
                Path destination = directory.resolve(relative);
                if (item.isDirectory()) {
                    if (Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) Files.delete(destination);
                    Files.createDirectories(destination);
                    pending.addLast(relative);
                } else {
                    var responses = files.downloadFiles(context, List.of(relative));
                    if (responses.size() != 1 || !responses.get(0).isSuccess()) {
                        throw new IOException("Cannot download workspace file: " + relative);
                    }
                    byte[] content = responses.get(0).content();
                    Files.createDirectories(destination.getParent());
                    if (Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
                        try (var stale = Files.walk(destination)) {
                            for (Path path : stale.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                        }
                    }
                    Files.write(destination, content);
                    before.put(relative, digest(content));
                }
            }
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("Execution cache contains a symbolic link: " + path);
                if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                        && !com.yomahub.liteflow.agent.harness.storage.ManagedSandboxFilesystem.isManaged(relative(directory.relativize(path).toString()))
                        && !before.containsKey(relative(directory.relativize(path).toString()))) {
                    Files.delete(path);
                }
            }
        }
        return before;
    }

    private void synchronize(RuntimeContext context, Path directory, Map<String, byte[]> before) throws IOException {
        Map<String, Path> after = new LinkedHashMap<>();
        // Validate the entire output tree before uploading or deleting any authoritative records.
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("Cannot persist a symbolic link: " + directory.relativize(path));
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                String relative = relative(directory.relativize(path).toString());
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Output must be a regular file: " + relative);
                }
                if (!com.yomahub.liteflow.agent.harness.storage.ManagedSandboxFilesystem.isManaged(relative)) after.put(relative, path);
            }
        }
        for (var entry : after.entrySet()) {
            byte[] content = Files.readAllBytes(entry.getValue());
            if (!Arrays.equals(before.get(entry.getKey()), digest(content))) {
                var responses = files.uploadFiles(context, List.of(Map.entry(entry.getKey(), content)));
                if (responses.size() != 1 || !responses.get(0).isSuccess()) {
                    throw new IOException("Cannot persist command output: " + entry.getKey());
                }
            }
        }
        for (String removed : before.keySet()) {
            if (!after.containsKey(removed) && !files.delete(context, removed).isSuccess()) {
                throw new IOException("Cannot delete workspace file: " + removed);
            }
        }
    }

    private String relative(String raw) throws IOException {
        String path = raw.replace('\\', '/');
        String root = getCwd().toAbsolutePath().normalize().toString().replace('\\', '/');
        if (path.startsWith(root + "/")) path = path.substring(root.length() + 1);
        else path = path.replaceFirst("^/+", "");
        if (path.isBlank() || path.matches("^[A-Za-z]:.*")) throw new IOException("Invalid workspace path: " + raw);
        for (String segment : path.split("/")) {
            if (segment.equals("..")) throw new IOException("Invalid workspace path: " + raw);
        }
        Path normalized = Path.of(path).normalize();
        if (normalized.isAbsolute() || normalized.toString().isEmpty()) throw new IOException("Invalid workspace path: " + raw);
        return normalized.toString().replace('\\', '/');
    }

    private static byte[] digest(byte[] value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static ExecuteResponse run(String command, Path directory, long timeout) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        Process process = new ProcessBuilder(windows ? List.of("cmd.exe", "/c", command) : List.of("sh", "-c", command))
                .directory(directory.toFile()).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean[] truncated = {false};
        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    synchronized (output) {
                        int kept = Math.min(count, MAX_OUTPUT_BYTES - output.size());
                        output.write(buffer, 0, kept);
                        if (kept < count) truncated[0] = true;
                    }
                }
            } catch (IOException ignored) { /* Closing a cancelled process also closes this stream. */ }
        }, "liteflow-host-command-output");
        reader.setDaemon(true);
        reader.start();
        Set<ProcessHandle> children = new LinkedHashSet<>();
        boolean finished = false;
        try {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
            do {
                process.descendants().forEach(children::add);
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                finished = process.waitFor(Math.max(1, Math.min(100, TimeUnit.NANOSECONDS.toMillis(remaining))), TimeUnit.MILLISECONDS);
            } while (!finished);
        } finally {
            process.descendants().forEach(children::add);
            for (ProcessHandle child : children) if (child.isAlive()) child.destroyForcibly();
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            reader.join(2000);
            process.getInputStream().close();
        }
        synchronized (output) {
            String text = output.toString(StandardCharsets.UTF_8);
            if (!finished) text += "\nCommand timed out after " + timeout + " ms";
            return new ExecuteResponse(text, finished ? process.exitValue() : 124, truncated[0]);
        }
    }

    private WriteResult refreshed(RuntimeContext context, WriteResult result) {
        if (result.isSuccess()) {
            try { refresh(context); } catch (IOException failure) { return WriteResult.fail(failure.getMessage()); }
        }
        return result;
    }

    @Override public LsResult ls(RuntimeContext rc, String p) { return files.ls(rc, p); }
    @Override public ReadResult read(RuntimeContext rc, String p, int offset, int limit) { return files.read(rc, p, offset, limit); }
    @Override public WriteResult write(RuntimeContext rc, String p, String content) {
        return refreshed(rc, files.write(rc, p, content));
    }
    @Override public EditResult edit(RuntimeContext rc, String p, String old, String content, boolean all) {
        EditResult result = files.edit(rc, p, old, content, all);
        if (result.isSuccess()) {
            try { refresh(rc); } catch (IOException failure) { return EditResult.fail(failure.getMessage()); }
        }
        return result;
    }
    @Override public GrepResult grep(RuntimeContext rc, String pattern, String p, String glob) { return files.grep(rc, pattern, p, glob); }
    @Override public GlobResult glob(RuntimeContext rc, String pattern, String p) { return files.glob(rc, pattern, p); }
    @Override public List<FileUploadResponse> uploadFiles(RuntimeContext rc, List<Map.Entry<String, byte[]>> data) {
        var result = files.uploadFiles(rc, data);
        if (result.stream().allMatch(FileUploadResponse::isSuccess)) {
            try { refresh(rc); }
            catch (IOException failure) { return data.stream().map(file -> FileUploadResponse.fail(file.getKey(), failure.getMessage())).toList(); }
        }
        return result;
    }
    @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext rc, List<String> paths) { return files.downloadFiles(rc, paths); }
    @Override public WriteResult delete(RuntimeContext rc, String p) { return refreshed(rc, files.delete(rc, p)); }
    @Override public WriteResult move(RuntimeContext rc, String from, String to) { return refreshed(rc, files.move(rc, from, to)); }
    @Override public boolean exists(RuntimeContext rc, String p) { return files.exists(rc, p); }
}
