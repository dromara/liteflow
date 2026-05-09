package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.slot.Slot;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 在 {@link ReActAgentComponent} 的钩子中暴露给子类的运行时上下文。
 *
 * <p>三个标识层：
 * <ul>
 *   <li>{@link #getConversationId()}：业务/对话维度，整条 chain 内一致。</li>
 *   <li>{@link #getAgentKey()}：组件维度，默认为 {@code nodeId}。</li>
 *   <li>{@link #getWorkspaceDir()}：按 conversationId 创建，同一段对话中的多个 agent 共享。</li>
 * </ul>
 *
 * <p><b>勿在跨 invocation 缓存的对象中持有 {@code ReActAgentContext} 引用</b>
 * （例如自定义工具实例、Hook、Model 实现）。这些对象会被缓存的 ReActAgent 跨次复用，
 * 而 ctx 是 per-invocation 的——捕获后下一次 process() 时通过该 ctx 访问的 slot
 * 已被 {@code DataBus.releaseSlot} 回收并复用，是悬挂引用。
 *
 * <p>正确做法：在工具/Model 类中持有组件实例引用，运行时通过
 * {@code component.ctx()} 动态获取当次 ctx。
 */
public class ReActAgentContext {
    private final Slot slot;
    private final String conversationId;
    private final String agentKey;
    private final Path workspaceDir;

    public ReActAgentContext(Slot slot, String conversationId, String agentKey, Path workspaceDir) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId");
        this.agentKey = Objects.requireNonNull(agentKey, "agentKey");
        this.workspaceDir = Objects.requireNonNull(workspaceDir, "workspaceDir");
    }

    public Slot getSlot() { return slot; }

    public String getConversationId() { return conversationId; }

    public String getAgentKey() { return agentKey; }

    public Path getWorkspaceDir() { return workspaceDir; }
}
