package com.yomahub.liteflow.property.agent;

/**
 * 仅在 {@link MemoryStorageMode#MYSQL} 模式下生效的配置项，对应配置段
 * {@code liteflow.agent.session.memory.mysql.*}。
 *
 * <p>{@link javax.sql.DataSource} 由用户在框架容器中预先注册，
 * 通过 {@link #dataSourceBeanName} 借助 ContextAware 查找；
 * LiteFlow 自身不会创建任何 JDBC 连接池。
 */
public class MysqlMemoryConfig {

    /**
     * 用于查找 {@link javax.sql.DataSource} 的 Bean 名称（必填）。
     *
     * <p>{@code MysqlAgentSessionFactory} 启动时通过 ContextAware 拿到该 bean，
     * 若为空或类型不匹配会抛出 {@code AgentConfigException}。
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
        this.dataSourceBeanName = dataSourceBeanName;
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
