package com.yomahub.liteflow.parser.redis.vo;

import com.yomahub.liteflow.parser.redis.mode.RedisParserMode;

/**
 * 用于解析RuleSourceExtData的vo类，用于Redis模式中
 *
 * @author hxinyu
 * @since  2.11.0
 */

public class RedisParserVO {

    /*连接地址*/
    private String host;

    /*端口号*/
    private Integer port;

    /*密码*/
    private String password;

    /*监听机制 轮询为poll 订阅为subscribe 默认为poll*/
    private RedisParserMode mode = RedisParserMode.POLL;

    /*轮询时间间隔(s) 默认1分钟 若选择订阅机制可不配置*/
    private Integer pollingInterval = 60;

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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RedisParserMode getMode() {
        return mode;
    }

    public void setMode(String mode) {
        mode = mode.toUpperCase();
        try{
            RedisParserMode m = RedisParserMode.valueOf(mode);
            this.mode = m;
        }
        catch (Exception ignored) {
            //枚举类转换出错默认为轮询方式
        }
    }

    public Integer getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(Integer pollingInterval) {
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
