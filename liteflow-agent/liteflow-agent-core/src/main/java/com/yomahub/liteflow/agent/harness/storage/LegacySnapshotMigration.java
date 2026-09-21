package com.yomahub.liteflow.agent.harness.storage;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshot;
import org.apache.commons.compress.archivers.tar.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.*;

/** One-time import of old local archives; originals are never modified or deleted. */
public final class LegacySnapshotMigration {
    private LegacySnapshotMigration() { }
    public static void migrate(SandboxSnapshot source, SandboxSnapshot destination,
                               AbstractFilesystem records, RuntimeContext context) throws Exception {
        // A previous attempt may have committed the import before its metadata write failed.
        if (destination.isRestorable()) return;
        if (!source.isRestorable()) throw new IOException(
                "Legacy archive is unavailable on this node: " + source.getId() + ". Run migration on the node holding the archive first.");
        PipedInputStream input = new PipedInputStream(64 * 1024);
        PipedOutputStream output = new PipedOutputStream(input);
        FutureTask<Void> filter = new FutureTask<>(() -> {
            try (var archive = new TarArchiveInputStream(source.restore());
                 var business = new TarArchiveOutputStream(output)) {
                business.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                business.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                for (TarArchiveEntry entry; (entry = archive.getNextEntry()) != null;) {
                    AbstractFilesystem.validatePath(entry.getName());
                    if (Path.of(entry.getName()).isAbsolute()) throw new IOException("Absolute path in legacy archive");
                    String path = Path.of(entry.getName()).normalize().toString();
                    if (ManagedSandboxFilesystem.isManaged(path)) {
                        if (entry.isDirectory()) continue;
                        if (!entry.isFile()) {
                            throw new IOException("Unsupported Agent record in legacy archive: " + path);
                        }
                        byte[] bytes = archive.readAllBytes();
                        if (bytes.length != entry.getSize()) throw new EOFException("Incomplete Agent record: " + path);
                        if (!records.exists(context, path)) {
                            var result = records.write(context, path, new String(bytes, StandardCharsets.UTF_8));
                            if (!result.isSuccess() && !records.exists(context, path)) throw new IOException(result.error());
                        }
                    } else {
                        business.putArchiveEntry(entry);
                        archive.transferTo(business);
                        business.closeArchiveEntry();
                    }
                }
                business.finish();
            }
            return null;
        });
        Thread worker = new Thread(filter, "liteflow-snapshot-import");
        worker.setDaemon(true); worker.start();
        try (input) {
            destination.persist(new FilterInputStream(input) {
                private int checked(int count) throws IOException {
                    if (count != -1) return count;
                    try { filter.get(); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IOException("Snapshot import interrupted", failure); }
                    catch (ExecutionException failure) { throw new IOException("Snapshot import failed", failure.getCause()); }
                    return -1;
                }
                @Override public int read() throws IOException { return checked(super.read()); }
                @Override public int read(byte[] b, int off, int len) throws IOException { return checked(in.read(b, off, len)); }
            });
        } finally {
            input.close();
            if (!filter.isDone()) filter.cancel(true);
            worker.join(5000);
        }
    }
}
