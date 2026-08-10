package com.yomahub.liteflow.property.agent;

/** State store selection and failure behavior for AgentScope 2 execution. */
public class AgentStateStoreConfig {

	private AgentStateStoreType type = AgentStateStoreType.MEMORY;
	private String beanName;
	private String jsonRoot = "./data/agent-state";
	private AgentStateStoreFailurePolicy failurePolicy = AgentStateStoreFailurePolicy.FAIL_FAST;

	public AgentStateStoreType getType() {
		return type;
	}

	public void setType(AgentStateStoreType type) {
		this.type = type;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getJsonRoot() {
		return jsonRoot;
	}

	public void setJsonRoot(String jsonRoot) {
		this.jsonRoot = jsonRoot;
	}

	public AgentStateStoreFailurePolicy getFailurePolicy() {
		return failurePolicy;
	}

	public void setFailurePolicy(AgentStateStoreFailurePolicy failurePolicy) {
		this.failurePolicy = failurePolicy;
	}
}
