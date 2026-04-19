package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;

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

public class AgentSessionManager implements AutoCloseable {

    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9_\\-]+");

    private final AgentConfig config;
    private final Path root;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

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
        long every = Math.max(20, config.getSession().getCleanupInterval().toMillis());
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liteflow-agent-session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleWithFixedDelay(this::cleanup, every, every, TimeUnit.MILLISECONDS);
    }

    public AgentSession acquire(String sessionId) {
        String safe = safeId(sessionId);
        AgentSession s = sessions.computeIfAbsent(safe, id -> {
            Path ws = root.resolve(id);
            try { Files.createDirectories(ws); }
            catch (IOException e) { throw new AgentConfigException("cannot create workspace: " + ws, e); }
            return new AgentSession(id, ws);
        });
        s.touch();
        enforceMaxSessions();
        return s;
    }

    public boolean contains(String sessionId) {
        return sessions.containsKey(safeId(sessionId));
    }

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
                    .ifPresent(victim -> remove(victim, true));
        }
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(config.getSession().getIdleTimeout());
        for (AgentSession s : sessions.values()) {
            if (s.getLastActive().isAfter(cutoff)) continue;
            if (!s.getLock().tryLock()) continue;
            try {
                remove(s, config.getWorkspace().isCleanupOnSessionExpire());
            } finally {
                s.getLock().unlock();
            }
        }
    }

    private void remove(AgentSession s, boolean cleanWorkspace) {
        sessions.remove(s.getSessionId(), s);
        if (cleanWorkspace) {
            deleteRecursively(s.getWorkspaceDir());
        }
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
        sessions.clear();
    }
}
