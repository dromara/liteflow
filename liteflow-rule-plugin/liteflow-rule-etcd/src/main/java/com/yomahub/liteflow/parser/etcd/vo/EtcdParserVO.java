package com.yomahub.liteflow.parser.etcd.vo;

/**
 * 用于解析RuleSourceExtData的vo类，用于etcd模式中
 *
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserVO {

	private String endpoints;

	private String user;

	private String password;

	private String namespace;

	private String chainPath;

	private String scriptPath;

	public String getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(String endpoints) {
		this.endpoints = endpoints;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getChainPath() {
		return chainPath;
	}

	public void setChainPath(String chainPath) {
		this.chainPath = chainPath;
	}

	public String getScriptPath() {
		return scriptPath;
	}

	public void setScriptPath(String scriptPath) {
		this.scriptPath = scriptPath;
	}

}
