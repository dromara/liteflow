/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

/**
 * 正则实体，主要用于条件节点
 * @author Bryan.Zhang
 */
public class RegexEntity {

	private String item;

	private String[] realItemArray;

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public String[] getRealItemArray() {
		return realItemArray;
	}

	public void setRealItemArray(String[] realItemArray) {
		this.realItemArray = realItemArray;
	}
}
