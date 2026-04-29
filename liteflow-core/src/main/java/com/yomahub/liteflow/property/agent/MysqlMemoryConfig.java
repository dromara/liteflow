package com.yomahub.liteflow.property.agent;

/**
 * Settings that only apply when {@link MemoryStorageMode#MYSQL} is selected.
 *
 * <p>The DataSource is supplied by the user via {@code beanName} and looked up
 * through ContextAware. LiteFlow never builds a JDBC connection pool itself.
 */
public class MysqlMemoryConfig {
    /** Bean name of the {@link javax.sql.DataSource} to look up via ContextAware. */
    private String dataSourceBeanName;

    /** Database name passed to {@code MysqlSession}. Empty means use AgentScope's default ({@code agentscope}). */
    private String databaseName;

    /** Table name passed to {@code MysqlSession}. Empty means use AgentScope's default ({@code agentscope_sessions}). */
    private String tableName;

    /** When true, AgentScope auto-creates database & table; defaults to false to respect production constraints. */
    private boolean createIfNotExist = false;

    public String getDataSourceBeanName() { return dataSourceBeanName; }
    public void setDataSourceBeanName(String dataSourceBeanName) { this.dataSourceBeanName = dataSourceBeanName; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public boolean isCreateIfNotExist() { return createIfNotExist; }
    public void setCreateIfNotExist(boolean createIfNotExist) { this.createIfNotExist = createIfNotExist; }
}
