package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.middleware.DynamicSubagentsMiddleware;
import io.agentscope.harness.agent.middleware.SubagentEntry;
import io.agentscope.harness.agent.subagent.DefaultAgentManager;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.SubagentFactory;
import io.agentscope.harness.agent.tool.AgentSpawnTool;

import java.io.InputStream;
import java.lang.invoke.VarHandle;
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
    private static final String AGENTSCOPE_HARNESS_VERSION = "2.0.2";
    private static final String HARNESS_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static volatile Field initialPermissionContextField;
    private static volatile List<Field> dynamicMiddlewareFields;

    private HarnessAgentBuilderSubagentPermissionBridge() {
    }

    static void inheritDeclaredLocalPermissions(
            HarnessAgent parent, PermissionContextState parentPermissions) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(parentPermissions, "parentPermissions");
        sealDynamicFactories(parent, parentPermissions);
        DefaultAgentManager manager = parent.getSubagentAgentManager();
        inheritDeclaredLocalPermissions(parent, parentPermissions, manager);
    }

    private static void inheritDeclaredLocalPermissions(
            HarnessAgent parent,
            PermissionContextState parentPermissions,
            DefaultAgentManager manager) {
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
            SubagentFactory decorated = inheritingFactory(
                    factory, declaration, parent, parentPermissions);
            if (decorated != factory) {
                factory = decorated;
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

    private static SubagentFactory inheritingFactory(
            SubagentFactory factory,
            SubagentDeclaration declaration,
            HarnessAgent parent,
            PermissionContextState parentPermissions) {
        if (factory == null
                || declaration == null
                || !declaration.isInheritParentPermissions()
                || declaration.isRemote()
                || (factory instanceof InheritingFactory inheriting
                        && inheriting.parent == parent
                        && inheriting.parentPermissions == parentPermissions)) {
            return factory;
        }
        return new InheritingFactory(factory, parent, parentPermissions);
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
            ReActAgent delegateAgent;
            if (child instanceof HarnessAgent harness) {
                delegateAgent = harness.getDelegate();
            }
            else if (child instanceof ReActAgent concreteAgent) {
                delegateAgent = concreteAgent;
            }
            else {
                closeQuietly(child);
                throw new AgentConfigException(
                        "local declared subagent must expose an Agent permission context");
            }
            requireCompatibleChild(delegateAgent);
            setInitialPermissionContext(delegateAgent, effectiveParentPermissions(parentContext));
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

    private static void sealDynamicFactories(
            HarnessAgent parent, PermissionContextState parentPermissions) {
        for (MiddlewareBase middleware : parent.getDelegate().getMiddlewares()) {
            if (middleware instanceof DynamicSubagentsMiddleware dynamic) {
                sealDynamicFactories(dynamic, parent, parentPermissions);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void sealDynamicFactories(
            DynamicSubagentsMiddleware dynamic,
            HarnessAgent parent,
            PermissionContextState parentPermissions) {
        try {
            List<Field> fields = dynamicMiddlewareFields();
            List<SubagentEntry> staticEntries = (List<SubagentEntry>) fields.get(0).get(dynamic);
            Function<SubagentDeclaration, SubagentFactory> factoryBuilder =
                    (Function<SubagentDeclaration, SubagentFactory>) fields.get(1).get(dynamic);
            if (factoryBuilder instanceof InheritingFactoryBuilder inheriting
                    && inheriting.parent == parent
                    && inheriting.parentPermissions == parentPermissions) {
                return;
            }
            List<SubagentEntry> decoratedEntries = staticEntries.stream()
                    .map(entry -> new SubagentEntry(
                            entry.name(),
                            entry.description(),
                            inheritingFactory(
                                    entry.factory(),
                                    entry.declaration(),
                                    parent,
                                    parentPermissions),
                            entry.declaration()))
                    .toList();
            Function<SubagentDeclaration, SubagentFactory> decoratedBuilder =
                    new InheritingFactoryBuilder(factoryBuilder, parent, parentPermissions);
            fields.get(0).set(dynamic, decoratedEntries);
            fields.get(1).set(dynamic, decoratedBuilder);
            // Both final references are replaced during build, before HarnessAgentRuntime is
            // published. Complete the reflective initialization before any call thread can run.
            VarHandle.fullFence();
            if (fields.get(0).get(dynamic) != decoratedEntries
                    || fields.get(1).get(dynamic) != decoratedBuilder) {
                throw incompatibleHarness(
                        "dynamic subagent final fields rejected the permission decorator");
            }
        }
        catch (IllegalAccessException failure) {
            throw incompatibleHarness(
                    "cannot install dynamic subagent permission decorator", failure);
        }
    }

    private static List<Field> dynamicMiddlewareFields() {
        List<Field> resolved = dynamicMiddlewareFields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderSubagentPermissionBridge.class) {
            if (dynamicMiddlewareFields == null) {
                requireHarnessVersion();
                dynamicMiddlewareFields = validateDynamicMiddlewareShape(
                        DynamicSubagentsMiddleware.class);
            }
            return dynamicMiddlewareFields;
        }
    }

    static List<Field> validateDynamicMiddlewareShape(Class<?> middlewareType) {
        try {
            Field staticEntries = requirePrivateFinalField(
                    middlewareType, "staticEntries", List.class);
            Field factoryBuilder = requirePrivateFinalField(
                    middlewareType, "factoryBuilder", Function.class);
            return List.of(staticEntries, factoryBuilder);
        }
        catch (NoSuchFieldException failure) {
            throw incompatibleHarness("dynamic subagent middleware shape changed", failure);
        }
    }

    private static Field requirePrivateFinalField(
            Class<?> owner, String name, Class<?> expectedType) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        int modifiers = field.getModifiers();
        if (field.getType() != expectedType
                || !Modifier.isPrivate(modifiers)
                || !Modifier.isFinal(modifiers)
                || Modifier.isStatic(modifiers)
                || !field.trySetAccessible()) {
            throw incompatibleHarness(owner.getSimpleName() + "." + name + " shape changed");
        }
        return field;
    }

    private static void requireHarnessVersion() {
        Properties properties = new Properties();
        try (InputStream input = HarnessAgent.class.getClassLoader()
                .getResourceAsStream(HARNESS_VERSION_RESOURCE)) {
            if (input == null) {
                throw incompatibleHarness("version metadata is unavailable");
            }
            properties.load(input);
        }
        catch (Exception failure) {
            if (failure instanceof AgentConfigException configFailure) {
                throw configFailure;
            }
            throw incompatibleHarness("cannot read version metadata", failure);
        }
        if (!AGENTSCOPE_HARNESS_VERSION.equals(properties.getProperty("version"))) {
            throw incompatibleHarness("unexpected AgentScope Harness version");
        }
    }

    private static AgentConfigException incompatibleHarness(String detail) {
        return incompatibleHarness(detail, null);
    }

    private static AgentConfigException incompatibleHarness(String detail, Throwable cause) {
        return new AgentConfigException(
                "Harness subagent permission bridge requires agentscope-harness 2.0.2: "
                        + detail,
                cause);
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

    private static final class InheritingFactoryBuilder
            implements Function<SubagentDeclaration, SubagentFactory> {
        private final Function<SubagentDeclaration, SubagentFactory> delegate;
        private final HarnessAgent parent;
        private final PermissionContextState parentPermissions;

        private InheritingFactoryBuilder(
                Function<SubagentDeclaration, SubagentFactory> delegate,
                HarnessAgent parent,
                PermissionContextState parentPermissions) {
            this.delegate = delegate;
            this.parent = Objects.requireNonNull(parent, "parent");
            this.parentPermissions = Objects.requireNonNull(
                    parentPermissions, "parentPermissions");
        }

        @Override
        public SubagentFactory apply(SubagentDeclaration declaration) {
            if (delegate == null) {
                return null;
            }
            return inheritingFactory(
                    delegate.apply(declaration), declaration, parent, parentPermissions);
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
            HarnessAgent bound = Objects.requireNonNull(parentAgent, "parentAgent");
            if (!parent.compareAndSet(null, bound)) {
                throw new AgentConfigException("subagent permission guard was already bound");
            }
            sealDynamicFactories(bound, parentPermissions);
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE + 1;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent,
                RuntimeContext context,
                AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            HarnessAgent harness = parent.get();
            DefaultAgentManager scoped = context != null
                    ? context.get(AgentSpawnTool.CTX_AGENT_MANAGER, DefaultAgentManager.class)
                    : null;
            if (harness != null && scoped != null) {
                inheritDeclaredLocalPermissions(harness, parentPermissions, scoped);
            }
            return next.apply(input);
        }

    }
}
