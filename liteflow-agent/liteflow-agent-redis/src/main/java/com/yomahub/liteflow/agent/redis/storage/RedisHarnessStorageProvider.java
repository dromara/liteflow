package com.yomahub.liteflow.agent.redis.storage;

import com.yomahub.liteflow.agent.harness.storage.HarnessStorage;
import com.yomahub.liteflow.agent.harness.storage.HarnessStorageProvider;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;

public final class RedisHarnessStorageProvider implements HarnessStorageProvider {
    @Override public AgentSessionStoreType type() { return AgentSessionStoreType.REDIS; }
    @Override public HarnessStorage open(AgentSessionStoreConfig config) {
        var connection = RedisStorageConnection.open(config.getRedis());
        return new HarnessStorage(new RedisWorkspaceStore(connection,
                RedisStorageConnection.prefix(config.getRedis()) + "workspace:"), connection);
    }
}
