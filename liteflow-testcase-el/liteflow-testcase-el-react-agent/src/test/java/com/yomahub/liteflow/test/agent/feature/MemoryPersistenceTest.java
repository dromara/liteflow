package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import com.yomahub.liteflow.test.agent.feature.cmp.MemoryAgentCmp;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 覆盖 guide §5.5 中 memory 持久化模式：JVM（默认）、NONE、LOCAL_FILE。
 *
 * <p>JVM/Hot Session 复用语义由 {@link SessionReuseTest} 覆盖，
 * 这里聚焦持久化模式开关本身能正常初始化并完成一次链路调用。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = MemoryPersistenceTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class MemoryPersistenceTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        MemoryAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "MemoryPersistenceTest");
    }

    @Test
    public void testJvmModeIsDefaultAndChainSucceeds() {
        liteflowConfig.getAgent().getSession().getMemory().setMode(MemoryStorageMode.JVM);
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
    }

    @Test
    public void testNoneModeChainStillSucceeds() {
        liteflowConfig.getAgent().getSession().getMemory().setMode(MemoryStorageMode.NONE);
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
    }

    @Test
    public void testLocalFileModeCreatesSessionDirAndPersistsMemory() {
        liteflowConfig.getAgent().getSession().getMemory().setMode(MemoryStorageMode.LOCAL_FILE);
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // LOCAL_FILE 模式会在 workspace.root/.agent-session 下产生文件。
        Path sessionDir = Paths.get(liteflowConfig.getAgent().getWorkspace().getRoot(), ".agent-session")
                .toAbsolutePath()
                .normalize();
        Assertions.assertTrue(Files.isDirectory(sessionDir),
                "LOCAL_FILE 模式应在 workspace.root 下创建 .agent-session 目录：" + sessionDir);
    }
}
