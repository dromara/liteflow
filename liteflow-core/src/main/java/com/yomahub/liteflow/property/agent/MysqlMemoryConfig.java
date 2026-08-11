package com.yomahub.liteflow.property.agent;

/**
 * 1.x MySQL memory 配置的绑定兼容对象，仅用于旧配置迁移诊断。
 *
 * <p>setter 只记录用户显式绑定了旧键，使 {@link AgentConfig#validateForExecution()}
 * 在运行前要求迁移；AgentScope 2 runtime 不读取这些字段。数据库状态存储应由应用提供
 * {@code AgentStateStore} Bean，并使用 {@code state-store.type=BEAN}。
 */
public class MysqlMemoryConfig {

	private boolean explicitlyConfigured;

    /**
     * 旧 DataSource Bean 名，仅保留用于配置绑定和迁移诊断。
     */
    private String dataSourceBeanName;

    /**
     * 旧数据库名，仅保留用于配置绑定和迁移诊断；2.0 runtime 不读取。
     */
    private String databaseName;

    /**
     * 旧表名，仅保留用于配置绑定和迁移诊断；2.0 runtime 不读取。
     */
    private String tableName;

    /**
     * 旧自动建库建表开关，仅保留用于配置绑定和迁移诊断；2.0 runtime 不读取。
     */
    private boolean createIfNotExist = false;

    public String getDataSourceBeanName() {
        return dataSourceBeanName;
    }

    public void setDataSourceBeanName(String dataSourceBeanName) {
		this.explicitlyConfigured = true;
        this.dataSourceBeanName = dataSourceBeanName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
		this.explicitlyConfigured = true;
        this.databaseName = databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
		this.explicitlyConfigured = true;
        this.tableName = tableName;
    }

    public boolean isCreateIfNotExist() {
        return createIfNotExist;
    }

    public void setCreateIfNotExist(boolean createIfNotExist) {
		this.explicitlyConfigured = true;
        this.createIfNotExist = createIfNotExist;
    }

	boolean isExplicitlyConfigured() {
		return explicitlyConfigured;
	}
}
