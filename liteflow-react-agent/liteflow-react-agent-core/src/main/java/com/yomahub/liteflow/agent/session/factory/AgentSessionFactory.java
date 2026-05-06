package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.session.Session;

/**
 * 用于为 {@link io.agentscope.core.session.Session} 接入额外持久化后端的 SPI。
 *
 * <p>框架内置模式（{@code JVM}、{@code LOCAL_FILE}、{@code REDIS}、
 * {@code MYSQL}、{@code NONE}）。需要其他后端（例如 PostgreSQL、OSS、
 * 加密 JSON）的用户，可以在 {@code META-INF/services/}{@link AgentSessionFactory}
 * 下注册自定义工厂。
 */
public interface AgentSessionFactory {

    /**
     * 当前工厂处理的模式。所有已注册工厂之间必须唯一。
     */
    MemoryStorageMode mode();

    /**
     * 根据 agent 配置构建底层 {@link Session}。该方法会在首次
     * {@code process()} 时懒调用，而不是在框架启动时调用。
     *
     * @return 非 null 的 Session；如果需要跳过持久化则返回 {@code null}
     *         （{@link MemoryStorageMode#NONE} 对应的工厂会返回 {@code null}）。
     */
    Session create(AgentConfig agentConfig);
}
