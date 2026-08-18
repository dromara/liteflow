package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;

/**
 * SPI for state-store backends delivered by companion modules.
 *
 * <p>Providers are discovered through {@link java.util.ServiceLoader}; the companion
 * module registers its implementation in
 * {@code META-INF/services/com.yomahub.liteflow.agent.state.AgentStateStoreProvider}.
 * This keeps optional backend dependencies (Redis, MySQL, ...) out of
 * liteflow-react-agent-core while {@link DefaultAgentStateStoreResolver} stays the
 * single selection point for {@code liteflow.agent.state-store.*}.
 */
public interface AgentStateStoreProvider {

	/** The {@link AgentStateStoreType} this provider builds. */
	AgentStateStoreType type();

	/**
	 * Builds the resolved store for {@code liteflow.agent.state-store.type == type()}.
	 *
	 * @param config the full state-store config; nested sections relevant to the
	 *               provider's type are guaranteed non-null
	 * @return the store plus ownership flag; never {@code null}
	 */
	ResolvedAgentStateStore resolve(AgentStateStoreConfig config);
}
