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
	 * 应用名
	 */
	private String applicationName;

	/**
	 * chain表名
	 */
	private String chainTableName;

	/**
	 * chain表里的应用名字段
	 */
	private String chainApplicationNameField = "application_name";

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
	private String scriptTableName;

	/**
	 * script表里的应用名字段
	 */
	private String scriptApplicationNameField = "application_name";

	/**
	 * 脚本 node id 字段
	 */
	private String scriptIdField = "script_id";

	/**
	 * 脚本 node name 字段
	 */
	private String scriptNameField = "script_name";

	/**
	 * 脚本 node data 字段
	 */
	private String scriptDataField = "script_data";

	/**
	 * 脚本 node type 字段
	 */
	private String scriptTypeField = "script_type";

	/**
	 * 脚本 node language 字段
	 */
	private String scriptLanguageField = "script_language";

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

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getChainTableName() {
		return chainTableName;
	}

	public void setChainTableName(String chainTableName) {
		this.chainTableName = chainTableName;
	}

	public String getChainApplicationNameField() {
		return chainApplicationNameField;
	}

	public void setChainApplicationNameField(String chainApplicationNameField) {
		this.chainApplicationNameField = chainApplicationNameField;
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

	public String getScriptTableName() {
		return scriptTableName;
	}

	public void setScriptTableName(String scriptTableName) {
		this.scriptTableName = scriptTableName;
	}

	public String getScriptApplicationNameField() {
		return scriptApplicationNameField;
	}

	public void setScriptApplicationNameField(String scriptApplicationNameField) {
		this.scriptApplicationNameField = scriptApplicationNameField;
	}

	public String getScriptIdField() {
		return scriptIdField;
	}

	public void setScriptIdField(String scriptIdField) {
		this.scriptIdField = scriptIdField;
	}

	public String getScriptNameField() {
		return scriptNameField;
	}

	public void setScriptNameField(String scriptNameField) {
		this.scriptNameField = scriptNameField;
	}

	public String getScriptDataField() {
		return scriptDataField;
	}

	public void setScriptDataField(String scriptDataField) {
		this.scriptDataField = scriptDataField;
	}

	public String getScriptTypeField() {
		return scriptTypeField;
	}

	public void setScriptTypeField(String scriptTypeField) {
		this.scriptTypeField = scriptTypeField;
	}

	public String getScriptLanguageField() {
		return scriptLanguageField;
	}

	public void setScriptLanguageField(String scriptLanguageField) {
		this.scriptLanguageField = scriptLanguageField;
	}
}
