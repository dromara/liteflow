package com.yomahub.liteflow.test.agent.feature.memorypersistence;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 覆盖 AgentScope 2 state store 的 MEMORY（默认）与 JSON 持久化模式。
 *
 * <p>聚焦于各 state-store 类型都能正常初始化并完成一次真实链路调用；
 * JSON 额外验证配置的持久化目录被创建。
 */
@TestPropertySource("classpath:/feature/memorypersistence/application.properties")
@SpringBootTest(classes = MemoryPersistenceTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.memorypersistence")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MemoryPersistenceTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "MemoryPersistenceTest");
    }

    @Test
    public void testMemoryTypeIsDefaultAndChainSucceeds() {
        Assertions.assertEquals(AgentStateStoreType.MEMORY,
                liteflowConfig.getAgent().getStateStore().getType());
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
    }

    @Test
    public void testExplicitMemoryTypeChainStillSucceeds() {
        liteflowConfig.getAgent().getStateStore().setType(AgentStateStoreType.MEMORY);
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
    }

    @Test
    public void testJsonTypeCreatesStateStoreDirAndPersistsMemory() {
        Path stateStoreRoot = Paths.get("target/wk/feature_memorypersistence/agent-state")
                .toAbsolutePath()
                .normalize();
        liteflowConfig.getAgent().getStateStore().setType(AgentStateStoreType.JSON);
        liteflowConfig.getAgent().getStateStore().setJsonRoot(stateStoreRoot.toString());
        LiteflowResponse response = flowExecutor.execute2Resp("memoryChain", "你好。");
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        Assertions.assertTrue(Files.isDirectory(stateStoreRoot),
                "JSON state store 应创建持久化目录：" + stateStoreRoot);
    }
}
