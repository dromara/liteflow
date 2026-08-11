package com.yomahub.liteflow.property.agent;

/**
 * 1.x MySQL memory 配置的绑定兼容对象。
 *
 * <p>AgentScope 2 运行时不再创建数据库 session。请由应用提供实现
 * {@code AgentStateStore} 的 Bean，并配置
 * {@code liteflow.agent.state-store.type=BEAN} 与
 * {@code liteflow.agent.state-store.bean-name}。LiteFlow 不创建 JDBC 连接池。
 */
public class MysqlMemoryConfig {

	private boolean explicitlyConfigured;

    /**
     * 用于查找 {@link javax.sql.DataSource} 的 Bean 名称（必填）。
     *
     * <p>仅供旧配置迁移；2.0 运行时使用 {@code state-store.bean-name}。
     */
    private String dataSourceBeanName;

    /**
     * 传入 {@code MysqlSession} 的数据库名。
     *
     * <p>留空表示使用 AgentScope 的默认值 {@code agentscope}；
     * 与 {@link #tableName} 至少有一项非空时，会走带自定义库表名的构造重载。
     */
    private String databaseName;

    /**
     * 传入 {@code MysqlSession} 的表名。
     *
     * <p>留空表示使用 AgentScope 的默认值 {@code agentscope_sessions}。
     */
    private String tableName;

    /**
     * 是否允许 AgentScope 自动建库建表。
     *
     * <p>默认为 false，避免在生产环境因权限不足或库表已被运维管控而出错；
     * 当确实需要自动初始化（如本地开发、单测）时可显式打开。
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
