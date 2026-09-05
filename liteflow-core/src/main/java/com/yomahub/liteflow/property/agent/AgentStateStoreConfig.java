package com.yomahub.liteflow.property.agent;

/**
 * State store selection and failure behavior for AgentScope 2 execution.
 *
 * <p>Every backend is persistent. JSON keeps state in local files, while REDIS and
 * MYSQL delegate to the matching agentscope extension module and require the
 * corresponding liteflow-agent-{redis,mysql} dependency on the classpath.
 */
public class AgentStateStoreConfig {

	private AgentStateStoreType type = AgentStateStoreType.JSON;
	private String jsonRoot = "./data/agent-state";
	private AgentStateStoreFailurePolicy failurePolicy = AgentStateStoreFailurePolicy.FAIL_FAST;
	private final AgentStateStoreRedisConfig redis = new AgentStateStoreRedisConfig();
	private final AgentStateStoreMysqlConfig mysql = new AgentStateStoreMysqlConfig();

	public AgentStateStoreType getType() {
		return type;
	}

	public void setType(AgentStateStoreType type) {
		this.type = type;
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

	public AgentStateStoreRedisConfig getRedis() {
		return redis;
	}

	public AgentStateStoreMysqlConfig getMysql() {
		return mysql;
	}
}
