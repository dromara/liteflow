package com.yomahub.liteflow.agent.anthropic;

import com.anthropic.client.AnthropicClient;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

/** Version-pinned bridge for the Anthropic client owned by AgentScope's model. */
final class AnthropicClientBridge {

    private static final String AGENTSCOPE_VERSION = "2.0.3";
    private static final String ANTHROPIC_VERSION = "2.14.0";
    private static volatile Field clientField;

    private AnthropicClientBridge() {
    }

    static void verifyContract() {
        clientField();
    }

    static void close(Object model) {
        if (!(model instanceof AnthropicChatModel anthropicModel)) {
            throw incompatible("model must be AnthropicChatModel");
        }
        try {
            Object client = clientField().get(anthropicModel);
            if (!(client instanceof AnthropicClient anthropicClient)) {
                throw incompatible("client field must contain AnthropicClient");
            }
            anthropicClient.close();
        } catch (IllegalAccessException exception) {
            throw incompatible("cannot access AnthropicChatModel.client", exception);
        }
    }

    private static Field clientField() {
        Field resolved = clientField;
        if (resolved != null) {
            return resolved;
        }
        synchronized (AnthropicClientBridge.class) {
            if (clientField == null) {
                requireAgentScopeVersion();
                requireAnthropicVersion();
                Field field = validateShape(AnthropicChatModel.class, AnthropicClient.class);
                if (!field.trySetAccessible()) {
                    throw incompatible("AnthropicChatModel.client is inaccessible");
                }
                clientField = field;
            }
            return clientField;
        }
    }

    static Field validateShape(Class<?> modelType, Class<?> clientType) {
        try {
            Field field = modelType.getDeclaredField("client");
            if (!Modifier.isPrivate(field.getModifiers())
                    || !Modifier.isFinal(field.getModifiers())
                    || field.getType() != clientType) {
                throw incompatible("AnthropicChatModel.client signature changed");
            }
            Method close = clientType.getMethod("close");
            if (close.getParameterCount() != 0 || close.getReturnType() != Void.TYPE) {
                throw incompatible("AnthropicClient.close signature changed");
            }
            return field;
        } catch (NoSuchFieldException | NoSuchMethodException exception) {
            throw incompatible("Anthropic client bridge no longer matches upstream", exception);
        }
    }

    private static void requireAgentScopeVersion() {
        Properties properties = new Properties();
        try (var input = AnthropicChatModel.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/io.agentscope/agentscope-extensions-model-anthropic/pom.properties")) {
            if (input == null) {
                throw incompatible("AgentScope extension version metadata is unavailable");
            }
            properties.load(input);
        } catch (Exception exception) {
            if (exception instanceof AgentConfigException configException) {
                throw configException;
            }
            throw incompatible("cannot read AgentScope extension version metadata", exception);
        }
        if (!AGENTSCOPE_VERSION.equals(properties.getProperty("version"))) {
            throw incompatible("unexpected AgentScope extension version "
                    + properties.getProperty("version"));
        }
    }

    private static void requireAnthropicVersion() {
        String version = AnthropicClient.class.getPackage().getImplementationVersion();
        if (!ANTHROPIC_VERSION.equals(version)) {
            throw incompatible("unexpected anthropic-java version " + version);
        }
    }

    private static AgentConfigException incompatible(String detail) {
        return incompatible(detail, null);
    }

    private static AgentConfigException incompatible(String detail, Throwable cause) {
        return new AgentConfigException(
                "Anthropic client ownership requires AgentScope Anthropic 2.0.3 and "
                        + "anthropic-java 2.14.0: " + detail,
                cause);
    }
}
