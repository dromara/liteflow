package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessAgentBuilderFilesystemBridgeTest {

    @Test
    void upstreamRejectsAnAbstractFilesystemSentinelCombinedWithAPublicSpec() {
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .abstractFilesystem(filesystem())
                .filesystem(new LocalFilesystemSpec());

        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void acceptsExactlyOneAbstractFilesystemOrPublicFilesystemSpec() {
        assertDoesNotThrow(() -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                HarnessAgent.builder().abstractFilesystem(filesystem())));
        assertDoesNotThrow(() -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                HarnessAgent.builder().filesystem(new RemoteFilesystemSpec())));
    }

    @Test
    void rejectsNoOpAndMultipleFilesystemSelectionsBeforeBuild() {
        AgentConfigException missing = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                        HarnessAgent.builder()));
        assertTrue(missing.getMessage().contains("explicit filesystem backend"));

        AgentConfigException multiple = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                        HarnessAgent.builder()
                                .abstractFilesystem(filesystem())
                                .filesystem(new RemoteFilesystemSpec())));
        assertTrue(multiple.getMessage().contains("exactly one"));
    }

    @Test
    void verifiesPinnedMetadataAndFailsClosedOnBuilderShapeDrift() {
        assertDoesNotThrow(HarnessAgentBuilderFilesystemBridge::verifyContract);

        AgentConfigException drift = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.validateShape(DriftedBuilder.class));
        assertTrue(drift.getMessage().contains("2.0.2"));
        assertNotNull(drift.getCause());
    }

    private static AbstractFilesystem filesystem() {
        return (AbstractFilesystem) Proxy.newProxyInstance(
                AbstractFilesystem.class.getClassLoader(),
                new Class<?>[] {AbstractFilesystem.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("filesystem must not be invoked");
                });
    }

    private static final class DriftedBuilder {
    }
}
