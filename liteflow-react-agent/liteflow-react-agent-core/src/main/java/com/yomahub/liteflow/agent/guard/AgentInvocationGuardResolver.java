package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.property.agent.DistributedCoordinationMode;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Objects;
import java.util.function.Consumer;

/** Resolves the configured guard and validates distributed coordination declarations. */
public final class AgentInvocationGuardResolver {

    private final AgentInvocationGuard localGuard = new LocalAgentInvocationGuard();
    private final Consumer<String> warningSink;

    public AgentInvocationGuardResolver() {
        this(message -> { });
    }

    public AgentInvocationGuardResolver(Consumer<String> warningSink) {
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    public AgentInvocationGuard resolve(AgentConfig config) {
        validate(config);
        AgentInvocationGuardConfig guardConfig = config.getInvocationGuard();
        if (guardConfig.getMode() == null || guardConfig.getMode() == AgentInvocationGuardMode.LOCAL) {
            return localGuard;
        }
        if (guardConfig.getMode() != AgentInvocationGuardMode.BEAN) {
            throw new AgentConfigException("Unsupported invocation guard mode: " + guardConfig.getMode());
        }
        String beanName = guardConfig.getBeanName();
        if (beanName == null || beanName.isBlank()) {
            throw new AgentConfigException("liteflow.agent.invocation-guard.bean-name is required when mode=BEAN");
        }
        Object candidate = ContextAwareHolder.loadContextAware().getBean(beanName);
        if (!(candidate instanceof AgentInvocationGuard)) {
            throw new AgentConfigException("Invocation guard bean '" + beanName
                    + "' must implement " + AgentInvocationGuard.class.getName());
        }
        return (AgentInvocationGuard) candidate;
    }

    public void validate(AgentConfig config) {
        Objects.requireNonNull(config, "config");
        AgentInvocationGuardConfig guardConfig = config.getInvocationGuard();
        if (guardConfig == null) {
            throw new AgentConfigException("liteflow.agent.invocation-guard must not be null");
        }
        if (!usesPotentiallyDistributedStore(config)
                || guardConfig.getCoordinationMode() != DistributedCoordinationMode.NONE) {
            return;
        }
        String message = "StateStore type BEAN may be distributed, but invocationGuard.coordinationMode=NONE";
        if (guardConfig.isStrictDistributed()) {
            throw new AgentConfigException(message + "; configure sticky routing or a distributed guard");
        }
        warningSink.accept(message + "; proceeding with local coordination because strictDistributed=false");
    }

    private static boolean usesPotentiallyDistributedStore(AgentConfig config) {
        return config.getStateStore() != null && config.getStateStore().getType() == AgentStateStoreType.BEAN;
    }
}
