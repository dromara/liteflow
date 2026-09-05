package com.yomahub.liteflow.test.agent.feature.sandbox;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/sandbox/application.properties")
@SpringBootTest(classes = GuardedLocalSandboxTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.sandbox")
class GuardedLocalSandboxTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    void reset() {
        GuardedLocalSandboxAgentCmp.reset();
    }

    @Test
    void guardedLocalWritesInsideWorkspaceAndRejectsTraversalWithoutDocker() throws Exception {
        Path workspace = Path.of(liteflowConfig.getAgent().getWorkspace().getRoot())
                .toAbsolutePath()
                .normalize();
        Path escaped = workspace.resolve("../guarded-local-escape-probe.txt").normalize();
        assertFalse(Files.exists(escaped), "test precondition: escape probe must not exist");

        LiteflowResponse response = flowExecutor.execute2Resp(
                "guardedLocalChain", "offline",
                ExecuteOption.of().conversationId("guarded-local-conversation"));

        assertTrue(response.isSuccess());
        assertEquals(HarnessFilesystemBackend.GUARDED_LOCAL,
                liteflowConfig.getAgent().getHarness().getFilesystemBackend());
        Path written;
        try (var paths = Files.walk(workspace)) {
            written = paths.filter(path -> path.endsWith("notes/result.txt"))
                    .findFirst()
                    .orElseThrow();
        }
        assertEquals("inside-workspace", Files.readString(written));
        assertFalse(Files.exists(escaped));
        assertEquals(0, GuardedLocalSandboxAgentCmp.dockerClientCalls());
    }
}
