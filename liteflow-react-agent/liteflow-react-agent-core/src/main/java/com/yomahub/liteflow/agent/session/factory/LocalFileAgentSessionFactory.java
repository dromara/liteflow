package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.LocalFileMemoryConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 通过把 JSON 文件存储在 {@code workspace.root/.agent-session/<sessionId>/}
 * 下来支持 {@link MemoryStorageMode#LOCAL_FILE}。
 *
 * <p>session 存储子目录与各 session 的 workspace（{@code workspace.root/<sessionId>/}）
 * 平级而非嵌套：一方面避免 {@link com.yomahub.liteflow.agent.tool.WorkspaceFileTools}
 * 读到或覆盖 agent 自己的记忆；另一方面让 {@code cleanup-on-session-expire}
 * 在递归清空 workspace 子目录时不会误删持久化的记忆，
 * 与 Redis、MySQL 后端的"持久化与 workspace 生命周期解耦"语义保持一致。
 */
public class LocalFileAgentSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.LOCAL_FILE;
    }

    @Override
    public Session create(AgentConfig cfg) {
        if (cfg.getWorkspace() == null || cfg.getWorkspace().getRoot() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.workspace.root is required when session.memory.mode=LOCAL_FILE");
        }
        Path root = Paths.get(cfg.getWorkspace().getRoot()).toAbsolutePath().normalize()
                .resolve(LocalFileMemoryConfig.SUB_DIR);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new AgentConfigException("cannot create session storage dir: " + root, e);
        }
        return new JsonSession(root);
    }
}
