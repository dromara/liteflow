package com.yomahub.liteflow.agent.redis.storage;

import com.yomahub.liteflow.agent.harness.storage.HarnessStorage;
import com.yomahub.liteflow.agent.harness.storage.HarnessStorageProvider;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;

public final class RedisHarnessStorageProvider implements HarnessStorageProvider {
    @Override public AgentStateStoreType type() { return AgentStateStoreType.REDIS; }
    @Override public HarnessStorage open(AgentStateStoreConfig config) {
        var connection = RedisStorageConnection.open(config.getRedis());
        return new HarnessStorage(new RedisWorkspaceStore(connection,
                RedisStorageConnection.prefix(config.getRedis()) + "workspace:"), connection);
    }
}
