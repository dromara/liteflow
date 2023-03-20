package com.yomahub.liteflow.parser.apollo.vo;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloParserConfigVO {

	private String chainNamespace;

	private String scriptNamespace;

	public ApolloParserConfigVO() {
	}

	public ApolloParserConfigVO(String chainNamespace, String scriptNamespace) {
		this.chainNamespace = chainNamespace;
		this.scriptNamespace = scriptNamespace;
	}

	public String getChainNamespace() {
		return chainNamespace;
	}

	public void setChainNamespace(String chainNamespace) {
		this.chainNamespace = chainNamespace;
	}

	public String getScriptNamespace() {
		return scriptNamespace;
	}

	public void setScriptNamespace(String scriptNamespace) {
		this.scriptNamespace = scriptNamespace;
	}

	@Override
	public String toString() {
		return "ApolloParserConfigVO{" + "chainNamespace='" + chainNamespace + '\'' + ", scriptNamespace='"
				+ scriptNamespace + '\'' + '}';
	}

}
