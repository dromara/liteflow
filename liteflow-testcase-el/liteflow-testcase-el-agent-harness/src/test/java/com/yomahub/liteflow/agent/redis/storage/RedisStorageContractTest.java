package com.yomahub.liteflow.agent.redis.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.testsupport.MockBeanContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisStorageContractTest {
    private final RedissonClient client = mock(RedissonClient.class);
    private final RScript script = mock(RScript.class);
    private final AgentConfig config = new AgentConfig();
    private final Map<String, String> owners = new ConcurrentHashMap<>();
    private final CountDownLatch renewed = new CountDownLatch(2);
    private final AtomicReference<RuntimeException> renewalFailure = new AtomicReference<>();
    private MockBeanContext context;

    @BeforeEach
    void configure() throws Exception {
        config.getSessionStore().getRedis().setClientBeanName("mockRedis");
        config.getSessionStore().getRedis().setKeyPrefix("fixture:");
        config.getInvocationGuard().setLeaseDuration(Duration.ofMillis(300));
        context = new MockBeanContext(Map.of("mockRedis", client));
        when(client.getScript(StringCodec.INSTANCE)).thenReturn(script);
    }

    @AfterEach
    void restore() throws Exception {
        Thread.interrupted();
        try { verify(client, never()).shutdown(); }
        finally { context.close(); }
    }

    @Test
    void workspaceReadWriteCasPaginationAndDeleteUseOneNamespacedKey() throws Exception {
        RKeys keys = mock(RKeys.class);
        when(client.getKeys()).thenReturn(keys);
        RBucket<String> bucket = mock(RBucket.class);
        when(client.<String>getBucket(anyString(), eq(StringCodec.INSTANCE))).thenReturn(bucket);
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenAnswer(call -> {
                    List<?> addresses = call.getArgument(3);
                    assertEquals(1, addresses.size(), "workspace CAS must stay in one Redis cluster slot");
                    assertTrue(addresses.get(0).toString().startsWith("fixture:"));
                    assertEquals("0", call.getArgument(4));
                    String json = call.getArgument(5);
                    assertEquals(new ObjectMapper().createArrayNode(),
                            new ObjectMapper().readTree(json).path("value").path("empty"));
                    assertEquals("a", new ObjectMapper().readTree(json).path("key").asText());
                    return 1L;
                });
        try (var connection = RedisStorageConnection.open(config.getSessionStore().getRedis())) {
            var store = new RedisWorkspaceStore(connection, "fixture:");
            assertNull(store.get(List.of("app", "chat"), "missing"));
            assertTrue(store.putIfVersion(List.of("app", "chat"), "a", Map.of("empty", List.of()), 0));
            doReturn(0L).when(script).eval(eq(RScript.Mode.READ_WRITE), anyString(),
                    eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class));
            assertFalse(store.putIfVersion(List.of("app", "chat"), "a", Map.of("lost", true), 0));
            store.put(List.of("app", "chat"), "a", Map.of("unconditional", true));
            verify(script).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                    anyList(), eq("-1"), contains("unconditional"));
            when(bucket.get()).thenReturn("2\n{\"key\":\"a\",\"value\":{\"empty\":[]}}");
            var item = store.get(List.of("app", "chat"), "a");
            assertEquals(2, item.version());
            assertEquals(List.of(), item.value().get("empty"));
            when(keys.getKeysByPattern(anyString())).thenReturn(List.of("fixture:b", "fixture:a"));
            assertEquals(1, store.search(List.of("app", "chat"), 1, 1).size());
            assertTrue(store.search(List.of("app", "chat"), 0, 0).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> store.search(List.of(), -1, 0));
            assertThrows(IllegalArgumentException.class, () -> store.search(List.of(), 1, -1));
            assertThrows(IllegalArgumentException.class, () -> store.putIfVersion(List.of(), "a", Map.of(), -1));
            when(bucket.get()).thenReturn("corrupt-record");
            assertThrows(IllegalStateException.class, () -> store.get(List.of(), "broken"));
            store.delete(List.of("app", "chat"), "a");
            verify(bucket).delete();
        }
    }

    @Test
    void leasesRenewContendReleaseAndRejectUseAfterClose() throws Exception {
        mockLeases();
        var key = AgentInvocationKey.workspace("app", "chat");
        try (var first = new RedisInvocationGuardProvider().resolve(config);
             var second = new RedisInvocationGuardProvider().resolve(config)) {
            try (var held = first.acquire(key, Duration.ZERO)) {
                assertEquals(key, held.key());
                assertTrue(renewed.await(2, TimeUnit.SECONDS), "background renewal must reach the mock Redis client");
                AgentInvocationException failure = assertThrows(AgentInvocationException.class,
                        () -> second.acquire(key, Duration.ofMillis(30)));
                assertEquals(AgentInvocationErrorType.TIMEOUT, failure.getErrorType());
                try (var independent = second.acquire(AgentInvocationKey.workspace("app", "other"), Duration.ZERO)) {
                    assertNotEquals(key, independent.key());
                }
                held.close();
                held.close();
            }
            try (var acquired = second.acquire(key, Duration.ZERO)) { assertEquals(key, acquired.key()); }
            assertTrue(owners.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> first.acquire(key, null));
            assertThrows(IllegalArgumentException.class, () -> first.acquire(key, Duration.ofMillis(-1)));
            first.close();
            first.close();
            assertThrows(IllegalStateException.class, () -> first.acquire(key, Duration.ZERO));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lostLeaseInterruptsOwnerAndNeverReleasesItsSuccessor(boolean transportFailure) throws Exception {
        mockLeases();
        var key = AgentInvocationKey.workspace("app", "lost");
        try (var guard = new RedisInvocationGuardProvider().resolve(config)) {
            var held = guard.acquire(key, Duration.ZERO);
            owners.replaceAll((address, owner) -> "successor-owner");
            if (transportFailure) renewalFailure.set(new IllegalStateException("mock Redis unavailable"));
            try {
                assertThrows(InterruptedException.class, () -> new CountDownLatch(1).await(2, TimeUnit.SECONDS));
                assertThrows(AgentInvocationException.class, held::close);
                assertEquals(List.of("successor-owner"), new ArrayList<>(owners.values()));
                held.close();
            } finally {
                Thread.interrupted();
                held.close();
            }
        }
    }

    @Test
    void interruptedAcquisitionPreservesInterruptAndDoesNotContactRedis() {
        mockLeases();
        try (var guard = new RedisInvocationGuardProvider().resolve(config)) {
            Thread.currentThread().interrupt();
            var failure = assertThrows(AgentInvocationException.class,
                    () -> guard.acquire(AgentInvocationKey.workspace("app", "chat"), Duration.ZERO));
            assertEquals(AgentInvocationErrorType.INTERRUPTED, failure.getErrorType());
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(script);
        } finally { Thread.interrupted(); }
    }

    private void mockLeases() {
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenAnswer(call -> {
                    String lua = call.getArgument(1);
                    List<?> keys = call.getArgument(3);
                    assertEquals(1, keys.size());
                    String key = keys.get(0).toString();
                    assertTrue(key.startsWith("fixture:lock:"));
                    String owner = call.getArgument(4);
                    if (lua.contains("'NX'")) {
                        assertEquals("300", call.getArgument(5));
                        return owners.putIfAbsent(key, owner) == null ? 1L : 0L;
                    }
                    if (lua.contains("'pexpire'")) {
                        if (renewalFailure.get() != null) throw renewalFailure.get();
                        renewed.countDown();
                        return owner.equals(owners.get(key)) ? 1L : 0L;
                    }
                    assertTrue(lua.contains("'del'"));
                    return owners.remove(key, owner) ? 1L : 0L;
                });
    }
}
