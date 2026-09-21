package com.yomahub.liteflow.property.agent;

/** Event-listener failure behavior. */
public class AgentEventConfig {

	private AgentListenerFailureMode listenerFailureMode = AgentListenerFailureMode.FAIL_FAST;

	public AgentListenerFailureMode getListenerFailureMode() {
		return listenerFailureMode;
	}

	public void setListenerFailureMode(AgentListenerFailureMode listenerFailureMode) {
		this.listenerFailureMode = listenerFailureMode;
	}
}
