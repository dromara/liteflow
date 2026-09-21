package com.yomahub.liteflow.agent.harness.config;

import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessConfigTest {

    @Test
    void guardedLocalDefaultsAreValidWithoutAnAdditionalTrustFlag() {
        AgentConfig config = new AgentConfig();
        assertEquals(HarnessFilesystemBackend.GUARDED_LOCAL, config.getHarness().getFilesystemBackend());
        assertDoesNotThrow(config.getHarness()::validate);
    }

    @Test
    void missingLocalConfigurationIsRejected() {
        AgentConfig config = new AgentConfig();
        config.getHarness().setLocal(null);
        assertThrows(IllegalStateException.class, config.getHarness()::validate);
    }

    @Test
    void dockerSelectionUsesSafeDefaultsWithoutTrustingTheHost() {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);

        assertDoesNotThrow(agentConfig.getHarness()::validate);
    }

    @Test
    void filesystemBackendContractContainsOnlyTheSupportedPolicies() {
        assertArrayEquals(
                new HarnessFilesystemBackend[] {
                    HarnessFilesystemBackend.GUARDED_LOCAL,
                    HarnessFilesystemBackend.DOCKER,
                    HarnessFilesystemBackend.CUSTOM
                },
                HarnessFilesystemBackend.values());
    }

    @Test
    void filesystemExtensionContextCarriesLocationAndCommandTimeout() {
        AgentConfig agentConfig = new AgentConfig();
        Path workspaceRoot = Path.of("build", "harness-workspace");
        Duration commandTimeout = Duration.ofSeconds(37);
        HarnessFilesystemContext context = new HarnessFilesystemContext(
                workspaceRoot, commandTimeout, agentConfig);
        AtomicReference<HarnessAgent.Builder> configuredBuilder = new AtomicReference<>();
        AtomicReference<HarnessFilesystemContext> configuredContext = new AtomicReference<>();
        HarnessFilesystemConfigurer configurer = (builder, suppliedContext) -> {
            configuredBuilder.set(builder);
            configuredContext.set(suppliedContext);
        };
        HarnessAgent.Builder builder = HarnessAgent.builder();

        configurer.configure(builder, context);

        assertSame(builder, configuredBuilder.get());
        assertSame(context, configuredContext.get());
        assertEquals(workspaceRoot, context.workspaceRoot());
        assertEquals(commandTimeout, context.commandTimeout());
        assertSame(agentConfig, context.agentConfig());
    }
}
