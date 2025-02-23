package com.yomahub.liteflow.parser.sql.vo;

import cn.hutool.core.util.StrUtil;

/**
 * 用于解析 RuleSourceExtData 的 VO 类，用于 sql 模式中
 *
 * @author tangkc
 * @author Bryan.Zhang
 * @author jay li
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
     * instanceId 表名
     */
    private String instanceIdTableName = "node_instance_id_table";

    /**
     * instanceId 里的应用名字段
     */
    private String instanceIdApplicationNameField = "application_name";
    /**
     * instanceId 里的 chainId 字段
     */
    private String instanceChainIdField = "chain_id";

    /**
     * el md5
     */
    private String elDataMd5Field = "el_data_md5";

   /**
    * group_key_instance_id
    */
    private String nodeInstanceIdMapJsonField = "node_instance_id_map_json";

    /**
     * 决策路由字段
     */
    private String routeField;

    /**
     * 命名空间字段
     */
    private String namespaceField;

    /**
     * 是否启动某一条chain
     */
    private String chainEnableField;

    /**
     * chain 自定义 sql
     */
    private String chainCustomSql;

    /**
     * 脚本 自定义 sql
     */
    private String scriptCustomSql;

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
    private String scriptLanguageField;

    /**
     * 是否启动这一条脚本
     */
    private String scriptEnableField;

    /**
     * 轮询机制是否开启 默认不开启
     */
    private Boolean pollingEnabled = false;

    /**
     * 轮询时间间隔(s) 默认60s
     */
    private Integer pollingIntervalSeconds = 60;

    /**
     * 规则配置后首次轮询的起始时间 默认为60s
     */
    private Integer pollingStartSeconds = 60;

    /**
     * 是否开启sql日志
     */
    private Boolean sqlLogEnabled = true;

    /**
     * 苞米豆动态数据源配置
     */
    private String baomidouDataSource;

    /**
     * sharding jdbc 动态数据源配置
     */
    private String shardingJdbcDataSource;

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

    /**
     * 判断是否自动查找数据源
     */
    public boolean isAutoFoundDataSource() {
        return StrUtil.isBlank(url) &&
                StrUtil.isBlank(username) &&
                StrUtil.isBlank(password) &&
                StrUtil.isBlank(driverClassName);
    }

    /**
     * 判断是否使用jdbc连接
     */
    public boolean isUseJdbcConn(){
        return StrUtil.isNotBlank(url) &&
                StrUtil.isNotBlank(username) &&
                StrUtil.isNotBlank(password) &&
                StrUtil.isNotBlank(driverClassName);
    }

    public Boolean getPollingEnabled() {
        return pollingEnabled;
    }

    public void setPollingEnabled(Boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public Integer getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public Integer getPollingStartSeconds() {
        return pollingStartSeconds;
    }

    public void setPollingStartSeconds(Integer pollingStartSeconds) {
        this.pollingStartSeconds = pollingStartSeconds;
    }

    public Boolean getSqlLogEnabled() {
        return sqlLogEnabled;
    }

    public void setSqlLogEnabled(Boolean sqlLogEnabled) {
        this.sqlLogEnabled = sqlLogEnabled;
    }

    public String getChainEnableField() {
        return chainEnableField;
    }

    public void setChainEnableField(String chainEnableField) {
        this.chainEnableField = chainEnableField;
    }

    public String getScriptEnableField() {
        return scriptEnableField;
    }

    public void setScriptEnableField(String scriptEnableField) {
        this.scriptEnableField = scriptEnableField;
    }

    /**
     * 是否包含启停字段（chain 和 script）
     */
    public boolean hasEnableField() {
        return StrUtil.isNotBlank(chainEnableField) || StrUtil.isNotBlank(scriptEnableField);
    }

    public String getRouteField() {
        return routeField;
    }

    public void setRouteField(String routeField) {
        this.routeField = routeField;
    }

    public String getNamespaceField() {
        return namespaceField;
    }

    public void setNamespaceField(String namespaceField) {
        this.namespaceField = namespaceField;
    }

    public String getChainCustomSql() {
        return chainCustomSql;
    }

    public void setChainCustomSql(String chainCustomSql) {
        this.chainCustomSql = chainCustomSql;
    }

    public String getScriptCustomSql() {
        return scriptCustomSql;
    }

    public void setScriptCustomSql(String scriptCustomSql) {
        this.scriptCustomSql = scriptCustomSql;
    }

    public String getInstanceIdApplicationNameField() {
        return instanceIdApplicationNameField;
    }

    public void setInstanceIdApplicationNameField(String instanceIdApplicationNameField) {
        this.instanceIdApplicationNameField = instanceIdApplicationNameField;
    }

    public String getInstanceIdTableName() {
        return instanceIdTableName;
    }

    public String getInstanceChainIdField() {
        return instanceChainIdField;
    }

    public void setInstanceChainIdField(String instanceChainIdField) {
        this.instanceChainIdField = instanceChainIdField;
    }

    public void setInstanceIdTableName(String instanceIdTableName) {
        this.instanceIdTableName = instanceIdTableName;
    }

    public String getElDataMd5Field() {
        return elDataMd5Field;
    }

    public void setElDataMd5Field(String elDataMd5Field) {
        this.elDataMd5Field = elDataMd5Field;
    }

    public String getNodeInstanceIdMapJsonField() {
        return nodeInstanceIdMapJsonField;
    }

    public void setNodeInstanceIdMapJsonField(String nodeInstanceIdMapJsonField) {
        this.nodeInstanceIdMapJsonField = nodeInstanceIdMapJsonField;
    }

    public String getBaomidouDataSource() {
        return baomidouDataSource;
    }

    public void setBaomidouDataSource(String baomidouDataSource) {
        this.baomidouDataSource = baomidouDataSource;
    }

    public String getShardingJdbcDataSource() {
        return shardingJdbcDataSource;
    }

    public void setShardingJdbcDataSource(String shardingJdbcDataSource) {
        this.shardingJdbcDataSource = shardingJdbcDataSource;
    }
}
