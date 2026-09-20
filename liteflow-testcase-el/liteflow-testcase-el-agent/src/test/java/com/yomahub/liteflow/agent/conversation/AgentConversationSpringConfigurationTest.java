package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Guide 2.6: import, inject, persist and close the public conversation service. */
class AgentConversationSpringConfigurationTest {
    @TempDir Path temp;

    @Test void namedInvocationGuardIsBorrowedFromSpringAndRejectsMissingOrWrongBeanConfiguration() throws Exception {
        var previous = com.yomahub.liteflow.spi.spring.SpringAware.getApplicationContext();
        var bridge = new com.yomahub.liteflow.spi.spring.SpringAware();
        var closes = new java.util.concurrent.atomic.AtomicInteger();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        com.yomahub.liteflow.agent.guard.AgentInvocationGuard guard = new com.yomahub.liteflow.agent.guard.AgentInvocationGuard() {
            @Override public com.yomahub.liteflow.agent.guard.AgentInvocationLease acquire(
                    com.yomahub.liteflow.agent.guard.AgentInvocationKey key, java.time.Duration timeout) {
                calls.incrementAndGet();
                assertEquals(java.time.Duration.ofSeconds(3), timeout);
                return new com.yomahub.liteflow.agent.guard.AgentInvocationLease() {
                    @Override public com.yomahub.liteflow.agent.guard.AgentInvocationKey key() { return key; }
                    @Override public void close() { }
                };
            }
            @Override public void close() { closes.incrementAndGet(); }
        };
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("businessGuard", com.yomahub.liteflow.agent.guard.AgentInvocationGuard.class, () -> guard);
            context.registerBean("badGuard", String.class, () -> "wrong type");
            context.refresh();
            bridge.setApplicationContext(context);
            var config = new AgentConfig();
            config.getInvocationGuard().setMode(com.yomahub.liteflow.property.agent.AgentInvocationGuardMode.BEAN);
            var resolver = new com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver();
            assertThrows(com.yomahub.liteflow.agent.exception.AgentConfigException.class, () -> resolver.resolve(config));
            config.getInvocationGuard().setBeanName("badGuard");
            assertThrows(com.yomahub.liteflow.agent.exception.AgentConfigException.class, () -> resolver.resolve(config));
            config.getInvocationGuard().setBeanName("businessGuard");
            var key = com.yomahub.liteflow.agent.guard.AgentInvocationKey.workspace("guide", "chat");
            try (var borrowed = resolver.resolve(config); var lease = borrowed.acquire(key, java.time.Duration.ofSeconds(3))) {
                assertEquals(key, lease.key());
            }
            assertEquals(1, calls.get());
            assertEquals(0, closes.get(), "runtime must leave the application's guard open");
        } finally { bridge.setApplicationContext(previous); }
        assertEquals(1, closes.get(), "Spring owns the guard lifecycle");
    }

    @Test void importedServiceUsesConfiguredStorageAndClosesWithTheSpringContext() {
        AgentConfig agent = new AgentConfig();
        agent.setApplicationName("spring-history");
        agent.getSessionStore().setJsonRoot(temp.toString());
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        AgentConversationService service;
        String id;
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(LiteflowConfig.class, () -> config);
            context.register(HistoryApplication.class);
            context.refresh();
            service = context.getBean(AgentConversationService.class);
            assertSame(service, context.getBean("agentConversationService"));
            id = service.create("订单对话").id();
            service.append(id, "user", "input", "订单 123");
        }
        assertThrows(IllegalStateException.class, () -> service.get(id));
        try (var reopened = AgentConversationService.open(agent)) {
            assertEquals("订单对话", reopened.get(id).orElseThrow().title());
            assertEquals("订单 123", reopened.messages(id, 0, 10).items().get(0).content());
            reopened.delete(id);
            assertTrue(reopened.get(id).isEmpty());
        }
    }

    @Configuration
    @Import(AgentConversationConfiguration.class)
    static class HistoryApplication { }
}
