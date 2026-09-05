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
        config.getRuntime().setNamespace("sql-conversation-test");
        String id;
        try (var service = new AgentConversationService(config,
                new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
            id = service.create("alice", "SQL 会话").id();
            service.append("alice", id, "user", "input", "第一条");
            service.append("alice", id, "assistant", "result", "第二条");
            service.create("Alice", id, "Different user", true);
        }
        try (var reopened = new AgentConversationService(config,
                new MysqlAgentStateStore(dataSource, "conversations", "sessions", false))) {
            assertEquals("SQL 会话", reopened.list("alice", 0, 10).items().get(0).title());
            assertEquals(1, reopened.list("alice", 0, 10).items().size());
            assertEquals("第二条", reopened.messages("alice", id, 1, 1).items().get(0).content());
            assertTrue(reopened.list("bob", 0, 10).items().isEmpty());
            reopened.delete("alice", id);
            assertTrue(reopened.list("alice", 0, 10).items().isEmpty());
            assertEquals("Different user", reopened.list("Alice", 0, 10).items().get(0).title());
            assertThrows(IllegalStateException.class, () -> reopened.create("alice", id, "revive", true));
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }
}
