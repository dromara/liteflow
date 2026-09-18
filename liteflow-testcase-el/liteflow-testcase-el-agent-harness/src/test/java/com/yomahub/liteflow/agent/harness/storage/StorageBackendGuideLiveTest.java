package com.yomahub.liteflow.agent.harness.storage;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.mysql.MysqlAgentStateStoreProvider;
import com.yomahub.liteflow.agent.redis.RedisAgentStateStoreProvider;
import com.yomahub.liteflow.property.agent.*;
import io.agentscope.extensions.mysql.state.MysqlAgentStateStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import redis.clients.jedis.JedisPooled;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Guide 2.4/2.5: real service contracts; the runner supplies disposable test services. */
@EnabledIfEnvironmentVariable(named = "LITEFLOW_TEST_SHARED_STORAGE", matches = "true")
class StorageBackendGuideLiveTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = AgentSessionStoreType.class, names = {"REDIS", "MYSQL"})
    void changingBackendDoesNotMigrateOrOverwriteTheOriginalJsonConversation(AgentSessionStoreType backend) throws Exception {
        AgentConfig config = new AgentConfig();
        config.setApplicationName("switch-" + UUID.randomUUID());
        config.getSessionStore().setJsonRoot(root.resolve("state").toString());
        try (var json = AgentConversationService.open(config)) {
            json.create("chat", "original-json", true);
            json.append("chat", "user", "input", "json-only");
        }
        config.getSessionStore().setType(backend);
        config.getSessionStore().getRedis().setUri(required("LITEFLOW_TEST_REDIS_URI"));
        var mysql = config.getSessionStore().getMysql();
        mysql.setJdbcUrl(required("LITEFLOW_TEST_MYSQL_URL"));
        mysql.setUsername("root");
        mysql.setPassword("");
        mysql.setDatabaseName("liteflow_storage_test");
        mysql.setTableName("switch_sessions");
        mysql.setCreateIfNotExist(true);
        try (var remote = AgentConversationService.open(config)) {
            assertTrue(remote.get("chat").isEmpty());
            remote.create("chat", "new-backend", true);
            remote.append("chat", "user", "input", "remote-only");
            assertEquals("remote-only", remote.messages("chat", 0, 10).items().get(0).content());
        }
        config.getSessionStore().setType(AgentSessionStoreType.JSON);
        try (var json = AgentConversationService.open(config)) {
            assertEquals("original-json", json.get("chat").orElseThrow().title());
            assertEquals(List.of("json-only"), json.messages("chat", 0, 10).items().stream()
                    .map(com.yomahub.liteflow.agent.conversation.AgentConversationMessage::content).toList());
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"jedis", "lettuce", "redisson"})
    void eachDocumentedRedisClientBeanPersistsHistoryWithoutTtlAndRemainsApplicationOwned(String type) throws Exception {
        String uri = required("LITEFLOW_TEST_REDIS_URI");
        Object client = switch (type) {
            case "jedis" -> new JedisPooled(URI.create(uri));
            case "lettuce" -> RedisClient.create(uri);
            default -> {
                var options = new org.redisson.config.Config().setThreads(2).setNettyThreads(2);
                options.useSingleServer().setAddress(uri);
                yield Redisson.create(options);
            }
        };
        try (var inspection = new JedisPooled(URI.create(uri))) {
            verifyRedisBean(client, inspection);
            // Resolution and service close must never close the borrowed application client.
            if (client instanceof JedisPooled jedis) assertEquals("PONG", jedis.ping());
            else if (client instanceof RedisClient lettuce) assertEquals("PONG", ping(lettuce));
            else if (client instanceof RedissonClient redisson) {
                assertFalse(redisson.isShutdown());
                assertDoesNotThrow(() -> redisson.getBucket("guide-probe").isExists());
            }
        } finally {
            if (client instanceof RedisClient lettuce) lettuce.shutdown();
            else if (client instanceof RedissonClient redisson) redisson.shutdown();
            else ((JedisPooled) client).close();
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LITEFLOW_TEST_REDIS_CLUSTER_URI", matches = ".+")
    void lettuceClusterUsesTheDocumentedSharedHashTagForAtomicUpdates() throws Exception {
        String uri = required("LITEFLOW_TEST_REDIS_CLUSTER_URI");
        RedisClusterClient client = RedisClusterClient.create(uri);
        try (var inspection = new JedisPooled(URI.create(uri))) {
            verifyRedisBean(client, inspection);
            try (var connection = client.connect()) { assertEquals("PONG", connection.sync().ping()); }
        } finally { client.shutdown(); }
    }

    private void verifyRedisBean(Object client, JedisPooled inspection) throws Exception {
        String suffix = UUID.randomUUID().toString();
        AgentConfig config = new AgentConfig();
        config.setApplicationName("redis-guide-" + suffix);
        config.getInvocationGuard().setMode(AgentInvocationGuardMode.LOCAL);
        config.getSessionStore().setType(AgentSessionStoreType.REDIS);
        config.getSessionStore().getRedis().setClientBeanName("businessRedis");
        String prefix = "agent:{guide-" + suffix + "}:";
        config.getSessionStore().getRedis().setKeyPrefix(prefix);
        var provider = new RedisAgentStateStoreProvider(name -> {
            assertEquals("businessRedis", name);
            return client;
        });
        try (var resolved = provider.resolve(config.getSessionStore())) {
            assertFalse(resolved.owned());
            try (var history = new AgentConversationService(config, resolved.store())) {
                history.create("chat", "Redis order", true);
                history.append("chat", "user", "input", "order-123");
                history.append("chat", "assistant", "result", "paid");
            }
            try (var reopened = new AgentConversationService(config, resolved.store())) {
                assertEquals("paid", reopened.messages("chat", 1, 1).items().get(0).content());
                var keys = inspection.keys(prefix + "*");
                assertFalse(keys.isEmpty());
                assertEquals(1, keys.stream().map(redis.clients.jedis.util.JedisClusterCRC16::getSlot).distinct().count());
                for (String key : keys) assertEquals(-1L, inspection.ttl(key), key);
                reopened.delete("chat");
                assertTrue(reopened.get("chat").isEmpty());
            }
        } finally {
            var keys = inspection.keys(prefix + "*");
            if (!keys.isEmpty()) inspection.del(keys.toArray(String[]::new));
        }
    }

    private static String ping(RedisClient client) {
        try (var connection = client.connect()) { return connection.sync().ping(); }
    }

    @Test
    void guideDdlSupportsManualProvisioningWorkspaceAndLegacyVersionUpgrade() throws Exception {
        String database = "guide_" + UUID.randomUUID().toString().replace("-", "");
        String jdbc = required("LITEFLOW_TEST_MYSQL_URL");
        try (var connection = DriverManager.getConnection(jdbc, "root", ""); var sql = connection.createStatement()) {
            try {
                String ddl;
                try (var stream = getClass().getResourceAsStream("/guide/mysql-schema.sql")) {
                    assertNotNull(stream);
                    ddl = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("liteflow_agent", database);
                }
                for (String statement : ddl.split(";")) if (!statement.isBlank()) sql.execute(statement);
                AgentConfig config = mysqlConfig(jdbc, database);
                config.getSessionStore().getMysql().setCreateIfNotExist(false);
                try (var history = AgentConversationService.open(config)) {
                    history.create("manual", "手工建表", true);
                    history.append("manual", "user", "input", "persisted");
                }
                try (var history = AgentConversationService.open(config)) {
                    assertEquals("persisted", history.messages("manual", 0, 10).items().get(0).content());
                }
                try (var storage = HarnessStorage.open(config.getSessionStore())) {
                    assertTrue(storage.store().putIfVersion(List.of("guide", "workspace"), "file",
                            Map.of("content", "业务文件"), 0));
                    assertEquals("业务文件", storage.store().get(List.of("guide", "workspace"), "file").value().get("content"));
                }
                sql.execute("ALTER TABLE " + database + ".agent_sessions DROP COLUMN version");
                try (var history = AgentConversationService.open(config)) {
                    assertEquals("手工建表", history.get("manual").orElseThrow().title());
                    history.append("manual", "assistant", "result", "after-upgrade");
                    assertEquals(2, history.messages("manual", 0, 10).items().size());
                }
                try (var rows = sql.executeQuery("SELECT version FROM " + database + ".agent_sessions")) {
                    assertTrue(rows.next());
                }
            } finally { sql.execute("DROP DATABASE IF EXISTS " + database); }
        }
    }

    @Test
    void mysqlDataSourceBeanAndJdbcUrlUseExplicitDatabaseInsteadOfTheUrlCatalog() throws Exception {
        String database = "guide_" + UUID.randomUUID().toString().replace("-", "");
        String jdbc = required("LITEFLOW_TEST_MYSQL_URL");
        AgentConfig config = mysqlConfig(jdbc, database);
        try (var connection = DriverManager.getConnection(jdbc, "root", ""); var sql = connection.createStatement()) {
            try {
                try (var resolved = new MysqlAgentStateStoreProvider().resolve(config.getSessionStore())) {
                    assertTrue(resolved.owned());
                    assertEquals(database, ((MysqlAgentStateStore) resolved.store()).getDatabaseName());
                }
                MysqlDataSource source = new MysqlDataSource();
                source.setURL(jdbc);
                source.setUser("root");
                source.setPassword("");
                config.getSessionStore().getMysql().setJdbcUrl(null);
                config.getSessionStore().getMysql().setDataSourceBeanName("pooledDataSource");
                config.getSessionStore().getMysql().setCreateIfNotExist(false);
                try (var resolved = new MysqlAgentStateStoreProvider(name -> source).resolve(config.getSessionStore());
                     var history = new AgentConversationService(config, resolved.store())) {
                    assertFalse(resolved.owned());
                    history.create("bean", "pooled", true);
                    assertEquals("pooled", history.get("bean").orElseThrow().title());
                }
                try (var borrowed = source.getConnection()) { assertFalse(borrowed.isClosed()); }
            } finally { sql.execute("DROP DATABASE IF EXISTS " + database); }
        }
    }

    private static AgentConfig mysqlConfig(String jdbc, String database) {
        AgentConfig config = new AgentConfig();
        config.setApplicationName(database);
        config.getInvocationGuard().setMode(AgentInvocationGuardMode.LOCAL);
        config.getSessionStore().setType(AgentSessionStoreType.MYSQL);
        var mysql = config.getSessionStore().getMysql();
        mysql.setJdbcUrl(jdbc);
        mysql.setUsername("root");
        mysql.setPassword("");
        mysql.setDatabaseName(database);
        mysql.setTableName("agent_sessions");
        mysql.setCreateIfNotExist(true);
        return config;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        assertNotNull(value, name + " must point to a dedicated test service");
        return value;
    }
}
