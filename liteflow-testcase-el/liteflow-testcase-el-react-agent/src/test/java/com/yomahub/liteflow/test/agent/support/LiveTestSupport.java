package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;

/**
 * 整个模块唯一共享的「凭据/skip/重置」插管。
 *
 * <p>按用户约定：不同 package 之间只共享这一层（凭据解析、无 key 即 skip、SessionManager 重置）；
 * 其余 agent 组件、辅助节点、探针、flow xml、application.properties 一律每个 package 各自冗余。
 *
 * <p>提供：
 * <ul>
 *   <li>{@link #resetAgentSessionManager()}：反射重置 ReActAgentComponent 内部单例 SessionManager，
 *       避免跨测试类的全局静态状态互相污染；</li>
 *   <li>{@link #compatibleCustomModel()}：功能测试统一用的 OpenAI 兼容自定义模型描述符；</li>
 *   <li>各平台的「装凭据或 skip」方法：把真实 apikey/baseUrl 从环境变量装入 AgentConfig，
 *       缺失即 {@code Assumptions.assumeTrue} 跳过当前测试。</li>
 * </ul>
 */
public final class LiveTestSupport {

    /** 功能测试统一使用的 OpenAI 兼容配置 key（自定义 baseUrl + apiKey）。 */
    public static final String COMPATIBLE_CONFIG_KEY = "compatible-custom";

    /** Anthropic 兼容网关固定 configKey。 */
    public static final String ANTHROPIC_GATEWAY_CONFIG_KEY = "gateway";

    private LiveTestSupport() {
    }

    /**
     * 强行重置 ReActAgentComponent 内部缓存的单例 SessionManager。
     * 否则跨 test class 的 JVM 静态状态复用时，前一个测试遗留的 Session 会污染当前断言。
     */
    public static void resetAgentSessionManager() throws Exception {
        Class<?> holder = Class.forName(
                "com.yomahub.liteflow.agent.component.ReActAgentComponent$AgentSessionManagerHolder");
        Method reset = holder.getDeclaredMethod("resetForTesting");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    /**
     * 功能测试统一使用的 OpenAI 兼容自定义模型描述符。
     * 真实 model 名由 {@link LiveTestEnv#COMPATIBLE_MODEL} 提供，缺省 gpt-4o-mini。
     * 凭据（apiKey/baseUrl）由测试 @BeforeEach 调用 {@link #applyCompatibleCustomOrSkip} 装入。
     */
    public static ModelSpec<?> compatibleCustomModel() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(128);
    }

    /**
     * 与 {@link #compatibleCustomModel()} 相同，但额外开启底层模型 stream 模式，
     * 供流式事件场景使用。
     */
    public static ModelSpec<?> compatibleCustomStreamingModel() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(128)
                .stream(true);
    }

    private static AgentConfig agent(LiteflowConfig cfg) {
        if (cfg.getAgent() == null) {
            cfg.setAgent(new AgentConfig());
        }
        return cfg.getAgent();
    }

    /* =================== 功能测试主入口：OpenAI 兼容自定义 =================== */

    /**
     * 把功能测试用的 compatible-custom apiKey/baseUrl 从环境变量装入 AgentConfig；
     * 缺失任一项即跳过当前测试。
     */
    public static void applyCompatibleCustomOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_API_KEY);
        String url = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_BASE_URL);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.COMPATIBLE_API_KEY + "，跳过");
        Assumptions.assumeTrue(!url.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.COMPATIBLE_BASE_URL + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        cred.setBaseUrl(url);
        agent(cfg).getOpenaiCompatible().put(COMPATIBLE_CONFIG_KEY, cred);
    }

    /* =================== 头等平台 =================== */

    public static void applyOpenAIOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.OPENAI_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.OPENAI_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        String baseUrl = LiveTestEnv.resolve(LiveTestEnv.OPENAI_BASE_URL);
        if (!baseUrl.isEmpty()) {
            cred.setBaseUrl(baseUrl);
        }
        agent(cfg).setOpenai(cred);
    }

    public static void applyAnthropicOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.ANTHROPIC_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        agent(cfg).setAnthropic(cred);
    }

    public static void applyGeminiOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.GEMINI_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.GEMINI_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        agent(cfg).setGemini(cred);
    }

    public static void applyDashScopeOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.DASHSCOPE_API_KEY);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.DASHSCOPE_API_KEY + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        agent(cfg).setDashscope(cred);
    }

    /* =================== OpenAI 兼容族预设（内置 baseUrl） =================== */

    /**
     * DeepSeek / Kimi / GLM / Minimax 这类内置默认 baseUrl 的兼容预设：只需 apiKey。
     * configKey 必须与入口类内部约定一致（deepseek/kimi/glm/minimax）。
     */
    public static void applyCompatiblePresetOrSkip(
            LiteflowConfig cfg, String configKey, String apiKeyEnv, String reasonPrefix) {
        String key = LiveTestEnv.resolve(apiKeyEnv);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + apiKeyEnv + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        agent(cfg).getOpenaiCompatible().put(configKey, cred);
    }

    /* =================== Anthropic 兼容网关 =================== */

    public static void applyAnthropicGatewayOrSkip(LiteflowConfig cfg, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_GATEWAY_API_KEY);
        String url = LiveTestEnv.resolve(LiveTestEnv.ANTHROPIC_GATEWAY_BASE_URL);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.ANTHROPIC_GATEWAY_API_KEY + "，跳过");
        Assumptions.assumeTrue(!url.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.ANTHROPIC_GATEWAY_BASE_URL + "，跳过");
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey(key);
        cred.setBaseUrl(url);
        agent(cfg).getAnthropicCompatible().put(ANTHROPIC_GATEWAY_CONFIG_KEY, cred);
    }
}
