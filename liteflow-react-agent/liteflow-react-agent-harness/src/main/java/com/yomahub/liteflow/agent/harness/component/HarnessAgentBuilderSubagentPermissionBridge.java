package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.middleware.SubagentEntry;
import io.agentscope.harness.agent.subagent.DefaultAgentManager;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.SubagentFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/** AgentScope Harness 2.0.2 compatibility bridge for declared child permission inheritance. */
final class HarnessAgentBuilderSubagentPermissionBridge {

    private static final String AGENTSCOPE_CORE_VERSION = "2.0.2";
    private static final String CORE_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-core/pom.properties";
    private static volatile Field initialPermissionContextField;

    private HarnessAgentBuilderSubagentPermissionBridge() {
    }

    static void inheritDeclaredLocalPermissions(
            HarnessAgent parent, PermissionContextState parentPermissions) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(parentPermissions, "parentPermissions");
        DefaultAgentManager manager = parent.getSubagentAgentManager();
        if (manager == null) {
            return;
        }
        Map<String, SubagentFactory> factories = manager.getAgentFactories();
        List<SubagentEntry> replacements = new ArrayList<>(factories.size());
        boolean changed = false;
        for (Map.Entry<String, SubagentFactory> entry : factories.entrySet()) {
            String name = entry.getKey();
            SubagentFactory factory = entry.getValue();
            SubagentDeclaration declaration = manager.getDeclaration(name).orElse(null);
            if (declaration != null
                    && declaration.isInheritParentPermissions()
                    && !declaration.isRemote()
                    && !(factory instanceof InheritingFactory inheriting
                            && inheriting.parent == parent
                            && inheriting.parentPermissions == parentPermissions)) {
                factory = new InheritingFactory(factory, parent, parentPermissions);
                changed = true;
            }
            replacements.add(new SubagentEntry(
                    name,
                    declaration != null ? declaration.getDescription() : name,
                    factory,
                    declaration));
        }
        if (changed) {
            manager.replaceAgents(replacements);
        }
    }

    static DynamicRefreshGuard dynamicRefreshGuard(PermissionContextState parentPermissions) {
        return new DynamicRefreshGuard(parentPermissions);
    }

    private static final class InheritingFactory implements SubagentFactory {
        private final SubagentFactory delegate;
        private final HarnessAgent parent;
        private final PermissionContextState parentPermissions;

        private InheritingFactory(
                SubagentFactory delegate,
                HarnessAgent parent,
                PermissionContextState parentPermissions) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.parent = Objects.requireNonNull(parent, "parent");
            this.parentPermissions = Objects.requireNonNull(
                    parentPermissions, "parentPermissions");
        }

        @Override
        public Agent create(RuntimeContext parentContext) {
            Agent child = delegate.create(parentContext);
            ReActAgent react;
            if (child instanceof HarnessAgent harness) {
                react = harness.getDelegate();
            }
            else if (child instanceof ReActAgent reActAgent) {
                react = reActAgent;
            }
            else {
                closeQuietly(child);
                throw new AgentConfigException(
                        "local declared subagent must expose a ReAct permission context");
            }
            requireCompatibleChild(react);
            setInitialPermissionContext(react, effectiveParentPermissions(parentContext));
            return child;
        }

        private PermissionContextState effectiveParentPermissions(RuntimeContext parentContext) {
            if (parentContext == null
                    || (parentContext.getUserId() == null
                            && (parentContext.getSessionId() == null
                                    || parentContext.getSessionId().isBlank()))) {
                return parentPermissions;
            }
            return parent.getDelegate()
                    .getAgentState(parentContext.getUserId(), parentContext.getSessionId())
                    .getPermissionContext();
        }
    }

    private static void requireCompatibleChild(ReActAgent child) {
        Properties properties = new Properties();
        try (InputStream input = child.getClass().getClassLoader()
                .getResourceAsStream(CORE_VERSION_RESOURCE)) {
            if (input == null) {
                throw new AgentConfigException(
                        "Harness subagent permission bridge requires agentscope-core 2.0.2 metadata");
            }
            properties.load(input);
        }
        catch (Exception failure) {
            if (failure instanceof AgentConfigException configFailure) {
                throw configFailure;
            }
            throw new AgentConfigException("cannot read agentscope-core version metadata", failure);
        }
        if (!AGENTSCOPE_CORE_VERSION.equals(properties.getProperty("version"))) {
            throw new AgentConfigException(
                    "Harness subagent permission bridge requires agentscope-core 2.0.2");
        }
    }

    private static void setInitialPermissionContext(
            ReActAgent child, PermissionContextState parentPermissions) {
        try {
            initialPermissionContextField().set(child, parentPermissions);
        }
        catch (IllegalAccessException failure) {
            throw new AgentConfigException(
                    "cannot install inherited subagent permission context", failure);
        }
    }

    private static Field initialPermissionContextField() {
        Field resolved = initialPermissionContextField;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderSubagentPermissionBridge.class) {
            if (initialPermissionContextField == null) {
                try {
                    Field field = ReActAgent.class.getDeclaredField("initialPermissionContext");
                    int modifiers = field.getModifiers();
                    if (field.getType() != PermissionContextState.class
                            || !Modifier.isPrivate(modifiers)
                            || !Modifier.isFinal(modifiers)
                            || !field.trySetAccessible()) {
                        throw new AgentConfigException(
                                "agentscope-core 2.0.2 permission context shape changed");
                    }
                    initialPermissionContextField = field;
                }
                catch (NoSuchFieldException failure) {
                    throw new AgentConfigException(
                            "agentscope-core 2.0.2 permission context shape changed", failure);
                }
            }
            return initialPermissionContextField;
        }
    }

    private static void closeQuietly(Agent child) {
        if (child instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            }
            catch (Exception ignored) {
                // Preserve the configuration failure.
            }
        }
    }

    /** Runs inside Harness dynamic-subagent refresh and re-wraps its newly materialized factories. */
    static final class DynamicRefreshGuard implements MiddlewareBase {
        private final PermissionContextState parentPermissions;
        private final AtomicReference<HarnessAgent> parent = new AtomicReference<>();

        private DynamicRefreshGuard(PermissionContextState parentPermissions) {
            this.parentPermissions = Objects.requireNonNull(parentPermissions, "parentPermissions");
        }

        void bind(HarnessAgent parentAgent) {
            if (!parent.compareAndSet(null, Objects.requireNonNull(parentAgent, "parentAgent"))) {
                throw new AgentConfigException("subagent permission guard was already bound");
            }
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE + 1;
        }

        @Override
        public Flux<AgentEvent> onReasoning(
                Agent agent,
                RuntimeContext context,
                ReasoningInput input,
                Function<ReasoningInput, Flux<AgentEvent>> next) {
            HarnessAgent harness = parent.get();
            if (harness != null) {
                inheritDeclaredLocalPermissions(harness, parentPermissions);
            }
            return next.apply(input);
        }
    }
}
