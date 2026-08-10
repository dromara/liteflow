package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/** Default MEMORY, JSON and container-BEAN state-store mapping. */
public final class DefaultAgentStateStoreResolver implements AgentStateStoreResolver {

    private final Function<String, Object> beanLookup;

    public DefaultAgentStateStoreResolver() {
        this(name -> ContextAwareHolder.loadContextAware().getBean(name));
    }

    public DefaultAgentStateStoreResolver(Function<String, Object> beanLookup) {
        this.beanLookup = Objects.requireNonNull(beanLookup, "beanLookup");
    }

    @Override
    public ResolvedAgentStateStore resolve(AgentStateStoreConfig config) {
        if (config == null) {
            throw new AgentConfigException("liteflow.agent.state-store must not be null");
        }
        if (config.getType() == null) {
            throw new AgentConfigException("liteflow.agent.state-store.type must not be null");
        }
        return switch (config.getType()) {
            case MEMORY -> new ResolvedAgentStateStore(new InMemoryAgentStateStore(), true);
            case JSON -> resolveJson(config);
            case BEAN -> resolveBean(config);
        };
    }

    private static ResolvedAgentStateStore resolveJson(AgentStateStoreConfig config) {
        String jsonRoot = config.getJsonRoot();
        if (jsonRoot == null || jsonRoot.isBlank()) {
            throw new AgentConfigException(
                    "liteflow.agent.state-store.json-root is required when type=JSON");
        }
        return new ResolvedAgentStateStore(new JsonFileAgentStateStore(Path.of(jsonRoot)), true);
    }

    private ResolvedAgentStateStore resolveBean(AgentStateStoreConfig config) {
        String beanName = config.getBeanName();
        if (beanName == null || beanName.isBlank()) {
            throw new AgentConfigException(
                    "liteflow.agent.state-store.bean-name is required when type=BEAN");
        }
        Object candidate;
        try {
            candidate = beanLookup.apply(beanName);
        } catch (RuntimeException | LinkageError failure) {
            throw new AgentConfigException(
                    "State store bean '" + beanName + "' could not be resolved", failure);
        }
        if (!(candidate instanceof AgentStateStore store)) {
            throw new AgentConfigException("State store bean '" + beanName + "' must implement "
                    + AgentStateStore.class.getName());
        }
        return new ResolvedAgentStateStore(store, false);
    }
}
