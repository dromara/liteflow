package com.yomahub.liteflow.agent.harness.storage;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.*;
import io.agentscope.harness.agent.filesystem.remote.RemoteFilesystem;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.filesystem.sandbox.SandboxBackedFilesystem;
import io.agentscope.harness.agent.filesystem.sandbox.BaseSandboxFilesystem;
import java.nio.file.Path;
import java.util.*;

/**
 * Routes Harness-owned records to shared storage and business files to Docker. The shell sees
 * only the business workspace; memory/session tools use this filesystem and never materialize
 * their records in the container. There is deliberately no local fallback for managed paths.
 */
public final class ManagedSandboxFilesystem extends BaseSandboxFilesystem {
    private final SandboxBackedFilesystem business = new SandboxBackedFilesystem();
    private final RemoteFilesystem records;
    private final String sourceRoot;
    private final String sandboxRoot;
    private final long maxFileBytes;

    public ManagedSandboxFilesystem(BaseStore store, String namespace, String agentNamespace,
                                    Path sourceRoot, String sandboxRoot) {
        this(store, namespace, agentNamespace, sourceRoot, sandboxRoot, 10L * 1024 * 1024);
    }
    public ManagedSandboxFilesystem(BaseStore store, String namespace, String agentNamespace,
                                    Path sourceRoot, String sandboxRoot, long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
        this.sourceRoot = sourceRoot.toAbsolutePath().normalize().toString();
        this.sandboxRoot = Path.of(sandboxRoot).normalize().toString();
        this.records = new StoredWorkspaceFilesystem(store, rc -> List.of("liteflow", namespace, "workspace-v1",
                textOr(rc.getUserId(), "_internal"), textOr(rc.getSessionId(), agentNamespace)), sourceRoot);
    }
    public long maxFileBytes() { return maxFileBytes; }
    private static String textOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    public static boolean isManaged(String path) {
        String p = path.replace('\\', '/').replaceFirst("^/+", "");
        return p.equals("plans") || p.startsWith("plans/") || p.equals("MEMORY.md") || p.equals("memory") || p.startsWith("memory/")
                || p.equals("agents") || p.startsWith("agents/")
                || p.equals(".agentscope") || p.startsWith(".agentscope/");
    }
    // Keep this composite router out of SandboxAware: AgentScope would otherwise replace it
    // with a PinnedSandboxFilesystem for background transcript writes, bypassing records.
    public void setSandbox(Sandbox sandbox) { business.setSandbox(sandbox); }
    public Sandbox getSandbox() { return business.getSandbox(); }
    @Override public String id() { return business.id(); }
    @Override public ExecuteResponse execute(RuntimeContext rc, String command, Integer timeoutSeconds) {
        return business.execute(rc, command, timeoutSeconds);
    }

    private String logical(RuntimeContext rc, String path) {
        AbstractFilesystem.validatePath(path);
        String p = Path.of(path.replace('\\', '/')).normalize().toString().replace('\\', '/');
        if (p.equals(sourceRoot) || p.equals(sandboxRoot) || p.equals(".") || p.isEmpty()) return "/";
        if (p.startsWith(sourceRoot + "/")) p = p.substring(sourceRoot.length());
        else if (p.startsWith(sandboxRoot + "/")) p = p.substring(sandboxRoot.length());
        p = p.replaceFirst("^/+", "");
        // WorkspaceManager can pass either raw relative paths or user/session-prefixed paths.
        for (String prefix : List.of(textOr(rc.getUserId(), ""),
                textOr(rc.getUserId(), "") + "/" + textOr(rc.getSessionId(), ""))) {
            if (!prefix.isBlank() && p.startsWith(prefix + "/") && isManaged(p.substring(prefix.length() + 1))) {
                p = p.substring(prefix.length() + 1); break;
            }
        }
        return "/" + p;
    }
    private String businessPath(String logical) { return sandboxRoot + (logical.equals("/") ? "" : logical); }
    private record Target(AbstractFilesystem filesystem, String path) {}
    private Target target(RuntimeContext rc, String path) {
        String logical = logical(rc, path);
        return isManaged(logical) ? new Target(records, logical) : new Target(business, businessPath(logical));
    }
    @Override public ReadResult read(RuntimeContext rc, String path, int offset, int limit) {
        var t = target(rc, path);
        if (t.filesystem == business && getSandbox() == null) return ReadResult.fail("No active business workspace");
        return t.filesystem.read(rc, t.path, offset, limit);
    }
    @Override public WriteResult write(RuntimeContext rc, String path, String content) {
        var t = target(rc, path); return t.filesystem.write(rc, t.path, content);
    }
    @Override public EditResult edit(RuntimeContext rc, String path, String oldText, String newText, boolean all) {
        var t = target(rc, path); return t.filesystem.edit(rc, t.path, oldText, newText, all);
    }
    @Override public boolean exists(RuntimeContext rc, String path) {
        var t = target(rc, path);
        if (t.filesystem == business && getSandbox() == null) return false;
        return t.filesystem.exists(rc, t.path);
    }
    @Override public WriteResult delete(RuntimeContext rc, String path) {
        if (logical(rc, path).equals("/")) return WriteResult.fail("Delete a specific workspace path");
        var t = target(rc, path); return t.filesystem.delete(rc, t.path);
    }
    @Override public WriteResult move(RuntimeContext rc, String from, String to) {
        var source = target(rc, from); var destination = target(rc, to);
        if (source.filesystem != destination.filesystem) return WriteResult.fail("Cannot move Agent records into or out of the business workspace");
        return source.filesystem.move(rc, source.path, destination.path);
    }
    @Override public List<FileUploadResponse> uploadFiles(RuntimeContext rc, List<Map.Entry<String, byte[]>> files) {
        List<FileUploadResponse> result = new ArrayList<>();
        for (var file : files) {
            var t = target(rc, file.getKey());
            result.addAll(t.filesystem.uploadFiles(rc, List.of(Map.entry(t.path, file.getValue()))));
        }
        return result;
    }
    @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext rc, List<String> paths) {
        List<FileDownloadResponse> result = new ArrayList<>();
        for (String path : paths) {
            var t = target(rc, path); result.addAll(t.filesystem.downloadFiles(rc, List.of(t.path)));
        }
        return result;
    }
    @Override public LsResult ls(RuntimeContext rc, String path) {
        var t = target(rc, path);
        if (!logical(rc, path).equals("/")) {
            if (t.filesystem == business && getSandbox() == null) return LsResult.success(List.of());
            return t.filesystem.ls(rc, t.path);
        }
        List<FileInfo> entries = new ArrayList<>(records.ls(rc, "/").entries());
        if (getSandbox() != null) {
            var local = business.ls(rc, sandboxRoot);
            if (!local.isSuccess()) return local;
            entries.addAll(local.entries());
        }
        return LsResult.success(entries);
    }
    @Override public GlobResult glob(RuntimeContext rc, String pattern, String path) {
        path = path == null || path.isBlank() ? "/" : path;
        var t = target(rc, path);
        if (!logical(rc, path).equals("/")) {
            if (t.filesystem == business && getSandbox() == null) return GlobResult.success(List.of());
            return t.filesystem.glob(rc, pattern, t.path);
        }
        var remote = records.glob(rc, pattern, "/");
        if (!remote.isSuccess() || getSandbox() == null) return remote;
        var local = business.glob(rc, pattern, sandboxRoot);
        if (!local.isSuccess()) return local;
        List<FileInfo> matches = new ArrayList<>(remote.matches()); matches.addAll(local.matches());
        return GlobResult.success(matches);
    }
    @Override public GrepResult grep(RuntimeContext rc, String pattern, String path, String glob) {
        path = path == null || path.isBlank() ? "/" : path;
        var t = target(rc, path);
        if (!logical(rc, path).equals("/")) return t.filesystem.grep(rc, pattern, t.path, glob);
        var remote = records.grep(rc, pattern, "/", glob);
        if (!remote.isSuccess() || getSandbox() == null) return remote;
        var local = business.grep(rc, pattern, sandboxRoot, glob);
        if (!local.isSuccess()) return local;
        List<GrepMatch> matches = new ArrayList<>(remote.matches()); matches.addAll(local.matches());
        return GrepResult.success(matches);
    }
}
