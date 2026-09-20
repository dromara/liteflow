package com.yomahub.liteflow.test.agent.real.basic;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * guide §2 快速开始 / §3 接入模型平台 的真实模型验证（OpenAI 兼容自定义端点）。
 */
@TestPropertySource("classpath:/real/basic/application.properties")
@SpringBootTest(classes = BasicChainLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.basic")
public class BasicChainLiveTest extends RealAgentTestBase {

    private static final String PROMPT = "请用一句话说明什么是 LiteFlow。";

    /** §2.5 最小链路：三抽象方法组件执行成功且答复写入 responseData。 */
    @Test
    public void minimalChainWritesReplyToResponseData() {
        LiteflowResponse response = flowExecutor.execute2Resp("realBasicChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + cause(response));
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply, "agent reply must be recorded into responseData");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }

    /** §3.2 通用模型参数（temperature/topP/maxTokens/附加头/附加体参数）被端点接受。 */
    @Test
    public void commonModelParametersAreAccepted() {
        LiteflowResponse response = flowExecutor.execute2Resp("realParamsChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed with model params: " + cause(response));
        Assertions.assertFalse(response.getSlot().getResponseData().toString().isBlank());
    }

    /** §3.1 代码显式 apiKey/baseUrl 优先于配置文件（配置被改成无效值仍应成功）。 */
    @Test
    public void explicitCredentialOverridesConfig() {
        PlatformCredential broken = new PlatformCredential();
        broken.setApiKey("sk-invalid-litteflow-test");
        broken.setBaseUrl("https://invalid.invalid.invalid/v1");
        PlatformCredential original = liteflowConfig.getAgent().getOpenaiCompatible()
                .put(LiveTestSupport.COMPATIBLE_CONFIG_KEY, broken);
        try {
            LiteflowResponse response = flowExecutor.execute2Resp("realExplicitChain", PROMPT);
            Assertions.assertTrue(response.isSuccess(),
                    "explicit apiKey/baseUrl must win over broken config: " + cause(response));
        } finally {
            liteflowConfig.getAgent().getOpenaiCompatible()
                    .put(LiveTestSupport.COMPATIBLE_CONFIG_KEY, original);
        }
    }

    /** §3.4 buildModel 逃生舱装配真实模型同样可用。 */
    @Test
    public void buildModelEscapeHatchUsesRealModel() {
        LiteflowResponse response = flowExecutor.execute2Resp("realBuildModelChain", PROMPT);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + cause(response));
        Assertions.assertFalse(response.getSlot().getResponseData().toString().isBlank());
    }

    /** §2.2 / §18：缺失 liteflow.agent.application-name 时首次执行 fail-fast。 */
    @Test
    public void missingNamespaceFailsFastWithAgentConfigException() {
        String original = liteflowConfig.getAgent().getApplicationName();
        liteflowConfig.getAgent().setApplicationName(" ");
        try {
            LiteflowResponse response = flowExecutor.execute2Resp("realBasicChain", PROMPT);
            Assertions.assertFalse(response.isSuccess(), "blank namespace must fail the chain");
            Assertions.assertNotNull(response.getCause());
            Assertions.assertTrue(cause(response).contains(
                            "liteflow.agent.application-name is required before execution"),
                    "unexpected cause: " + cause(response));
        } finally {
            liteflowConfig.getAgent().setApplicationName(original);
        }
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
