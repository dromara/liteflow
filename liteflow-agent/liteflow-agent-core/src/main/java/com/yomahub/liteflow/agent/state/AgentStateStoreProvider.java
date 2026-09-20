package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;

/**
 * SPI for session-store backends delivered by companion modules.
 *
 * <p>Providers are discovered through {@link java.util.ServiceLoader}; the companion
 * module registers its implementation in
 * {@code META-INF/services/com.yomahub.liteflow.agent.state.AgentStateStoreProvider}.
 * This keeps optional backend dependencies (Redis, MySQL, ...) out of
 * liteflow-agent-core while {@link DefaultAgentStateStoreResolver} stays the
 * single selection point for {@code liteflow.agent.session-store.*}.
 */
public interface AgentStateStoreProvider {

	/** The {@link AgentSessionStoreType} this provider builds. */
	AgentSessionStoreType type();

	/**
	 * Builds the resolved store for {@code liteflow.agent.session-store.type == type()}.
	 *
	 * @param config the full session-store config; nested sections relevant to the
	 *               provider's type are guaranteed non-null
	 * @return the store plus ownership flag; never {@code null}
	 */
	ResolvedAgentStateStore resolve(AgentSessionStoreConfig config);
}
