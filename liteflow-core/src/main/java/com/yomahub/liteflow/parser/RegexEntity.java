package com.yomahub.liteflow.parser;

import java.util.Arrays;

public class RegexEntity {

	private String condNode;

	private String[] realNodeArray;

	public String getCondNode() {
		return condNode;
	}

	public void setCondNode(String condNode) {
		this.condNode = condNode;
	}

	public String[] getRealNodeArray() {
		return realNodeArray;
	}

	public void setRealNodeArray(String[] realNodeArray) {
		this.realNodeArray = realNodeArray;
	}

	@Override
	public String toString() {
		return "RegexEntity [condNode=" + condNode + ", realNodeArray="
				+ Arrays.toString(realNodeArray) + "]";
	}
}
