package com.yomahub.liteflow.property.agent;

/**
 * Agent 日志开关配置，对应配置段 {@code liteflow.agent.logging.*}。
 */
public class LoggingConfig {

    /**
     * 是否输出 Agent 内部事件日志（reason / act / error 等）。
     *
     * <p>开启后，{@code AgentComponent} 会在每一轮推理-行动循环中
     * 输出对应的内部事件，便于排查 agent 调用流程；默认开启。
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
