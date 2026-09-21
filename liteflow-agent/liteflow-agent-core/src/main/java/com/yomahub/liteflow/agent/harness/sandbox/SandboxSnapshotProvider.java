package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

/** Supplies a provider-specific snapshot spec without exposing Docker client options. */
@FunctionalInterface
public interface SandboxSnapshotProvider {

    SandboxSnapshotSpec provide(HarnessFilesystemContext context);
}
