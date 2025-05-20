package com.yomahub.liteflow.parser.redis.vo;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.yomahub.liteflow.parser.redis.mode.RedisMode;
import com.yomahub.liteflow.parser.redis.mode.RedisParserMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用于解析RuleSourceExtData的vo类, 用于Redis模式中
 *
 * @author hxinyu
 * @since  2.11.0
 */

public class RedisParserVO {

    /*Redis配置模式 单点/哨兵, 默认为单点模式*/
    private RedisMode redisMode = RedisMode.SINGLE;

    /*单点模式 连接地址*/
    private String host;

    /*单点模式 端口号*/
    private Integer port;

    /*哨兵模式 主节点名*/
    private String masterName;

    /*哨兵模式 哨兵节点连接地址 ip:port, 可配置多个*/
    private List<String> sentinelAddress;

    /*用户名 需要Redis 6.0及以上*/
    private String username;

    /*密码*/
    private String password;

    private int connectionMinimumIdleSize = 2;

    private int connectionPoolSize = 4;

    /*监听机制 轮询为poll 订阅为subscribe 默认为poll*/
    private RedisParserMode mode = RedisParserMode.POLL;

    /*轮询时间间隔(s) 默认60s 若选择订阅机制可不配置*/
    private Integer pollingInterval = 60;

    /*规则配置后首次轮询的起始时间 默认为60s 若选择订阅机制可不配置*/
    private Integer pollingStartTime = 60;

    /*chain表配置的数据库号*/
    private Integer chainDataBase;

    /*chain配置的键名*/
    private String chainKey;

    /*脚本表配置的数据库号 若没有脚本数据可不配置*/
    private Integer scriptDataBase;

    /*脚本配置的键名 若没有脚本数据可不配置*/
    private String scriptKey;

    public void setRedisMode(String redisMode) {
        redisMode = redisMode.toUpperCase();
        try{
            RedisMode m = RedisMode.valueOf(redisMode);
            this.redisMode = m;
        }
        catch (Exception ignored) {
            //转换出错默认为单点模式
        }
    }

    public RedisMode getRedisMode() {
        return redisMode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public List<String> getSentinelAddress() {
        return sentinelAddress;
    }

    public void setSentinelAddress(List<String> sentinelAddress) {
        this.sentinelAddress = sentinelAddress;
    }

    @JsonSetter("sentinelAddress")
    public void setSentinelAddressFromString(String addresses) {
        if (addresses != null && !addresses.trim().isEmpty()) {
            // 按逗号分割，并去除每个地址前后的空格
            this.sentinelAddress = Arrays.asList(addresses.split("\\s*,\\s*"));
        } else {
            this.sentinelAddress = Collections.emptyList();
        }
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

    public Integer getPollingStartTime() {
        return pollingStartTime;
    }

    public void setPollingStartTime(Integer pollingStartTime) {
        this.pollingStartTime = pollingStartTime;
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

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getConnectionMinimumIdleSize() {
        return connectionMinimumIdleSize;
    }

    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
    }

    @Override
    public String toString() {
        return "RedisParserVO{" +
                "redisMode=" + redisMode +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", masterName=" + masterName +
                ", sentinelAddress=" + sentinelAddress +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", mode=" + mode +
                ", pollingInterval=" + pollingInterval +
                ", pollingStartTime=" + pollingStartTime +
                ", chainDataBase=" + chainDataBase +
                ", chainKey='" + chainKey + '\'' +
                ", scriptDataBase=" + scriptDataBase +
                ", scriptKey='" + scriptKey + '\'' +
                '}';
    }
}
