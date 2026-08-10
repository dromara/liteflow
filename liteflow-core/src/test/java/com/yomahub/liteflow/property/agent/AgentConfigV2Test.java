package com.yomahub.liteflow.property.agent;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigV2Test {

	@Test
	void appliesAgentScope2RuntimeDefaults() {
		AgentConfig config = new AgentConfig();

		assertNull(config.getRuntime().getNamespace());
		assertEquals("anonymous", config.getRuntime().getDefaultUserId());
		assertEquals(Duration.ofMinutes(2), config.getRuntime().getTimeout());
		assertEquals(AgentStateStoreType.MEMORY, config.getStateStore().getType());
		assertEquals("./data/agent-state", config.getStateStore().getJsonRoot());
		assertEquals(AgentStateStoreFailurePolicy.FAIL_FAST, config.getStateStore().getFailurePolicy());
		assertFalse(config.getToolkit().isParallel());
		assertEquals(AgentListenerFailureMode.FAIL_FAST, config.getEvent().getListenerFailureMode());
		assertEquals(AgentInvocationGuardMode.LOCAL, config.getInvocationGuard().getMode());
		assertEquals(Duration.ofMinutes(2), config.getInvocationGuard().getAcquireTimeout());
		assertEquals(DistributedCoordinationMode.NONE, config.getInvocationGuard().getCoordinationMode());
		assertTrue(config.getInvocationGuard().isStrictDistributed());
		assertEquals(WorkspaceBackend.GUARDED_LOCAL, config.getWorkspace().getBackend());
		assertFalse(config.getWorkspace().isTrustedLocal());
		assertEquals(Duration.ofMinutes(2), config.getHitl().getConfirmationTimeout());
		assertFalse(config.getHitl().isFailOnDeniedTool());
		assertEquals(ShellMode.DISABLED, config.getShell().getMode());
	}

	@Test
	void rejectsBlankRuntimeNamespaceBeforeExecution() {
		AgentConfig config = new AgentConfig();

		IllegalStateException error = assertThrows(IllegalStateException.class, config::validateForExecution);

		assertTrue(error.getMessage().contains("runtime.namespace"));
	}

	@Test
	void acceptsUntouchedLegacyMemoryDefaultsAfterNamespaceIsConfigured() {
		AgentConfig config = configuredConfig();

		assertDoesNotThrow(config::validateForExecution);
	}

	@Test
	void rejectsExplicitLegacyMemoryConfigurationWithMigrationHint() {
		AgentConfig config = configuredConfig();
		config.getSession().getMemory().setMode(MemoryStorageMode.NONE);

		IllegalStateException error = assertThrows(IllegalStateException.class, config::validateForExecution);

		assertTrue(error.getMessage().contains("session.memory -> state-store"));
	}

	@Test
	void rejectsExplicitLegacyNestedMemoryConfigurationWithMigrationHint() {
		AgentConfig config = configuredConfig();
		config.getSession().getMemory().getRedis().setBeanName("legacyRedis");

		IllegalStateException error = assertThrows(IllegalStateException.class, config::validateForExecution);

		assertTrue(error.getMessage().contains("session.memory -> state-store"));
	}

	private AgentConfig configuredConfig() {
		AgentConfig config = new AgentConfig();
		config.getRuntime().setNamespace("orders");
		return config;
	}
}
