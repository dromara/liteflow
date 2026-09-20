package com.yomahub.liteflow.property.agent;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigV2Test {

	@Test
	void appliesAgentScope2RuntimeDefaults() {
		AgentConfig config = new AgentConfig();

		assertTrue(config.isConversationHistoryEnabled());
		assertNull(config.getApplicationName());
		assertEquals(Duration.ofMinutes(10), config.getExecutionTimeout());
		assertEquals(AgentSessionStoreType.JSON, config.getSessionStore().getType());
		assertEquals("./data/agent-state", config.getSessionStore().getJsonRoot());
		assertEquals(AgentSessionStoreFailurePolicy.FAIL_FAST, config.getSessionStore().getFailurePolicy());
		assertNull(config.getSessionStore().getRedis().getUri());
		assertNull(config.getSessionStore().getMysql().getJdbcUrl());
		assertFalse(config.getToolkit().isParallel());
		assertEquals(AgentListenerFailureMode.FAIL_FAST, config.getEvent().getListenerFailureMode());
		assertEquals(AgentInvocationGuardMode.AUTO, config.getInvocationGuard().getMode());
		assertEquals(Duration.ofMinutes(2), config.getInvocationGuard().getAcquireTimeout());
		assertEquals(HarnessFilesystemBackend.GUARDED_LOCAL, config.getHarness().getFilesystemBackend());
		assertEquals(Duration.ofMinutes(2), config.getHitl().getConfirmationTimeout());
		assertFalse(config.getHitl().isFailOnDeniedTool());
		assertEquals(ShellMode.WHITELIST, config.getHarness().getShell().getMode());
		assertTrue(config.getHarness().getShell().getWhitelist().containsAll(java.util.List.of("sh", "bash", "python3", "node", "git", "npm", "mvn")));
	}

    @Test
    void aCustomCommandListReplacesDefaultsWithoutChangingOtherConfigurations() {
        ShellConfig first = new ShellConfig();
        ShellConfig second = new ShellConfig();
        first.setWhitelist(java.util.List.of("printf"));
        assertEquals(java.util.List.of("printf"), first.getWhitelist());
        assertTrue(second.getWhitelist().contains("python3"));
        assertTrue(second.getWhitelist().contains("node"));
    }

	@Test
	void rejectsMissingApplicationNameBeforeExecution() {
		AgentConfig config = new AgentConfig();

		IllegalStateException error = assertThrows(IllegalStateException.class, config::validateForExecution);

		assertTrue(error.getMessage().contains("application-name"));
	}

	@Test
	void acceptsConfiguredApplicationNameBeforeExecution() {
		AgentConfig config = new AgentConfig();
		config.setApplicationName("orders");

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(config::validateForExecution);
	}
}
