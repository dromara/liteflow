package com.yomahub.liteflow.agent.mysql.storage;

import com.yomahub.liteflow.agent.harness.storage.HarnessStorage;
import com.yomahub.liteflow.agent.harness.storage.HarnessStorageProvider;
import com.yomahub.liteflow.agent.mysql.MysqlAgentStateStoreProvider;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import io.agentscope.extensions.mysql.store.JdbcStore;

/** Uses the configured state database and a companion table for files and archive chunks. */
public final class MysqlHarnessStorageProvider implements HarnessStorageProvider {
    @Override public AgentStateStoreType type() { return AgentStateStoreType.MYSQL; }
    @Override public HarnessStorage open(AgentStateStoreConfig config) {
        var resolved = new MysqlAgentStateStoreProvider().resolve(config);
        try {
            var state = (MysqlAgentStateStore) resolved.store();
            var source = new CatalogDataSource(state.getDataSource(), state.getDatabaseName());
            var store = JdbcStore.builder(source).tableName(state.getTableName() + "_workspace")
                    .initializeSchema(config.getMysql().isCreateIfNotExist()).build();
            return new HarnessStorage(store, resolved);
        } catch (RuntimeException | Error failure) {
            try { resolved.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}
