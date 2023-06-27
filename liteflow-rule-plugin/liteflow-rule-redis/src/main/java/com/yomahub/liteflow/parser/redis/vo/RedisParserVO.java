package com.yomahub.liteflow.parser.redis.vo;

public class RedisParserVO {

    /*连接地址*/
    private String url;

    /*端口号*/
    private String port;

    /*账号名*/
    private String username;

    /*密码*/
    private String password;

    /*是否采用轮询机制 默认为轮询 否则选择pub/sub机制*/
    private boolean isPolling = true;

    /*轮询时间间隔(ms) 默认1分组 若选择pub/sub机制可不配置*/
    private String pollingInterval = "60000";

    /*chain表配置的数据库号*/
    private String chainDataBase;

    /*chain配置的键名*/
    private String chainKey;

    /*脚本表配置的数据库号*/
    private String scriptDataBase;

    /*脚本配置的键名*/
    private String scriptKey;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
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

    public boolean isPolling() {
        return isPolling;
    }

    public void setPolling(boolean polling) {
        isPolling = polling;
    }

    public String getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public String getChainDataBase() {
        return chainDataBase;
    }

    public void setChainDataBase(String chainDataBase) {
        this.chainDataBase = chainDataBase;
    }

    public String getChainKey() {
        return chainKey;
    }

    public void setChainKey(String chainKey) {
        this.chainKey = chainKey;
    }

    public String getScriptDataBase() {
        return scriptDataBase;
    }

    public void setScriptDataBase(String scriptDataBase) {
        this.scriptDataBase = scriptDataBase;
    }

    public String getScriptKey() {
        return scriptKey;
    }

    public void setScriptKey(String scriptKey) {
        this.scriptKey = scriptKey;
    }
}
