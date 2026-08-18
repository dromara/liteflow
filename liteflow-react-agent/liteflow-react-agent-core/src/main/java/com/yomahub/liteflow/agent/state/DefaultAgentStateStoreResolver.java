package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Default JSON and ServiceLoader-backed state-store mapping.
 *
 * <p>JSON is handled here directly. REDIS and MYSQL are delegated to the
 * {@link AgentStateStoreProvider} SPI, implemented by the optional
 * liteflow-react-agent-redis / liteflow-react-agent-mysql companion modules; when the
 * module is missing the failure names the required dependency instead of failing with a
 * class-loading error.
 */
public final class DefaultAgentStateStoreResolver implements AgentStateStoreResolver {

	static final Map<AgentStateStoreType, String> PROVIDER_MODULES =
			Map.of(AgentStateStoreType.REDIS, "liteflow-react-agent-redis",
					AgentStateStoreType.MYSQL, "liteflow-react-agent-mysql");

	private final Map<AgentStateStoreType, AgentStateStoreProvider> providers;

	public DefaultAgentStateStoreResolver() {
		this(loadProviders());
	}

	DefaultAgentStateStoreResolver(Iterable<AgentStateStoreProvider> providers) {
		Map<AgentStateStoreType, AgentStateStoreProvider> indexed = new EnumMap<>(AgentStateStoreType.class);
		for (AgentStateStoreProvider provider : providers) {
			if (provider == null || provider.type() == null) {
				continue;
			}
			indexed.putIfAbsent(provider.type(), provider);
		}
		this.providers = indexed;
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
			case JSON -> resolveJson(config);
			case REDIS, MYSQL -> resolveProvider(config);
		};
	}

	private static ResolvedAgentStateStore resolveJson(AgentStateStoreConfig config) {
		String jsonRoot = config.getJsonRoot();
		if (jsonRoot == null || jsonRoot.isBlank()) {
			throw new AgentConfigException(
					"liteflow.agent.state-store.json-root is required when type=JSON");
		}
		try {
			return new ResolvedAgentStateStore(
					new JsonFileAgentStateStore(Path.of(jsonRoot)), true);
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"JSON state store could not be created from "
							+ "liteflow.agent.state-store.json-root",
					failure);
		}
	}

	private ResolvedAgentStateStore resolveProvider(AgentStateStoreConfig config) {
		AgentStateStoreProvider provider = providers.get(config.getType());
		if (provider == null) {
			throw new AgentConfigException("state-store type " + config.getType()
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
