package com.yomahub.liteflow.agent.harness.storage;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.*;
import io.agentscope.harness.agent.filesystem.remote.RemoteFilesystem;
import io.agentscope.harness.agent.filesystem.remote.store.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Canonical workspace paths and lossless binary uploads over the shared BaseStore contract. */
public final class StoredWorkspaceFilesystem extends RemoteFilesystem {
    private final BaseStore store;
    private final NamespaceFactory namespace;
    private final String root;
    public StoredWorkspaceFilesystem(BaseStore store, NamespaceFactory namespace, Path root) {
        super(store, namespace); this.store = store; this.namespace = namespace;
        this.root = root.toAbsolutePath().normalize().toString();
    }
    private String path(RuntimeContext rc, String raw) {
        AbstractFilesystem.validatePath(raw);
        String p = Path.of(raw.replace('\\', '/')).normalize().toString();
        if (p.equals(root) || p.equals(".") || p.isEmpty()) return "/";
        if (p.startsWith(root + "/")) p = p.substring(root.length());
        p = p.replaceFirst("^/+", "");
        String user = rc.getUserId();
        if (user != null && !user.isBlank() && p.startsWith(user + "/")
                && ManagedSandboxFilesystem.isManaged(p.substring(user.length() + 1))) p = p.substring(user.length() + 1);
        return "/" + p;
    }
    @Override public ReadResult read(RuntimeContext rc, String p, int offset, int limit) { return super.read(rc, path(rc, p), offset, limit); }
    @Override public WriteResult write(RuntimeContext rc, String p, String value) { return super.write(rc, path(rc, p), value); }
    @Override public EditResult edit(RuntimeContext rc, String p, String old, String value, boolean all) { return super.edit(rc, path(rc, p), old, value, all); }
    @Override public boolean exists(RuntimeContext rc, String p) { return super.exists(rc, path(rc, p)); }
    @Override public WriteResult delete(RuntimeContext rc, String p) { return super.delete(rc, path(rc, p)); }
    @Override public WriteResult move(RuntimeContext rc, String from, String to) { return super.move(rc, path(rc, from), path(rc, to)); }
    @Override public LsResult ls(RuntimeContext rc, String p) { return super.ls(rc, path(rc, p)); }
    @Override public GlobResult glob(RuntimeContext rc, String pattern, String p) { return super.glob(rc, pattern, path(rc, p == null ? "/" : p)); }
    @Override public GrepResult grep(RuntimeContext rc, String pattern, String p, String glob) { return super.grep(rc, pattern, path(rc, p == null ? "/" : p), glob); }
    @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext rc, List<String> paths) { return super.downloadFiles(rc, paths.stream().map(p -> path(rc, p)).toList()); }
    @Override public List<FileUploadResponse> uploadFiles(RuntimeContext rc, List<Map.Entry<String, byte[]>> files) {
        List<FileUploadResponse> result = new ArrayList<>();
        for (var file : files) {
            String content; String encoding;
            try { content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(file.getValue())).toString(); encoding = "utf-8"; }
            catch (CharacterCodingException binary) { content = Base64.getEncoder().encodeToString(file.getValue()); encoding = "base64"; }
            String target = path(rc, file.getKey()); String now = Instant.now().toString();
            store.put(namespace.getNamespace(rc), target, Map.of("content", content, "encoding", encoding, "created_at", now, "modified_at", now));
            result.add(FileUploadResponse.success(target));
        }
        return result;
    }
}
