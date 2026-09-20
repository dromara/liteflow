package com.yomahub.liteflow.agent.harness;

import com.yomahub.liteflow.agent.component.AbstractAgentComponent;
import com.yomahub.liteflow.agent.conversation.AgentStateAddressProvider;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.state.HarnessAgentStateAddressProvider;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessDependencyBoundaryTest {

    @Test
    void coreDependencyExposesTheRequiredAgentScopeTypes() {
        assertNotNull(HarnessAgent.class);
        assertNotNull(DockerFilesystemSpec.class);
        assertNotNull(DockerSandboxClientOptions.class);
        assertNotNull(SubagentDeclaration.class);
    }

    @Test
    void coreContainsHarnessAndRegistersItsStateAddressProvider() {
        assertEquals(
                AbstractAgentComponent.class.getProtectionDomain().getCodeSource().getLocation(),
                HarnessAgentComponent.class.getProtectionDomain().getCodeSource().getLocation());
        assertTrue(ServiceLoader.load(AgentStateAddressProvider.class).stream()
                .anyMatch(provider -> provider.type() == HarnessAgentStateAddressProvider.class));
    }
}
