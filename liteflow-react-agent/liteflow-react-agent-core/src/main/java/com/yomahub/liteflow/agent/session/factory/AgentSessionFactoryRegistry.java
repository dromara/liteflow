package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.session.Session;

import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 根据指定模式解析合适的 {@link AgentSessionFactory}。
 *
 * <p>解析顺序：
 * <ol>
 *   <li>通过 {@link ServiceLoader} 注册的外部工厂</li>
 *   <li>框架内置工厂（JVM、workspace、Redis、MySQL、none）</li>
 * </ol>
 * 出现冲突时外部工厂优先，因此用户可以覆盖内置实现
 * （例如用自定义加密 JSON 工厂替换默认 workspace 工厂）。
 */
public final class AgentSessionFactoryRegistry {

    private static final Map<MemoryStorageMode, AgentSessionFactory> FACTORIES = new EnumMap<>(MemoryStorageMode.class);

    static {
        // 先注册内置实现；如果存在 SPI 实现，再由 SPI 覆盖。
        register(new InMemoryAgentSessionFactory());
        register(new WorkspaceAgentSessionFactory());
        register(new RedisAgentSessionFactory());
        register(new MysqlAgentSessionFactory());
        register(new NoneAgentSessionFactory());
        for (AgentSessionFactory f : ServiceLoader.load(AgentSessionFactory.class)) {
            register(f);
        }
    }

    private AgentSessionFactoryRegistry() { }

    private static void register(AgentSessionFactory f) {
        FACTORIES.put(f.mode(), f);
    }

    /** 根据配置模式构建 Session。{@link MemoryStorageMode#NONE} 可能返回 {@code null}。 */
    public static Session createSession(AgentConfig cfg) {
        MemoryStorageMode mode = cfg.getSession().getMemory().getMode();
        AgentSessionFactory f = FACTORIES.get(mode);
        if (f == null) {
            throw new AgentConfigException("No AgentSessionFactory registered for mode: " + mode);
        }
        return f.create(cfg);
    }
}
