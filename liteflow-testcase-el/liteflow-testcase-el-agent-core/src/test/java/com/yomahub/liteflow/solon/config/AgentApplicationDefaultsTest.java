package com.yomahub.liteflow.solon.config;

import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentApplicationDefaultsTest {

    @Test
    void usesSolonApplicationNameWithoutAgentConfiguration() {
        LiteflowAutoConfiguration configuration = new LiteflowAutoConfiguration();
        configuration.applicationName = "orders-module";

        AgentConfig agent = configuration.liteflowConfig(
                properties(), new LiteflowMonitorProperty()).getAgent();

        assertEquals("orders-module", agent.getApplicationName());
        assertEquals(Duration.ofMinutes(10), agent.getExecutionTimeout());
        assertEquals(100, agent.getMaxIterations());
        assertEquals(Duration.ofMinutes(1), agent.getHarness().getShell().getTimeout());
        assertTrue(agent.isExecutionLogEnabled());
    }

    @Test
    void preservesExplicitAgentApplicationNameAndExecutionTimeout() {
        LiteflowAutoConfiguration configuration = new LiteflowAutoConfiguration();
        configuration.applicationName = "orders-module";
        LiteflowProperty properties = properties();
        AgentConfig agent = new AgentConfig();
        agent.setApplicationName("shared-orders");
        agent.setExecutionTimeout(Duration.ofSeconds(45));
        agent.setMaxIterations(27);
        agent.getHarness().getShell().setTimeout(Duration.ofSeconds(31));
        agent.setExecutionLogEnabled(false);
        properties.setAgent(agent);

        AgentConfig resolved = configuration.liteflowConfig(
                properties, new LiteflowMonitorProperty()).getAgent();

        assertEquals("shared-orders", resolved.getApplicationName());
        assertEquals(Duration.ofSeconds(45), resolved.getExecutionTimeout());
        assertEquals(27, resolved.getMaxIterations());
        assertEquals(Duration.ofSeconds(31), resolved.getHarness().getShell().getTimeout());
        assertFalse(resolved.isExecutionLogEnabled());
    }

    private static LiteflowProperty properties() {
        LiteflowProperty properties = new LiteflowProperty();
        LiteflowProperty.ChainCacheProperty cache = new LiteflowProperty.ChainCacheProperty();
        cache.setEnabled(false);
        cache.setCapacity(10000);
        properties.setChainCache(cache);
        return properties;
    }
}
