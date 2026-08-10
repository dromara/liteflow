package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/** Runtime identity and timeout settings for AgentScope 2 execution. */
public class AgentRuntimeConfig {

	private String namespace;
	private String defaultUserId = "anonymous";
	private Duration timeout = Duration.ofMinutes(2);

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getDefaultUserId() {
		return defaultUserId;
	}

	public void setDefaultUserId(String defaultUserId) {
		this.defaultUserId = defaultUserId;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}
}
