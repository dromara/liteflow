package com.yomahub.liteflow.property.agent;

/**
 * Session store selection and failure behavior for AgentScope 2 execution.
 *
 * <p>Every backend is persistent. JSON keeps state in local files, while REDIS and
 * MYSQL delegate to the matching agentscope extension module and require the
 * corresponding liteflow-agent-{redis,mysql} dependency on the classpath.
 */
public class AgentSessionStoreConfig {

	private AgentSessionStoreType type = AgentSessionStoreType.JSON;
	private String jsonRoot = "./data/agent-state";
	private String jsonWorkspaceRoot;

	/** Optional directory for JSON-backed workspace records, separate from execution roots. */
	public String getJsonWorkspaceRoot() { return jsonWorkspaceRoot; }
	public void setJsonWorkspaceRoot(String root) { jsonWorkspaceRoot = root; }
	private AgentSessionStoreFailurePolicy failurePolicy = AgentSessionStoreFailurePolicy.FAIL_FAST;
	private final AgentSessionStoreRedisConfig redis = new AgentSessionStoreRedisConfig();
	private final AgentSessionStoreMysqlConfig mysql = new AgentSessionStoreMysqlConfig();

	public AgentSessionStoreType getType() {
		return type;
	}

	public void setType(AgentSessionStoreType type) {
		this.type = type;
	}

	public String getJsonRoot() {
		return jsonRoot;
	}

	public void setJsonRoot(String jsonRoot) {
		this.jsonRoot = jsonRoot;
	}

	public AgentSessionStoreFailurePolicy getFailurePolicy() {
		return failurePolicy;
	}

	public void setFailurePolicy(AgentSessionStoreFailurePolicy failurePolicy) {
		this.failurePolicy = failurePolicy;
	}

	public AgentSessionStoreRedisConfig getRedis() {
		return redis;
	}

	public AgentSessionStoreMysqlConfig getMysql() {
		return mysql;
	}
}
