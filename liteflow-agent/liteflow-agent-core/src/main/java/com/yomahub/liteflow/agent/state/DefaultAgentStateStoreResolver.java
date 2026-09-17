package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Default JSON and ServiceLoader-backed session-store mapping.
 *
 * <p>JSON is handled here directly. REDIS and MYSQL are delegated to the
 * {@link AgentStateStoreProvider} SPI, implemented by the optional
 * liteflow-agent-redis / liteflow-agent-mysql companion modules; when the
 * module is missing the failure names the required dependency instead of failing with a
 * class-loading error.
 */
public final class DefaultAgentStateStoreResolver implements AgentStateStoreResolver {

	static final Map<AgentSessionStoreType, String> PROVIDER_MODULES =
			Map.of(AgentSessionStoreType.REDIS, "liteflow-agent-redis",
					AgentSessionStoreType.MYSQL, "liteflow-agent-mysql");

	private final Map<AgentSessionStoreType, AgentStateStoreProvider> providers;

	public DefaultAgentStateStoreResolver() {
		this(loadProviders());
	}

	DefaultAgentStateStoreResolver(Iterable<AgentStateStoreProvider> providers) {
		Map<AgentSessionStoreType, AgentStateStoreProvider> indexed = new EnumMap<>(AgentSessionStoreType.class);
		for (AgentStateStoreProvider provider : providers) {
			if (provider == null || provider.type() == null) {
				continue;
			}
			indexed.putIfAbsent(provider.type(), provider);
		}
		this.providers = indexed;
	}

	@Override
	public ResolvedAgentStateStore resolve(AgentSessionStoreConfig config) {
		if (config == null) {
			throw new AgentConfigException("liteflow.agent.session-store must not be null");
		}
		if (config.getType() == null) {
			throw new AgentConfigException("liteflow.agent.session-store.type must not be null");
		}
		return switch (config.getType()) {
			case JSON -> resolveJson(config);
			case REDIS, MYSQL -> resolveProvider(config);
		};
	}

	private static ResolvedAgentStateStore resolveJson(AgentSessionStoreConfig config) {
		String jsonRoot = config.getJsonRoot();
		if (jsonRoot == null || jsonRoot.isBlank()) {
			throw new AgentConfigException(
					"liteflow.agent.session-store.json-root is required when type=JSON");
		}
		try {
			return new ResolvedAgentStateStore(
					new JsonFileAgentStateStore(Path.of(jsonRoot)), true);
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"JSON state store could not be created from "
							+ "liteflow.agent.session-store.json-root",
					failure);
		}
	}

	private ResolvedAgentStateStore resolveProvider(AgentSessionStoreConfig config) {
		AgentStateStoreProvider provider = providers.get(config.getType());
		if (provider == null) {
			throw new AgentConfigException("session-store type " + config.getType()
					+ " requires the " + PROVIDER_MODULES.get(config.getType())
					+ " module on the classpath");
		}
		ResolvedAgentStateStore resolved = provider.resolve(config);
		if (resolved == null) {
			throw new AgentConfigException("AgentStateStoreProvider for " + config.getType()
					+ " must not return null");
		}
		return resolved;
	}

	private static Iterable<AgentStateStoreProvider> loadProviders() {
		return ServiceLoader.load(AgentStateStoreProvider.class,
				DefaultAgentStateStoreResolver.class.getClassLoader());
	}
}
