package com.yomahub.liteflow.agent.harness;

import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HarnessDependencyBoundaryTest {

    @Test
    void harnessDependencyExposesTheRequiredAgentScopeTypes() {
        assertNotNull(HarnessAgent.class);
        assertNotNull(DockerFilesystemSpec.class);
        assertNotNull(DockerSandboxClientOptions.class);
        assertNotNull(SubagentDeclaration.class);
    }
}
