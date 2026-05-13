package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.property.agent.ShellMode;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;

/**
 * 各测试类共享的辅助方法。
 *
 * <p>包含：
 * <ul>
 *   <li>{@link #resetAgentSessionManager()}：反射调用 ReActAgentComponent 内部单例的重置方法，
 *       让每个测试方法都从全新 SessionManager 开始；</li>
 *   <li>{@link #ensureMinimalAgentConfig(LiteflowConfig, String, ShellMode)}：补齐功能测试需要的
 *       最小 agent 配置；</li>
 *   <li>{@link #ensureCompatibleCustomCredential(LiteflowConfig, String)}：把 compatible-custom
 *       的 baseUrl/apiKey 从环境变量装到 AgentConfig，并在缺失时跳过当前测试。</li>
 * </ul>
 */
public final class LiveTestSupport {

    /** compatible-custom 配置 key，所有功能测试统一用这一个配置入口。 */
    public static final String COMPATIBLE_CONFIG_KEY = "compatible-custom";

    private LiveTestSupport() {
    }

    /**
     * 强行重置 ReActAgentComponent 内部缓存的单例 SessionManager。
     * 否则跨 test class 的 SpringBoot 上下文复用时，前一个测试遗留的 Session 会污染当前断言。
     */
    public static void resetAgentSessionManager() throws Exception {
        Class<?> holder = Class.forName(
                "com.yomahub.liteflow.agent.component.ReActAgentComponent$AgentSessionManagerHolder");
        Method reset = holder.getDeclaredMethod("resetForTesting");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    public static void ensureMinimalAgentConfig(
            LiteflowConfig liteflowConfig,
            String workspaceRoot,
            ShellMode shellMode) {
        if (liteflowConfig.getAgent() == null) {
            liteflowConfig.setAgent(new AgentConfig());
        }
        AgentConfig cfg = liteflowConfig.getAgent();
        cfg.getWorkspace().setRoot(workspaceRoot);
        cfg.getWorkspace().setAutoCreate(true);
        cfg.getShell().setMode(shellMode);
        cfg.getDefaults().setMaxIterations(6);
        cfg.getLogging().setReactEnabled(false);
        cfg.getSkills().setEnabled(false);
        cfg.getSkills().setStrict(true);
    }

    /**
     * 真实 compatible-custom apikey/baseUrl 从环境变量装到 AgentConfig。
     * 缺失任何一项即跳过当前测试。
     */
    public static String ensureCompatibleCustomCredentialOrSkip(
            LiteflowConfig liteflowConfig, String reasonPrefix) {
        String key = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_API_KEY);
        String url = LiveTestEnv.resolve(LiveTestEnv.COMPATIBLE_BASE_URL);
        Assumptions.assumeTrue(!key.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.COMPATIBLE_API_KEY + "，跳过");
        Assumptions.assumeTrue(!url.isEmpty(),
                reasonPrefix + " 未配置 " + LiveTestEnv.COMPATIBLE_BASE_URL + "，跳过");
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");

        AgentConfig cfg = liteflowConfig.getAgent();
        PlatformCredential cred = cfg.getOpenaiCompatible()
                .computeIfAbsent(COMPATIBLE_CONFIG_KEY, k -> new PlatformCredential());
        cred.setApiKey(key);
        cred.setBaseUrl(url);
        return model;
    }
}
