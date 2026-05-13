package com.yomahub.liteflow.test.agent.features.support;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.property.agent.ShellMode;

/**
 * 新增 ReAct Agent 功能测试的共享辅助方法。
 *
 * <p>这些测试每个功能包都使用独立的 Spring 配置文件，但有两件事需要保持一致：
 * 1）补齐 SpringBoot 属性绑定在单模块测试中偶发缺失的 agent 段；
 * 2）每个测试方法开始前清掉 ReActAgentComponent 内部缓存的 SessionManager。
 */
public final class ReActAgentFeatureTestSupport {

    public static final String COMPATIBLE_CONFIG_KEY = "compatible-custom";
    public static final String DEFAULT_COMPATIBLE_API_KEY = "test-compatible-key";
    public static final String DEFAULT_COMPATIBLE_BASE_URL = "http://127.0.0.1:65535/v1";

    private ReActAgentFeatureTestSupport() {
    }

    public static void ensureAgentConfig(
            LiteflowConfig liteflowConfig,
            String workspaceRoot,
            boolean skillsEnabled,
            String skillsPath,
            ShellMode shellMode) {
        if (liteflowConfig.getAgent() == null) {
            liteflowConfig.setAgent(new AgentConfig());
        }
        AgentConfig agentConfig = liteflowConfig.getAgent();
        agentConfig.getWorkspace().setRoot(workspaceRoot);
        agentConfig.getShell().setMode(shellMode);
        agentConfig.getDefaults().setMaxIterations(6);
        agentConfig.getLogging().setReactEnabled(false);
        agentConfig.getSkills().setEnabled(skillsEnabled);
        if (skillsPath != null) {
            agentConfig.getSkills().setPath(skillsPath);
        }
        agentConfig.getSkills().setStrict(true);

        PlatformCredential credential = agentConfig.getOpenaiCompatible()
                .computeIfAbsent(COMPATIBLE_CONFIG_KEY, key -> new PlatformCredential());
        if (credential.getApiKey() == null || credential.getApiKey().isBlank()) {
            credential.setApiKey(DEFAULT_COMPATIBLE_API_KEY);
        }
        if (credential.getBaseUrl() == null || credential.getBaseUrl().isBlank()) {
            credential.setBaseUrl(DEFAULT_COMPATIBLE_BASE_URL);
        }
    }

    public static void resetAgentSessionManager() throws Exception {
        Class<?> holder = Class.forName(
                "com.yomahub.liteflow.agent.component.ReActAgentComponent$AgentSessionManagerHolder");
        var reset = holder.getDeclaredMethod("resetForTesting");
        reset.setAccessible(true);
        reset.invoke(null);
    }
}
