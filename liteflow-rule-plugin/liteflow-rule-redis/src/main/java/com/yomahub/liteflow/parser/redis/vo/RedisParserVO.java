package com.yomahub.liteflow.parser.redis.vo;

/**
 * 用于解析RuleSourceExtData的vo类，用于Redis模式中
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserVO {

    /*连接地址*/
    private String host;

    /*端口号*/
    private String port;

    /*密码*/
    private String password;

    /*监听机制 轮询为poll 订阅为subscribe 默认为poll*/
    private String mode = "poll";

    /*轮询时间间隔(ms) 默认1分钟 若选择订阅机制可不配置*/
    private String pollingInterval = "60000";

    /*chain表配置的数据库号*/
    private Integer chainDataBase;

    /*chain配置的键名*/
    private String chainKey;

    /*脚本表配置的数据库号 若没有脚本数据可不配置*/
    private Integer scriptDataBase;

    /*脚本配置的键名 若没有脚本数据可不配置*/
    private String scriptKey;

    public String getHost() {
        return host;
    }

    public void setHost(String url) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public Integer getChainDataBase() {
        return chainDataBase;
    }

    public void setChainDataBase(Integer chainDataBase) {
        this.chainDataBase = chainDataBase;
    }

    public String getChainKey() {
        return chainKey;
    }

    public void setChainKey(String chainKey) {
        this.chainKey = chainKey;
    }

    public Integer getScriptDataBase() {
        return scriptDataBase;
    }

    public void setScriptDataBase(Integer scriptDataBase) {
        this.scriptDataBase = scriptDataBase;
    }

    public String getScriptKey() {
        return scriptKey;
    }

    public void setScriptKey(String scriptKey) {
        this.scriptKey = scriptKey;
    }
}
