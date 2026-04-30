package com.yomahub.liteflow.property.agent;

/**
 * ReAct agent 日志开关配置。
 * <p>对应 {@code liteflow.agent.logging.*} 配置段。
 */
public class LoggingConfig {

    /** 是否输出 reason / act / error 内部事件日志。默认开启。 */
    private boolean reactEnabled = true;

    public boolean isReactEnabled() { return reactEnabled; }
    public void setReactEnabled(boolean reactEnabled) { this.reactEnabled = reactEnabled; }
}
