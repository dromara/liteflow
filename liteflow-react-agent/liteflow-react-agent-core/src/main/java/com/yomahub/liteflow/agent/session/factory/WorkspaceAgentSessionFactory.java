package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import com.yomahub.liteflow.property.agent.WorkspaceMemoryConfig;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 通过把 JSON 文件存储在 {@code workspace.root/.agent-session/<sessionId>/}
 * 下来支持 {@link MemoryStorageMode#WORKSPACE_FILE}。
 *
 * <p>session 存储子目录特意与 {@code workspace.root/<sessionId>/}
 * 形式的单 session 工具 workspace 分离，避免
 * {@link com.yomahub.liteflow.agent.tool.WorkspaceFileTools} 读取或覆盖
 * agent 自身记忆。
 */
public class WorkspaceAgentSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.WORKSPACE_FILE;
    }

    @Override
    public Session create(AgentConfig cfg) {
        if (cfg.getWorkspace() == null || cfg.getWorkspace().getRoot() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.workspace.root is required when session.memory.mode=WORKSPACE_FILE");
        }
        Path root = Paths.get(cfg.getWorkspace().getRoot()).toAbsolutePath().normalize()
                .resolve(WorkspaceMemoryConfig.SUB_DIR);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new AgentConfigException("cannot create session storage dir: " + root, e);
        }
        return new JsonSession(root);
    }
}
