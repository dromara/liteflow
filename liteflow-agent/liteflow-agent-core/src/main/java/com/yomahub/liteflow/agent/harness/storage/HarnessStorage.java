package com.yomahub.liteflow.agent.harness.storage;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/** An authoritative workspace store; no local cache or fallback is created. */
public final class HarnessStorage implements AutoCloseable {
    private final BaseStore store;
    private final AutoCloseable cleanup;
    private final AtomicBoolean closed = new AtomicBoolean();

    public HarnessStorage(BaseStore store, AutoCloseable cleanup) {
        this.store = Objects.requireNonNull(store);
        this.cleanup = Objects.requireNonNull(cleanup);
    }
    public BaseStore store() { return store; }

    public static HarnessStorage open(AgentSessionStoreConfig config) {
        var providers = ServiceLoader.load(HarnessStorageProvider.class).stream()
                .map(ServiceLoader.Provider::get).filter(p -> p.type() == config.getType()).toList();
        if (providers.size() != 1) throw new AgentConfigException(
                "Exactly one HarnessStorageProvider is required for " + config.getType());
        return providers.get(0).open(config);
    }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { cleanup.close(); }
        catch (Exception failure) { throw new IllegalStateException("Cannot close Harness storage", failure); }
    }
}
