package com.yomahub.liteflow.agent.mysql;

import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the official MySQL store's JDBC/JSON contract against H2 in MySQL mode. */
class MysqlConversationServiceTest {
    @Test
    void metadataPagingMessagesAndTombstonesRoundTripThroughSql() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:conversation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA conversations");
            statement.execute("CREATE TABLE conversations.sessions (session_id VARCHAR(255), state_key VARCHAR(255),"
                    + " item_index INT, state_data LONGTEXT, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " PRIMARY KEY(session_id,state_key,item_index))");
        }
        AgentConfig config = new AgentConfig();
        config.setApplicationName("sql-conversation-test");
        AgentConfig otherConfig = new AgentConfig();
        otherConfig.setApplicationName("other-sql-application");
        String id;
        try (var service = new AgentConversationService(config,
                new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
            id = service.create("SQL 会话").id();
            service.append(id, "user", "input", "第一条");
            service.append(id, "assistant", "result", "第二条");
            try (var other = new AgentConversationService(otherConfig,
                    new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
                other.create(id, "Different application", true);
            }
        }
        try (var reopened = new AgentConversationService(config,
                new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
            assertEquals("SQL 会话", reopened.list(0, 10).items().get(0).title());
            assertEquals(1, reopened.list(0, 10).items().size());
            assertEquals("第二条", reopened.messages(id, 1, 1).items().get(0).content());
            reopened.delete(id);
            assertTrue(reopened.list(0, 10).items().isEmpty());
            try (var other = new AgentConversationService(otherConfig,
                    new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
                assertEquals("Different application", other.get(id).orElseThrow().title());
            }
            assertThrows(IllegalStateException.class, () -> reopened.create(id, "revive", true));
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }
}
