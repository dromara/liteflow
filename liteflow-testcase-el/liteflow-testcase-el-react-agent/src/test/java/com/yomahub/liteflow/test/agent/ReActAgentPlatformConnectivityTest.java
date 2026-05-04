package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 测试各模型平台 provider 的真实连通性。
 *
 * <p>这些用例会真实访问外部模型服务，因此全部通过环境变量或配置中的 API Key
 * 控制是否执行。缺少对应平台 Key 时，用例会被跳过，不影响本地和 CI 的基础测试。
 *
 * <p>每个平台仍然通过 LiteFlow EL 三节点链路执行，例如
 * {@code THEN(prepare, geminiAgent, recordReply)}，从而同时验证 provider
 * 与 LiteFlow 节点编排的集成。
 */
public class ReActAgentPlatformConnectivityTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证 Gemini provider 在真实 API Key 存在时可以完成一次模型调用。
     */
    @Test
    public void testGeminiConnectivity() {
        String key = resolveApiKey("GEMINI_API_KEY", liteflowConfig.getAgent().getGemini());
        Assumptions.assumeTrue(!key.isBlank(), "gemini api-key 未配置，跳过");
        liteflowConfig.getAgent().getGemini().setApiKey(key);

        assertPlatformChainSuccess("geminiChain", "请用一句话回答：LiteFlow 是什么？");
    }

    /**
     * 验证 OpenAI provider 在真实 API Key 存在时可以完成一次模型调用。
     */
    @Test
    public void testOpenAIConnectivity() {
        String key = resolveApiKey("OPENAI_API_KEY", liteflowConfig.getAgent().getOpenai());
        Assumptions.assumeTrue(!key.isBlank(), "openai api-key 未配置，跳过");
        liteflowConfig.getAgent().getOpenai().setApiKey(key);

        assertPlatformChainSuccess("openAIChain", "请用一句话回答：LiteFlow 是什么？");
    }

    /**
     * 验证 Anthropic provider 在真实 API Key 存在时可以完成一次模型调用。
     */
    @Test
    public void testAnthropicConnectivity() {
        String key = resolveApiKey("ANTHROPIC_API_KEY", liteflowConfig.getAgent().getAnthropic());
        Assumptions.assumeTrue(!key.isBlank(), "anthropic api-key 未配置，跳过");
        liteflowConfig.getAgent().getAnthropic().setApiKey(key);

        assertPlatformChainSuccess("anthropicChain", "请用一句话回答：LiteFlow 是什么？");
    }

    /**
     * 验证 DashScope provider 在真实 API Key 存在时可以完成一次模型调用。
     */
    @Test
    public void testDashScopeConnectivity() {
        String key = resolveApiKey("DASHSCOPE_API_KEY", liteflowConfig.getAgent().getDashscope());
        Assumptions.assumeTrue(!key.isBlank(), "dashscope api-key 未配置，跳过");
        liteflowConfig.getAgent().getDashscope().setApiKey(key);

        assertPlatformChainSuccess("dashScopeChain", "请用一句话回答：LiteFlow 是什么？");
    }

    /**
     * 先读环境变量，再回退到 application.properties 中的平台配置。
     */
    private String resolveApiKey(String envName, PlatformCredential credential) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String configured = credential.getApiKey();
        return configured == null ? "" : configured.trim();
    }

    /**
     * 平台连通性测试的公共断言：链路成功，并且 recordReply 能拿到非空模型回复。
     */
    private void assertPlatformChainSuccess(String chainId, String question) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, question);
        if (!response.isSuccess() && response.getCause() != null) {
            response.getCause().printStackTrace();
        }
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply, "agent reply must be recorded");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");
    }
}
