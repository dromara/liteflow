package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.session.InMemorySession;
import io.agentscope.core.session.Session;

/**
 * 使用 AgentScope 的内存存储支持 {@link MemoryStorageMode#JVM} 模式。
 *
 * <p>注意：状态仍会在同一个 JVM 内跨调用保留（适合希望在单进程内保留多轮记忆的场景），
 * 但进程退出后会丢失。如果需要跨重启持久化，请选择
 * {@code WORKSPACE_FILE}、{@code REDIS} 或 {@code MYSQL}。
 */
public class InMemoryAgentSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.JVM;
    }

    @Override
    public Session create(AgentConfig agentConfig) {
        return new InMemorySession();
    }
}
