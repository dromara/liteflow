package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/** Human-in-the-loop confirmation behavior. */
public class AgentHitlConfig {

	private Duration confirmationTimeout = Duration.ofMinutes(2);
	private boolean failOnDeniedTool;

	public Duration getConfirmationTimeout() {
		return confirmationTimeout;
	}

	public void setConfirmationTimeout(Duration confirmationTimeout) {
		this.confirmationTimeout = confirmationTimeout;
	}

	public boolean isFailOnDeniedTool() {
		return failOnDeniedTool;
	}

	public void setFailOnDeniedTool(boolean failOnDeniedTool) {
		this.failOnDeniedTool = failOnDeniedTool;
	}
}
