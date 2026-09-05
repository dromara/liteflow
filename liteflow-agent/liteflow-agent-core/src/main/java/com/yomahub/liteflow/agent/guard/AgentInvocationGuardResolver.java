package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Objects;

/** Resolves the configured guard. */
public final class AgentInvocationGuardResolver {

    private static final AgentInvocationGuard PROCESS_GUARD = new LocalAgentInvocationGuard();

    public AgentInvocationGuard resolve(AgentConfig config) {
        validate(config);
        AgentInvocationGuardConfig guardConfig = config.getInvocationGuard();
        if (guardConfig.getMode() == null || guardConfig.getMode() == AgentInvocationGuardMode.LOCAL) {
            return PROCESS_GUARD;
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
        if (config.getInvocationGuard() == null) {
            throw new AgentConfigException("liteflow.agent.invocation-guard must not be null");
        }
    }
}
