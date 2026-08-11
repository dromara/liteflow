package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/**
 * AgentScope 1 会话配置兼容对象。
 *
 * <p>AgentScope 2 不使用静态会话缓存或后台清理线程；本类仅保留旧属性的绑定边界，
 * 其中显式配置的 {@code session.memory.*} 会由 {@link AgentConfig} 给出迁移诊断。
 */
public class SessionConfig {

    /**
     * 1.x 会话空闲超时时间，仅为源代码兼容保留。
     */
    private Duration idleTimeout = Duration.ofMinutes(30);

    /**
     * 1.x 后台清理周期，仅为源代码兼容保留。
     */
    private Duration cleanupInterval = Duration.ofMinutes(1);

    /**
     * 1.x 会话数量上限，仅为源代码兼容保留。
     */
    private int maxSessions = 10_000;

    /**
     * 1.x 记忆持久化配置；显式使用时要求迁移到 {@code state-store.*}。
     */
    private MemoryStorageConfig memory = new MemoryStorageConfig();

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration v) {
        this.idleTimeout = v;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration v) {
        this.cleanupInterval = v;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(int v) {
        this.maxSessions = v;
    }

    public MemoryStorageConfig getMemory() {
        return memory;
    }

	public void setMemory(MemoryStorageConfig memory) {
		if (memory != null) {
			memory.markExplicitlyConfigured();
		}
		this.memory = memory;
	}
}
