package com.yomahub.liteflow.agent.harness.storage;

import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotClient;
import java.io.*;
import java.util.*;

/** Streams archives to bounded records. The caller must hold its workspace lease until stream close.
 * A manifest is published only after every chunk succeeds; normal replacements reclaim old chunks. */
public final class StoreSnapshotClient implements RemoteSnapshotClient {
    private static final int CHUNK_BYTES = 512 * 1024;
    private final BaseStore store;
    private final List<String> namespace;

    public StoreSnapshotClient(BaseStore store, String applicationNamespace) {
        this.store = Objects.requireNonNull(store);
        this.namespace = List.of("liteflow", applicationNamespace, "sandbox-snapshots-v1");
    }
    @Override public void upload(String id, InputStream input) throws IOException {
        String version = UUID.randomUUID().toString();
        List<String> chunks = new ArrayList<>(namespace);
        chunks.add(id); chunks.add(version);
        int count = 0;
        var previous = store.get(namespace, id);
        for (byte[] bytes; (bytes = input.readNBytes(CHUNK_BYTES)).length > 0; count++) {
            store.put(chunks, Integer.toString(count), Map.of("data", Base64.getEncoder().encodeToString(bytes)));
        }
        // A failed/uncertain write must not delete chunks that a committed manifest might reference.
        store.put(namespace, id, Map.of("version", version, "chunks", count));
        // The caller holds the conversation's distributed lease for upload AND download, so no
        // reader can still consume the previous generation here. Garbage cleanup is best effort.
        if (previous != null) {
            List<String> old = new ArrayList<>(namespace);
            old.add(id); old.add((String) previous.value().get("version"));
            for (int i = 0; i < ((Number) previous.value().get("chunks")).intValue(); i++) {
                try { store.delete(old, Integer.toString(i)); }
                catch (RuntimeException cleanup) {
                    org.slf4j.LoggerFactory.getLogger(StoreSnapshotClient.class)
                            .warn("Could not remove obsolete snapshot chunk for {}", id, cleanup);
                }
            }
        }
    }
    @Override public InputStream download(String id) throws IOException {
        var manifest = store.get(namespace, id);
        if (manifest == null) throw new FileNotFoundException("Snapshot not found: " + id);
        List<String> chunks = new ArrayList<>(namespace);
        chunks.add(id); chunks.add((String) manifest.value().get("version"));
        int count = ((Number) manifest.value().get("chunks")).intValue();
        return new InputStream() {
            private int next;
            private ByteArrayInputStream current = new ByteArrayInputStream(new byte[0]);
            private boolean closed;
            @Override public int read() throws IOException {
                byte[] one = new byte[1];
                return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                Objects.checkFromIndexSize(offset, length, bytes.length);
                if (closed) throw new IOException("Snapshot stream is closed");
                if (length == 0) return 0;
                while (current.available() == 0) {
                    if (next >= count) return -1;
                    var chunk = store.get(chunks, Integer.toString(next++));
                    if (chunk == null) throw new IOException("Snapshot chunk is missing: " + id);
                    current = new ByteArrayInputStream(Base64.getDecoder().decode((String) chunk.value().get("data")));
                }
                return current.read(bytes, offset, length);
            }
            @Override public void close() { closed = true; }
        };
    }
    @Override public boolean exists(String id) { return store.get(namespace, id) != null; }
}
