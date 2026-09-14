package com.yomahub.liteflow.agent.mysql.guard;

import com.yomahub.liteflow.agent.guard.*;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.mysql.MysqlAgentStateStoreProvider;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MysqlInvocationGuardProvider implements AgentInvocationGuardProvider {
    @Override public AgentStateStoreType type() { return AgentStateStoreType.MYSQL; }
    @Override public AgentInvocationGuard resolve(AgentConfig config) {
        var resolved = new MysqlAgentStateStoreProvider().resolve(config.getStateStore());
        var state = (MysqlAgentStateStore) resolved.store();
        var source = state.getDataSource();
        String storageIdentity = state.getDatabaseName() + ":" + state.getTableName();
        resolved.close();
        return (key, timeout) -> {
            Connection connection = null;
            try {
                String material = storageIdentity + ":" + key;
                String name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(material.getBytes(StandardCharsets.UTF_8)));
                connection = source.getConnection();
                try (var query = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                    query.setString(1, name);
                    query.setLong(2, timeout.isZero() ? 0 : Math.max(1, timeout.toSeconds()));
                    try (var result = query.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 1) throw new AgentInvocationException(
                                AgentInvocationErrorType.TIMEOUT, "Timed out acquiring MySQL invocation lease");
                    }
                }
                Connection owned = connection;
                return new AgentInvocationLease() {
                    final AtomicBoolean closed = new AtomicBoolean();
                    public AgentInvocationKey key() { return key; }
                    public void close() {
                        if (!closed.compareAndSet(false,true)) return;
                        try (owned; var query = owned.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                            query.setString(1,name); query.execute();
                        } catch (Exception failure) { throw new AgentInvocationException("Cannot release MySQL lease", failure); }
                    }
                };
            } catch (Exception failure) {
                if (connection != null) try { connection.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
                if (failure instanceof AgentInvocationException typed) throw typed;
                throw new AgentInvocationException("Cannot acquire MySQL invocation lease", failure);
            }
        };
    }
}
