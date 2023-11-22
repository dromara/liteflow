package com.yomahub.liteflow.parser.nacos.vo;

/**
 * 用于解析 RuleSourceExtData 的vo类，用于nacos模式中
 *
 * @author mll
 * @since 2.9.0
 */
public class NacosParserVO {

	private String serverAddr;

	private String namespace;

	private String dataId;

	private String group;

	private String accessKey;

	private String secretKey;

	private String username;

	private String password;

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	@Override
	public String toString() {
		return "NacosParserVO{" +
				"serverAddr='" + serverAddr + '\'' +
				", namespace='" + namespace + '\'' +
				", dataId='" + dataId + '\'' +
				", group='" + group + '\'' +
				", accessKey='" + accessKey + '\'' +
				", secretKey='" + secretKey + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				'}';
	}
}
