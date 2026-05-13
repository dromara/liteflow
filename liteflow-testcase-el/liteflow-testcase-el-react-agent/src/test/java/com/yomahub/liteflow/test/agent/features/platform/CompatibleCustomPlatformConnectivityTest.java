package com.yomahub.liteflow.test.agent.features.platform;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * 覆盖 guide 中“测试者提供 baseUrl 和 key 后访问 OpenAI 兼容平台”的真实冒烟场景。
 *
 * <p>该测试默认跳过。运行者需要提供：
 * {@code TEST_LITEFLOW_COMPATIBLE_CUSTOM_API_KEY}、
 * {@code TEST_LITEFLOW_COMPATIBLE_CUSTOM_BASE_URL}，以及可选的
 * {@code TEST_LITEFLOW_COMPATIBLE_CUSTOM_MODEL}。
 */
@TestPropertySource(value = "classpath:/agent/features/platform/application.properties")
@SpringBootTest(classes = CompatibleCustomPlatformConnectivityTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.platform.cmp" })
public class CompatibleCustomPlatformConnectivityTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ensureMinimalAgentConfigWithoutFakeCredential();
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
    }

    @Test
    public void testCompatibleCustomPlatformConnectivityWhenCredentialProvided() {
        PlatformCredential credential = liteflowConfig.getAgent().getOpenaiCompatible()
                .get(ReActAgentFeatureTestSupport.COMPATIBLE_CONFIG_KEY);
        Assumptions.assumeTrue(credential != null
                        && credential.getApiKey() != null
                        && !credential.getApiKey().isBlank()
                        && credential.getBaseUrl() != null
                        && !credential.getBaseUrl().isBlank(),
                "compatible-custom api-key/base-url 未配置，跳过真实平台冒烟测试");

        LiteflowResponse response = flowExecutor.execute2Resp(
                "compatibleCustomPlatformChain",
                "用一句中文短句回复：LiteFlow ReAct Agent 连通性正常。");

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Object reply = response.getSlot().getOutput("platformRecord");
        Assertions.assertNotNull(reply, "真实平台应返回非空回复");
        Assertions.assertFalse(reply.toString().isBlank(), "真实平台回复不应为空白");
    }

    private void ensureMinimalAgentConfigWithoutFakeCredential() {
        if (liteflowConfig.getAgent() == null) {
            liteflowConfig.setAgent(new AgentConfig());
        }
        AgentConfig agentConfig = liteflowConfig.getAgent();
        agentConfig.getWorkspace().setRoot("target/wk_react_agent_platform");
        agentConfig.getShell().setMode(ShellMode.DISABLED);
        agentConfig.getDefaults().setMaxIterations(3);
        agentConfig.getLogging().setReactEnabled(false);
    }
}
