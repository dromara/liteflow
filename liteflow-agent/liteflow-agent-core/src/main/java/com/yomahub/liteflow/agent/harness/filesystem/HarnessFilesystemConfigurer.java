package com.yomahub.liteflow.agent.harness.filesystem;

import io.agentscope.harness.agent.HarnessAgent;

/** Extension contract for applying a Harness filesystem policy to its builder. */
@FunctionalInterface
public interface HarnessFilesystemConfigurer {

    void configure(HarnessAgent.Builder builder, HarnessFilesystemContext context);
}
