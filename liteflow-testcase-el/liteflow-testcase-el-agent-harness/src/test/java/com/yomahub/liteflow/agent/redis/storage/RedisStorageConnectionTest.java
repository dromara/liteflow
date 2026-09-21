package com.yomahub.liteflow.agent.redis.storage;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.testsupport.MockBeanContext;
import com.yomahub.liteflow.property.agent.AgentSessionStoreRedisConfig;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisStorageConnectionTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lettuceStandaloneAndClusterScanEveryPageAndCloseOnlyTheBorrowedClientConnection(boolean cluster) throws Exception {
        RedisClient client = mock(RedisClient.class);
        RedisClusterClient clusterClient = mock(RedisClusterClient.class);
        StatefulRedisConnection<String, String> socket = mock(StatefulRedisConnection.class);
        StatefulRedisClusterConnection<String, String> clusterSocket = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> commands = mock(RedisAdvancedClusterCommands.class,
                withSettings().extraInterfaces(RedisCommands.class));
        RedisCommands<String, String> standalone = (RedisCommands<String, String>) commands;
        when(client.connect()).thenReturn(socket);
        when(socket.sync()).thenReturn(standalone);
        when(clusterClient.connect()).thenReturn(clusterSocket);
        when(clusterSocket.sync()).thenReturn(commands);
        KeyScanCursor<String> first = new KeyScanCursor<>();
        first.setCursor("1"); first.setFinished(false); first.getKeys().add("fixture:a");
        KeyScanCursor<String> last = new KeyScanCursor<>();
        last.setCursor("0"); last.setFinished(true); last.getKeys().add("fixture:b");
        when(commands.scan(any(ScanCursor.class), any(ScanArgs.class))).thenReturn(first, last);
        when(commands.get("fixture:a")).thenReturn("value");
        when(commands.eval(eq("mock-script"), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
                .thenReturn(1L);
        try (var context = new MockBeanContext(Map.of("mock", cluster ? clusterClient : client));
             var connection = RedisStorageConnection.open(config())) {
            assertEquals(Set.of("fixture:a", "fixture:b"), connection.keys.apply("fixture:*"));
            verify(commands).scan(eq(ScanCursor.INITIAL), any(ScanArgs.class));
            verify(commands).scan(eq(first), any(ScanArgs.class));
            assertEquals("value", connection.get.apply("fixture:a"));
            assertEquals(1, connection.eval.run("mock-script", List.of("fixture:a"), List.of("value")));
            connection.delete.accept("fixture:a");
            verify(commands).del("fixture:a");
            connection.close();
            connection.close();
        }
        if (cluster) verify(clusterSocket).close(); else verify(socket).close();
        verify(client, never()).shutdown();
        verify(clusterClient, never()).shutdown();
    }

    @Test
    void jedisScansEveryPageForwardsLuaAndNeverClosesTheBorrowedClient() throws Exception {
        UnifiedJedis client = mock(UnifiedJedis.class);
        when(client.scan(eq("0"), any(ScanParams.class))).thenReturn(new ScanResult<>("1", List.of("fixture:a")));
        when(client.scan(eq("1"), any(ScanParams.class))).thenReturn(new ScanResult<>("0", List.of("fixture:b")));
        when(client.get("fixture:a")).thenReturn("value");
        when(client.eval("mock-script", List.of("fixture:a"), List.of("value"))).thenReturn(1L);
        try (var context = new MockBeanContext(Map.of("mock", client));
             var connection = RedisStorageConnection.open(config())) {
            assertEquals(Set.of("fixture:a", "fixture:b"), connection.keys.apply("fixture:*"));
            assertEquals("value", connection.get.apply("fixture:a"));
            assertEquals(1L, connection.eval.run("mock-script", List.of("fixture:a"), List.of("value")));
            connection.delete.accept("fixture:a");
            verify(client).del("fixture:a");
        }
        verify(client, never()).close();
    }

    @Test
    void invalidSourcesFailBeforeConnectingAndBlankPrefixesRetainTheDocumentedDefault() throws Exception {
        AgentSessionStoreRedisConfig config = new AgentSessionStoreRedisConfig();
        assertThrows(AgentConfigException.class, () -> RedisStorageConnection.open(config));
        config.setClientBeanName("mock");
        config.setUri("redis://unused.invalid:6379");
        assertThrows(AgentConfigException.class, () -> RedisStorageConnection.open(config));
        config.setUri(null);
        try (var context = new MockBeanContext(Map.of("mock", new Object()))) {
            assertThrows(AgentConfigException.class, () -> RedisStorageConnection.open(config));
        }
        config.setKeyPrefix(null);
        assertEquals("agentscope:session:", RedisStorageConnection.prefix(config));
        config.setKeyPrefix(" ");
        assertEquals("agentscope:session:", RedisStorageConnection.prefix(config));
        config.setKeyPrefix("fixture:");
        assertEquals("fixture:", RedisStorageConnection.prefix(config));
    }

    private static AgentSessionStoreRedisConfig config() {
        AgentSessionStoreRedisConfig config = new AgentSessionStoreRedisConfig();
        config.setClientBeanName("mock");
        return config;
    }
}
