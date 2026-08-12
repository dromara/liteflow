package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.harness.agent.HarnessAgent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Version-pinned fail-closed policy and inspection of the final Harness permission context. */
final class HarnessAgentBuilderPermissionBridge {

    private static final String AGENTSCOPE_VERSION = "2.0.2";
    private static final String HARNESS_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final String CORE_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-core/pom.properties";
    private static final List<String> DANGEROUS_HARNESS_TOOLS =
            List.of("execute", "write_file", "edit_file");
    private static volatile Fields fields;

    private HarnessAgentBuilderPermissionBridge() {
    }

    /**
     * Makes the upstream 2.0.2 {@code DEFAULT} contract explicit without changing filesystem
     * boundaries: its documented fallback is ASK, but a completely empty state selects a legacy
     * lightweight path. Exact ASK rules for Harness' execute/write tools engage the full engine.
     */
    static PermissionContextState failClosed(PermissionContextState configured) {
        if (configured != null && configured.getMode() == PermissionMode.BYPASS) {
            throw new AgentConfigException("Harness permission mode BYPASS is prohibited");
        }
        if (configured != null && !configured.isTrivial()) {
            return configured;
        }
        PermissionContextState.Builder builder =
                PermissionContextState.builder().mode(PermissionMode.DEFAULT);
        for (String toolName : DANGEROUS_HARNESS_TOOLS) {
            builder.addAskRule(
                    toolName,
                    new PermissionRule(
                            toolName,
                            null,
                            PermissionBehavior.ASK,
                            "liteflowHarnessDefault"));
        }
        return builder.build();
    }

    static PermissionSnapshot snapshot(
            HarnessAgent.Builder builder, PermissionContextState expected) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(expected, "expected");
        try {
            Fields resolved = fields();
            Object inner = resolved.inner().get(builder);
            PermissionContextState actual =
                    (PermissionContextState) resolved.permissionContext().get(inner);
            requireExpected(actual, expected);
            return new PermissionSnapshot(inner, expected);
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect final permission context", failure);
        }
    }

    static MiddlewareSnapshot installInnermostGuard(
            HarnessAgent.Builder builder, MiddlewareBase guard) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(guard, "guard");
        if (guard.order() != Integer.MIN_VALUE) {
            throw incompatible("mandatory middleware must use the innermost order");
        }
        try {
            Fields resolved = fields();
            @SuppressWarnings("unchecked")
            List<MiddlewareBase> middlewares =
                    (List<MiddlewareBase>) resolved.middlewares().get(builder);
            if (identityCount(middlewares, guard) != 0) {
                throw incompatible("mandatory middleware was registered before final assembly");
            }
            builder.middleware(guard);
            requireLastIdentity(middlewares, guard);
            return new MiddlewareSnapshot(middlewares, guard);
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot install mandatory middleware", failure);
        }
    }

    private static long identityCount(List<MiddlewareBase> middlewares, MiddlewareBase expected) {
        return middlewares.stream().filter(middleware -> middleware == expected).count();
    }

    private static void requireLastIdentity(
            List<MiddlewareBase> middlewares, MiddlewareBase expected) {
        if (identityCount(middlewares, expected) != 1
                || middlewares.isEmpty()
                || middlewares.get(middlewares.size() - 1) != expected) {
            throw incompatible("mandatory middleware is not the unique final registration");
        }
    }

    private static void requireExpected(
            PermissionContextState actual, PermissionContextState expected) {
        if (actual != expected) {
            throw new AgentConfigException(
                    "customizeHarness must retain the final permission context identity");
        }
        if (actual.getMode() == PermissionMode.BYPASS) {
            throw new AgentConfigException("Harness permission mode BYPASS is prohibited");
        }
    }

    private static Fields fields() {
        Fields resolved = fields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderPermissionBridge.class) {
            if (fields == null) {
                requireVersion(HarnessAgent.class, HARNESS_VERSION_RESOURCE, "Harness");
                requireVersion(ReActAgent.class, CORE_VERSION_RESOURCE, "core");
                fields = validateShape();
            }
            return fields;
        }
    }

    private static Fields validateShape() {
        try {
            Field inner = HarnessAgent.Builder.class.getDeclaredField("inner");
            int innerModifiers = inner.getModifiers();
            if (!Modifier.isPrivate(innerModifiers)
                    || !Modifier.isFinal(innerModifiers)
                    || Modifier.isStatic(innerModifiers)
                    || inner.getType() != ReActAgent.Builder.class
                    || !inner.trySetAccessible()) {
                throw incompatible("HarnessAgent.Builder.inner signature changed");
            }
            Field permission =
                    ReActAgent.Builder.class.getDeclaredField("permissionContext");
            int permissionModifiers = permission.getModifiers();
            if (!Modifier.isPrivate(permissionModifiers)
                    || Modifier.isFinal(permissionModifiers)
                    || Modifier.isStatic(permissionModifiers)
                    || permission.getType() != PermissionContextState.class
                    || !permission.trySetAccessible()) {
                throw incompatible("ReActAgent.Builder.permissionContext signature changed");
            }
            Field middlewares = HarnessAgent.Builder.class.getDeclaredField("middlewares");
            int middlewareModifiers = middlewares.getModifiers();
            if (Modifier.isPrivate(middlewareModifiers)
                    || Modifier.isProtected(middlewareModifiers)
                    || Modifier.isPublic(middlewareModifiers)
                    || !Modifier.isFinal(middlewareModifiers)
                    || Modifier.isStatic(middlewareModifiers)
                    || middlewares.getType() != List.class
                    || !middlewares.trySetAccessible()) {
                throw incompatible("HarnessAgent.Builder.middlewares signature changed");
            }
            return new Fields(inner, permission, middlewares);
        }
        catch (NoSuchFieldException failure) {
            throw incompatible("AgentScope permission builder shape changed", failure);
        }
    }

    private static void requireVersion(Class<?> owner, String resource, String artifact) {
        Properties properties = new Properties();
        try (var input = owner.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw incompatible("AgentScope " + artifact + " version metadata is unavailable");
            }
            properties.load(input);
        }
        catch (Exception failure) {
            if (failure instanceof AgentConfigException configFailure) {
                throw configFailure;
            }
            throw incompatible(
                    "cannot read AgentScope " + artifact + " version metadata", failure);
        }
        String version = properties.getProperty("version");
        if (!AGENTSCOPE_VERSION.equals(version)) {
            throw incompatible("unexpected AgentScope " + artifact + " version " + version);
        }
    }

    private static AgentConfigException incompatible(String detail) {
        return incompatible(detail, null);
    }

    private static AgentConfigException incompatible(String detail, Throwable cause) {
        return new AgentConfigException(
                "Harness permission guard requires AgentScope 2.0.2: " + detail, cause);
    }

    private record Fields(Field inner, Field permissionContext, Field middlewares) {
    }

    record PermissionSnapshot(Object inner, PermissionContextState permissionContext) {

        PermissionSnapshot {
            Objects.requireNonNull(inner, "inner");
            Objects.requireNonNull(permissionContext, "permissionContext");
        }

        void requireUnchanged(HarnessAgent.Builder builder) {
            Objects.requireNonNull(builder, "builder");
            try {
                Fields resolved = fields();
                Object actualInner = resolved.inner().get(builder);
                if (actualInner != inner) {
                    throw new AgentConfigException(
                            "customizeHarness must retain the Harness permission builder identity");
                }
                PermissionContextState actual =
                        (PermissionContextState) resolved.permissionContext().get(actualInner);
                requireExpected(actual, permissionContext);
            }
            catch (IllegalAccessException failure) {
                throw incompatible("cannot re-inspect final permission context", failure);
            }
        }
    }

    record MiddlewareSnapshot(List<MiddlewareBase> builderMiddlewares, MiddlewareBase guard) {

        MiddlewareSnapshot {
            Objects.requireNonNull(builderMiddlewares, "builderMiddlewares");
            Objects.requireNonNull(guard, "guard");
        }

        void requireUnchanged(HarnessAgent.Builder builder) {
            try {
                Object actual = fields().middlewares().get(builder);
                if (actual != builderMiddlewares) {
                    throw incompatible("mandatory middleware builder list identity changed");
                }
                requireLastIdentity(builderMiddlewares, guard);
            }
            catch (IllegalAccessException failure) {
                throw incompatible("cannot re-inspect mandatory middleware", failure);
            }
        }

        void requireFinal(HarnessAgent agent) {
            requireLastIdentity(agent.getDelegate().getMiddlewares(), guard);
        }
    }
}
