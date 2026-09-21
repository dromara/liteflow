package com.yomahub.liteflow.agent.mysql;

import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** JDBC schema and migration contract checks use an isolated H2 database in MySQL mode. */
class MysqlBeanAndSchemaTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void borrowedDataSourceUsesConfiguredDatabaseAndMigratesLegacyVersionColumn(boolean explicitDatabase) throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:guide_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
        try (var connection = source.getConnection(); var sql = connection.createStatement()) {
            for (String database : new String[]{"agentscope", "explicit_database", "url_database"}) {
                sql.execute("CREATE SCHEMA " + database);
                sql.execute("CREATE TABLE " + database + ".agentscope_sessions (session_id VARCHAR(255),"
                        + "state_key VARCHAR(255),item_index INT,state_data LONGTEXT,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY(session_id,state_key,item_index))");
            }
            source.setURL(source.getURL() + ";SCHEMA=url_database");
            AgentSessionStoreConfig config = new AgentSessionStoreConfig();
            config.getMysql().setDataSourceBeanName("businessDataSource");
            if (explicitDatabase) config.getMysql().setDatabaseName("explicit_database");
            String expected = explicitDatabase ? "explicit_database" : "agentscope";
            var provider = new MysqlAgentStateStoreProvider(name -> {
                assertEquals("businessDataSource", name);
                return source;
            });
            try (var resolved = provider.resolve(config)) {
                assertFalse(resolved.owned());
                assertEquals(expected, ((MysqlAgentStateStore) resolved.store()).getDatabaseName());
                assertEquals("agentscope_sessions", ((MysqlAgentStateStore) resolved.store()).getTableName());
                AgentConfig agent = new AgentConfig();
                agent.setApplicationName("mysql-bean-guide");
                try (var history = new AgentConversationService(agent, resolved.store())) {
                    history.create("chat", "订单", true);
                    history.append("chat", "user", "input", "订单 123");
                    assertEquals("订单 123", history.messages("chat", 0, 10).items().get(0).content());
                }
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + expected + ".agentscope_sessions")) {
                    assertTrue(rows.next());
                    assertTrue(rows.getInt(1) > 0);
                }
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM url_database.agentscope_sessions")) {
                    assertTrue(rows.next());
                    assertEquals(0, rows.getInt(1));
                }
                try (var rows = sql.executeQuery("SELECT version FROM " + expected + ".agentscope_sessions")) {
                    assertTrue(rows.next(), "legacy table must gain the optimistic-lock version column");
                }
            }
            // Closing a resolved borrowed store must leave the application's DataSource usable.
            try (var stillOpen = source.getConnection()) { assertFalse(stillOpen.isClosed()); }
            sql.execute("SHUTDOWN");
        }
    }
}
