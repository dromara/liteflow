package com.yomahub.liteflow.agent.harness.filesystem;

import com.yomahub.liteflow.property.agent.AgentConfig;

import java.nio.file.Path;
import java.time.Duration;

/** LiteFlow-owned limits supplied to a Harness filesystem extension. */
public record HarnessFilesystemContext(
        Path workspaceRoot,
        long maxFileBytes,
        Duration commandTimeout,
        AgentConfig agentConfig) {
}
