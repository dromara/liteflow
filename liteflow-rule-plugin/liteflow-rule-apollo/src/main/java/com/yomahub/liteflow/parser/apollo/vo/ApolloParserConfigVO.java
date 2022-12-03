package com.yomahub.liteflow.parser.apollo.vo;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 13:45
 */
public class ApolloParserConfigVO {

	private String namespace;


	public ApolloParserConfigVO() {
	}

	public ApolloParserConfigVO(String namespace) {
		this.namespace = namespace;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String toString() {
		return "ApolloParserConfigVO{" +
				"namespace='" + namespace + '\'' +
				'}';
	}
}
