package com.yomahub.liteflow.parser.sql.vo;

/**
 * 用于解析 RuleSourceExtData 的 VO 类，用于 sql 模式中
 *
 * @author tangkc
 * @since 2.9.0
 */
public class SQLParserVO {

	/**
	 * 连接地址
	 */
	private String url;

	/**
	 * 驱动
	 */
	private String driverClassName;

	/**
	 * 账号名
	 */
	private String username;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 表名
	 */
	private String tableName = "el_table";

	/**
	 * chainName
	 */
	private String chainNameField = "chain_name";

	/**
	 * el 表达式相关数据
	 */
	private String elDataField = "el_data";

	/**
	 * 脚本 node 表名
	 */
	private String scriptNodeTableName = "script_node_table";

	/**
	 * 脚本 node id 字段
	 */
	private String scriptNodeIdField = "script_node_id";

	/**
	 * 脚本 node name 字段
	 */
	private String scriptNodeNameField = "script_node_name";

	/**
	 * 脚本 node type 字段
	 */
	private String scriptNodeDataField = "script_node_data";

	/**
	 * 脚本 node type 字段
	 */
	private String scriptNodeTypeField = "script_node_type";

	/**
	 * 脚本 node language 字段
	 */
	private String scriptNodeLanguageField = "script_node_language";

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
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

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getChainNameField() {
		return chainNameField;
	}

	public void setChainNameField(String chainNameField) {
		this.chainNameField = chainNameField;
	}

	public String getElDataField() {
		return elDataField;
	}

	public void setElDataField(String elDataField) {
		this.elDataField = elDataField;
	}

	public String getScriptNodeTableName() {
		return scriptNodeTableName;
	}

	public void setScriptNodeTableName(String scriptNodeTableName) {
		this.scriptNodeTableName = scriptNodeTableName;
	}

	public String getScriptNodeIdField() {
		return scriptNodeIdField;
	}

	public void setScriptNodeIdField(String scriptNodeIdField) {
		this.scriptNodeIdField = scriptNodeIdField;
	}

	public String getScriptNodeNameField() {
		return scriptNodeNameField;
	}

	public void setScriptNodeNameField(String scriptNodeNameField) {
		this.scriptNodeNameField = scriptNodeNameField;
	}

	public String getScriptNodeDataField() {
		return scriptNodeDataField;
	}

	public void setScriptNodeDataField(String scriptNodeDataField) {
		this.scriptNodeDataField = scriptNodeDataField;
	}

	public String getScriptNodeTypeField() {
		return scriptNodeTypeField;
	}

	public void setScriptNodeTypeField(String scriptNodeTypeField) {
		this.scriptNodeTypeField = scriptNodeTypeField;
	}

	public String getScriptNodeLanguageField() {
		return scriptNodeLanguageField;
	}

	public void setScriptNodeLanguageField(String scriptNodeLanguageField) {
		this.scriptNodeLanguageField = scriptNodeLanguageField;
	}
}
