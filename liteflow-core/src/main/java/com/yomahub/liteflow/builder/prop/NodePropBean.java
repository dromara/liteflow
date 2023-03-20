package com.yomahub.liteflow.builder.prop;

/**
 * 构建 node 的中间属性
 */
public class NodePropBean {

	/**
	 * id
	 */
	String id;

	/**
	 * 名称
	 */
	String name;

	/**
	 * 类
	 */
	String clazz;

	/**
	 * 脚本
	 */
	String script;

	/**
	 * 类型
	 */
	String type;

	/**
	 * 脚本存放位置
	 */
	String file;

	/**
	 * 脚本语言
	 */
	String language;

	public String getId() {
		return id;
	}

	public NodePropBean setId(String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public NodePropBean setName(String name) {
		this.name = name;
		return this;
	}

	public String getClazz() {
		return clazz;
	}

	public NodePropBean setClazz(String clazz) {
		this.clazz = clazz;
		return this;
	}

	public String getScript() {
		return script;
	}

	public NodePropBean setScript(String script) {
		this.script = script;
		return this;
	}

	public String getType() {
		return type;
	}

	public NodePropBean setType(String type) {
		this.type = type;
		return this;
	}

	public String getFile() {
		return file;
	}

	public NodePropBean setFile(String file) {
		this.file = file;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public NodePropBean setLanguage(String language) {
		this.language = language;
		return this;
	}

}
