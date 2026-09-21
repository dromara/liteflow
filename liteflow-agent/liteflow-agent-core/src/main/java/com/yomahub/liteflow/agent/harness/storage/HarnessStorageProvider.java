package com.yomahub.liteflow.agent.harness.storage;

import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;

/** Resolves Harness files and snapshots from the same backend configuration as Agent state. */
public interface HarnessStorageProvider {
    AgentSessionStoreType type();
    HarnessStorage open(AgentSessionStoreConfig config);
}
