package com.yomahub.liteflow.parser.zk.vo;

/**
 * 用于解析RuleSourceExtData的vo类，用于zk模式中
 *
 * @author Bryan.Zhang
 * @since 2.8.6
 */
public class ZkParserVO {

	private String connectStr;

	private String chainPath;

	private String scriptPath;

	public String getConnectStr() {
		return connectStr;
	}

	public void setConnectStr(String connectStr) {
		this.connectStr = connectStr;
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
