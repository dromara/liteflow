package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/** Cross-invocation coordination settings. */
public class AgentInvocationGuardConfig {

	private AgentInvocationGuardMode mode = AgentInvocationGuardMode.LOCAL;
	private String beanName;
	private Duration acquireTimeout = Duration.ofMinutes(2);
	private Duration leaseDuration = Duration.ofMinutes(2);
	private DistributedCoordinationMode coordinationMode = DistributedCoordinationMode.NONE;
	private boolean strictDistributed = true;

	public AgentInvocationGuardMode getMode() {
		return mode;
	}

	public void setMode(AgentInvocationGuardMode mode) {
		this.mode = mode;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public Duration getAcquireTimeout() {
		return acquireTimeout;
	}

	public void setAcquireTimeout(Duration acquireTimeout) {
		this.acquireTimeout = acquireTimeout;
	}

	public Duration getLeaseDuration() {
		return leaseDuration;
	}

	public void setLeaseDuration(Duration leaseDuration) {
		this.leaseDuration = leaseDuration;
	}

	public DistributedCoordinationMode getCoordinationMode() {
		return coordinationMode;
	}

	public void setCoordinationMode(DistributedCoordinationMode coordinationMode) {
		this.coordinationMode = coordinationMode;
	}

	public boolean isStrictDistributed() {
		return strictDistributed;
	}

	public void setStrictDistributed(boolean strictDistributed) {
		this.strictDistributed = strictDistributed;
	}
}
