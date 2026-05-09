package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.session.factory.AgentSessionFactoryRegistry;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.session.Session;
import io.agentscope.core.session.SessionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 跟踪当前 JVM 中存活的 {@link AgentSession}，并桥接到可插拔的
 * {@link Session}（由 {@link AgentSessionFactoryRegistry} 提供）。
 *
 * <p>会话标识被拆分为两个维度：
 * <ul>
 *   <li>{@code conversationId}：业务/对话维度，决定 workspace 目录与连续对话的恢复。</li>
 *   <li>{@code agentKey}：组件维度（默认为 {@code nodeId}），用于在同一段对话内
 *       区分不同 agent 的 ReActAgent 实例与对话记忆。</li>
 * </ul>
 *
 * <p>缓存与持久化都按 {@code (conversationId, agentKey)} 组合 key 隔离；
 * workspace 目录则只按 {@code conversationId} 建一份，让同一段对话中的多个 agent
 * 通过共享文件协作。
 */
public class AgentSessionManager implements AutoCloseable {

    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9_\\-]+");

    /** 缓存 key 内部使用的分隔符；不在 SAFE 字符集中以外，且不会出现在 NanoId 输出里。 */
    static final String KEY_SEPARATOR = "__";

    private final AgentConfig config;
    private final Path root;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;
    /** memory 模式为 NONE 时可能为 null。 */
    private final Session storage;

    public AgentSessionManager(AgentConfig config) {
        this.config = config;
        if (config == null || config.getWorkspace() == null || config.getWorkspace().getRoot() == null) {
            throw new AgentConfigException("liteflow.agent.workspace.root is required");
        }
        this.root = Paths.get(config.getWorkspace().getRoot()).toAbsolutePath().normalize();
        if (config.getWorkspace().isAutoCreate()) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new AgentConfigException("cannot create workspace root: " + root, e);
            }
        } else if (!Files.isDirectory(root)) {
            throw new AgentConfigException("workspace root does not exist: " + root);
        }
        this.storage = AgentSessionFactoryRegistry.createSession(config);
        long every = Math.max(20, config.getSession().getCleanupInterval().toMillis());
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liteflow-agent-session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleWithFixedDelay(this::cleanup, every, every, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取（或创建）一个 agent 会话。
     *
     * <p>同一 {@code conversationId} 下的多个 {@code agentKey} 共享同一个 workspace 目录，
     * 但分别拥有独立的 {@link AgentSession}（独立的 ReActAgent 实例、独立的记忆持久化 key）。
     */
    public AgentSession acquire(String conversationId, String agentKey) {
        String safeCid = safeId(conversationId);
        String safeKey = safeId(agentKey);
        String cacheKey = safeCid + KEY_SEPARATOR + safeKey;
        AgentSession s = sessions.computeIfAbsent(cacheKey, k -> {
            Path ws = root.resolve(safeCid);
            try {
                Files.createDirectories(ws);
            } catch (IOException e) {
                throw new AgentConfigException("cannot create workspace: " + ws, e);
            }
            return new AgentSession(safeCid, safeKey, k, ws);
        });
        s.touch();
        enforceMaxSessions();
        return s;
    }

    public boolean contains(String conversationId, String agentKey) {
        return sessions.containsKey(safeId(conversationId) + KEY_SEPARATOR + safeId(agentKey));
    }

    /**
     * 将之前持久化的状态懒加载恢复到 agent 中。
     * 同一个 {@code (conversationId, agentKey)} 在当前 JVM 生命周期内应只调用一次，并且应在 agent
     * 构建完成后、首次 {@code agent.call(...)} 前调用。
     */
    public void loadIfExists(AgentSession session, ReActAgent agent) {
        if (storage == null || agent == null) return;
        MemoryStorageConfig mc = config.getSession().getMemory();
        if (!mc.isLoadOnFirstUse()) return;
        if (mc.getMode() == MemoryStorageMode.NONE) return;
        SessionManager.forSessionId(session.getCacheKey())
                .withSession(storage)
                .addComponent(agent)
                .loadIfExists();
    }

    /** 持久化 agent 当前状态。失败会向调用方暴露。 */
    public void save(AgentSession session, ReActAgent agent) {
        if (storage == null || agent == null) return;
        MemoryStorageConfig mc = config.getSession().getMemory();
        if (mc.getMode() == MemoryStorageMode.NONE) return;
        SessionManager.forSessionId(session.getCacheKey())
                .withSession(storage)
                .addComponent(agent)
                .saveSession();
    }

    public Session storage() { return storage; }

    static String safeId(String raw) {
        if (raw == null || raw.isEmpty()) return "_";
        if (SAFE.matcher(raw).matches()) return raw;
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("%", "_");
    }

    private void enforceMaxSessions() {
        int max = config.getSession().getMaxSessions();
        while (sessions.size() > max) {
            sessions.values().stream()
                    .min(Comparator.comparing(AgentSession::getLastActive))
                    // LRU 淘汰只移除 JVM 内缓存；持久化数据保持不变。
                    .ifPresent(victim -> evictFromCache(victim, false));
        }
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(config.getSession().getIdleTimeout());
        for (AgentSession s : sessions.values()) {
            if (s.getLastActive().isAfter(cutoff)) continue;
            if (!s.getLock().tryLock()) continue;
            try {
                evictFromCache(s, config.getWorkspace().isCleanupOnSessionExpire());
            } finally {
                s.getLock().unlock();
            }
        }
    }

    /**
     * @param cleanWorkspace 为 true 时同时尝试删除磁盘上的 workspace 目录。由于同一 workspace
     *                       可能被 {@code (conversationId, *)} 下的多个 agent 共享，仅在该
     *                       conversation 下没有其他存活会话时才会真正删除目录。
     *                       存储在其他位置的持久化 session 状态（例如 workspaceRoot/.agent-session、
     *                       Redis、MySQL）不会在这里被删除。
     */
    private void evictFromCache(AgentSession s, boolean cleanWorkspace) {
        sessions.remove(s.getCacheKey(), s);
        if (cleanWorkspace && !hasSiblingInSameConversation(s)) {
            deleteRecursively(s.getWorkspaceDir());
        }
    }

    private boolean hasSiblingInSameConversation(AgentSession evicted) {
        String prefix = evicted.getConversationId() + KEY_SEPARATOR;
        for (String key : sessions.keySet()) {
            if (key.startsWith(prefix)) return true;
        }
        return false;
    }

    private static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (var walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }

    @Override
    public void close() {
        cleaner.shutdownNow();
        if (config.getWorkspace().isCleanupOnJvmShutdown()) {
            sessions.values().forEach(s -> deleteRecursively(s.getWorkspaceDir()));
        }
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) {}
        }
        sessions.clear();
    }
}
