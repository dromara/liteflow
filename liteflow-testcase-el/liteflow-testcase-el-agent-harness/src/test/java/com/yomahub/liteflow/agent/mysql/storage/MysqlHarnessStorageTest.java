package com.yomahub.liteflow.agent.mysql.storage;

import com.yomahub.liteflow.agent.harness.storage.HarnessStorage;
import com.yomahub.liteflow.agent.testsupport.MockBeanContext;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import io.agentscope.extensions.mysql.store.JdbcStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MysqlHarnessStorageTest {
    @Test
    void providerPersistsWorkspaceCasAndNamespacesAcrossReopeningWithoutClosingTheBorrowedPool() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:workspace_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
        try (var connection = source.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE SCHEMA agentscope");
            sql.execute("CREATE TABLE agentscope.sessions (session_id VARCHAR(255), state_key VARCHAR(255),"
                    + "item_index INT, state_data LONGTEXT, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY(session_id,state_key,item_index))");
            source.setURL(source.getURL() + ";SCHEMA=agentscope");
            JdbcStore.builder(source).tableName("sessions_workspace").initializeSchema(true).build();
            AgentSessionStoreConfig config = new AgentSessionStoreConfig();
            config.setType(AgentSessionStoreType.MYSQL);
            config.getMysql().setDataSourceBeanName("mockSql");
            config.getMysql().setDatabaseName("agentscope");
            config.getMysql().setTableName("sessions");
            List<String> namespace = List.of("app", "chat");
            try (var context = new MockBeanContext(Map.of("mockSql", source))) {
                try (var storage = HarnessStorage.open(config)) {
                    assertTrue(storage.store().putIfVersion(namespace, "MEMORY.md", Map.of("empty", List.of()), 0));
                    assertFalse(storage.store().putIfVersion(namespace, "MEMORY.md", Map.of("lost", true), 0));
                    assertTrue(storage.store().putIfVersion(namespace, "MEMORY.md", Map.of("text", "remembered"), 1));
                    storage.store().put(List.of("other-app", "chat"), "MEMORY.md", Map.of("text", "isolated"));
                }
                try (var reopened = HarnessStorage.open(config)) {
                    assertEquals("remembered", reopened.store().get(namespace, "MEMORY.md").value().get("text"));
                    assertEquals(2, reopened.store().get(namespace, "MEMORY.md").version());
                    assertEquals(1, reopened.store().search(namespace, 10, 0).size());
                    reopened.store().delete(namespace, "MEMORY.md");
                    assertNull(reopened.store().get(namespace, "MEMORY.md"));
                    assertEquals("isolated", reopened.store().get(List.of("other-app", "chat"), "MEMORY.md").value().get("text"));
                }
                try (var stillUsable = source.getConnection()) { assertFalse(stillUsable.isClosed()); }
            } finally { sql.execute("SHUTDOWN"); }
        }
    }
}
