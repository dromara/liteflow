package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.session.Session;

/**
 * 返回 {@code null} Session，用于通知 AgentSessionManager 跳过所有加载和保存操作。
 * 供 {@link MemoryStorageMode#NONE} 使用。
 */
public class NoneAgentSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.NONE;
    }

    @Override
    public Session create(AgentConfig agentConfig) {
        return null;
    }
}
