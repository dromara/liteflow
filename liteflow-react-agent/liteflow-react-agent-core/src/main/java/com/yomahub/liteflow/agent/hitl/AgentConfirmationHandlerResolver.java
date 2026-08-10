package com.yomahub.liteflow.agent.hitl;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Resolves at most one container handler for each invocation. */
public final class AgentConfirmationHandlerResolver {

    private final Supplier<Map<String, AgentConfirmationHandler>> beanLookup;

    public AgentConfirmationHandlerResolver() {
        this(AgentConfirmationHandlerResolver::lookupContainerHandlers);
    }

    AgentConfirmationHandlerResolver(
            Supplier<Map<String, AgentConfirmationHandler>> beanLookup) {
        this.beanLookup = Objects.requireNonNull(beanLookup, "beanLookup");
    }

    public AgentConfirmationHandler resolve(AgentConfirmationHandler explicit) {
        if (explicit != null) {
            return explicit;
        }
        Map<String, AgentConfirmationHandler> handlers;
        try {
            handlers = beanLookup.get();
        } catch (RuntimeException | LinkageError failure) {
            throw new AgentConfigException(
                    "AgentConfirmationHandler container lookup failed", failure);
        }
        if (handlers == null || handlers.isEmpty()) {
            return null;
        }
        if (handlers.size() > 1) {
            throw new AgentConfigException(
                    "Multiple AgentConfirmationHandler beans found: " + handlers.keySet());
        }
        AgentConfirmationHandler handler = handlers.values().iterator().next();
        if (handler == null) {
            throw new AgentConfigException(
                    "AgentConfirmationHandler bean must not be null");
        }
        return handler;
    }

    private static Map<String, AgentConfirmationHandler> lookupContainerHandlers() {
        ContextAware contextAware;
        try {
            contextAware = ContextAwareHolder.loadContextAware();
        } catch (RuntimeException | LinkageError failure) {
            // A container SPI may be visible while its optional framework is absent.
            // That is the same non-container environment represented by LocalContextAware.
            contextAware = new LocalContextAware();
        }
        return contextAware.getBeansOfType(AgentConfirmationHandler.class);
    }
}
