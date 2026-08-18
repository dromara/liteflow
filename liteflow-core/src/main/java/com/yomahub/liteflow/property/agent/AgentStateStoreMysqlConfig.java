package com.yomahub.liteflow.property.agent;

/**
 * MySQL state-store settings, bound from {@code liteflow.agent.state-store.mysql.*}.
 *
 * <p>Exactly one connection source must be configured: either
 * {@code data-source-bean-name} (the application's pooled {@code DataSource}, the
 * recommended production setup) or {@code jdbc-url} plus credentials (LiteFlow
 * builds a driver {@code DataSource} and owns it).
 */
public class AgentStateStoreMysqlConfig {

	/** Name of a container-managed {@code javax.sql.DataSource} bean. */
	private String dataSourceBeanName;

	/** JDBC url such as {@code jdbc:mysql://localhost:3306}; when set LiteFlow owns the DataSource. */
	private String jdbcUrl;

	/** JDBC user used together with {@code jdbc-url}. */
	private String username;

	/** JDBC password used together with {@code jdbc-url}. */
	private String password;

	/** Database holding the state table; defaults to the agentscope extension default. */
	private String databaseName;

	/** State table name; defaults to the agentscope extension default. */
	private String tableName;

	/** Whether the database and state table may be created automatically when missing. */
	private boolean createIfNotExist = false;

	public String getDataSourceBeanName() {
		return dataSourceBeanName;
	}

	public void setDataSourceBeanName(String dataSourceBeanName) {
		this.dataSourceBeanName = dataSourceBeanName;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
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

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public boolean isCreateIfNotExist() {
		return createIfNotExist;
	}

	public void setCreateIfNotExist(boolean createIfNotExist) {
		this.createIfNotExist = createIfNotExist;
	}
}
