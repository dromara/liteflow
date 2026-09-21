package com.yomahub.liteflow.agent.redis;

import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import io.agentscope.core.state.AgentState;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RedissonStateCompatibilityTest {
    @Test void stateVersionWritesAndKeyDiscoveryUseTheSupportedRedissonApiWithoutClosingTheBorrowedClient() {
        AtomicInteger scripts = new AtomicInteger();
        RScript script = proxy(RScript.class, (method, args) -> {
            assertEquals("eval", method);
            assertEquals(RScript.ReturnType.INTEGER, args[2]);
            assertEquals(3, ((List<?>) args[3]).size());
            scripts.incrementAndGet();
            return 1L;
        });
        RKeys keys = proxy(RKeys.class, (method, args) -> {
            assertEquals("getKeysByPattern", method);
            assertEquals("guide:*", args[0]);
            return List.of("guide:one", "guide:two");
        });
        RedissonClient client = proxy(RedissonClient.class, (method, args) -> switch (method) {
            case "getScript" -> script;
            case "getKeys" -> keys;
            default -> throw new AssertionError("Unexpected client operation: " + method);
        });
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getRedis().setClientBeanName("redisson");
        try (var resolved = new RedisAgentStateStoreProvider(name -> client).resolve(config)) {
            assertFalse(resolved.owned());
            assertEquals(1L, resolved.store().saveIfVersion(null, "chat", "agent_state", AgentState.builder().build(), 0));
            assertEquals(1, scripts.get());
        }
        assertEquals(java.util.Set.of("guide:one", "guide:two"),
                new RedissonStateClientAdapter(client).findKeysByPattern("guide:*"));
    }

    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args)));
    }
}
