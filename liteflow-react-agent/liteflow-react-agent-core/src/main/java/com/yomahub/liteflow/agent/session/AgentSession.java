package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.skill.SkillTrackingHook;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单个 agent 在某次会话中的运行时状态。
 *
 * <p>会话标识被拆分为两个维度：
 * <ul>
 *   <li>{@code conversationId}：业务/对话维度，由调用方决定，整条 chain 内所有 agent 共享，
 *       决定 workspace 目录与对话连续性。</li>
 *   <li>{@code agentKey}：组件维度，默认是 {@code nodeId}，用于在同一段对话中区分
 *       不同 agent 的 ReActAgent 实例和持久化记忆。</li>
 * </ul>
 *
 * <p>{@code workspaceDir} 仅按 {@code conversationId} 创建，因此同一段对话中的多个 agent
 * 共享同一个工作区目录（实现 agent 之间的文件协作）。
 */
public class AgentSession {

    private final String conversationId;
    private final String agentKey;
    private final String cacheKey;
    private final Path workspaceDir;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Object agent;
    private volatile SkillTrackingHook skillTrackingHook;
    private volatile Instant lastActive = Instant.now();

    public AgentSession(String conversationId, String agentKey, String cacheKey, Path workspaceDir) {
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId");
        this.agentKey = Objects.requireNonNull(agentKey, "agentKey");
        this.cacheKey = Objects.requireNonNull(cacheKey, "cacheKey");
        this.workspaceDir = Objects.requireNonNull(workspaceDir, "workspaceDir");
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getAgentKey() {
        return agentKey;
    }

    /**
     * JVM 内的缓存 key 与持久化 key，由 {@code conversationId} 与 {@code agentKey} 组合并安全编码后得到。
     */
    public String getCacheKey() {
        return cacheKey;
    }

    public Path getWorkspaceDir() {
        return workspaceDir;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Object getAgent() {
        return agent;
    }

    public void setAgent(Object agent) {
        this.agent = agent;
    }

    public SkillTrackingHook getSkillTrackingHook() {
        return skillTrackingHook;
    }

    public void setSkillTrackingHook(SkillTrackingHook skillTrackingHook) {
        this.skillTrackingHook = skillTrackingHook;
    }

    public Instant getLastActive() {
        return lastActive;
    }

    public void touch() {
        this.lastActive = Instant.now();
    }
}
