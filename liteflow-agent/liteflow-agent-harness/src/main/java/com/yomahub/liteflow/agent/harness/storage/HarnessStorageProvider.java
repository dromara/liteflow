package com.yomahub.liteflow.agent.harness.storage;

import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;

/** Resolves Harness files and snapshots from the same backend configuration as Agent state. */
public interface HarnessStorageProvider {
    AgentStateStoreType type();
    HarnessStorage open(AgentStateStoreConfig config);
}
