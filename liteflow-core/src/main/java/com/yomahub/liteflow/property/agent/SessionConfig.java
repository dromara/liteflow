package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/**
 * Agent 会话生命周期配置，对应配置段 {@code liteflow.agent.session.*}。
 *
 * <p>本配置控制 JVM 内 agent 实例的缓存策略，所有字段都由
 * {@code AgentSessionManager} 在调度后台清理任务时读取使用。
 */
public class SessionConfig {

    /**
     * 会话空闲超时时间。
     *
     * <p>{@code AgentSessionManager} 在每次清理时，会把最后访问时间早于
     * {@code now - idleTimeout} 的会话从缓存中淘汰，并按工作区配置决定是否清理目录。
     */
    private Duration idleTimeout = Duration.ofMinutes(30);

    /**
     * 后台清理任务的执行周期。
     *
     * <p>{@code AgentSessionManager} 启动时按该周期调度清理线程；过短会增加 CPU 抖动，
     * 过长会让超时会话滞留更久。最终周期会与下限 20ms 取较大值。
     */
    private Duration cleanupInterval = Duration.ofMinutes(1);

    /**
     * 同时存活的 agent 会话数量上限。
     *
     * <p>{@code AgentSessionManager} 在分配新会话时若超过该值会触发 LRU 淘汰，
     * 用于在高并发场景下兜底防止内存膨胀。
     */
    private int maxSessions = 10_000;

    /**
     * 会话记忆的持久化配置。
     *
     * <p>与本类的 JVM 缓存策略正交：本类管理 agent 实例 <em>在内存里能活多久</em>，
     * 而该子配置决定对话历史 <em>持久化到哪里</em>。
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
        this.memory = memory;
    }
}
