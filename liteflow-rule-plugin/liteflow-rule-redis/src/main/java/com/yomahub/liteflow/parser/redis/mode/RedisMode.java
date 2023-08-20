package com.yomahub.liteflow.parser.redis.mode;

/**
 * 用于定义Redis模式的枚举类
 *
 * single单点模式, sentinel哨兵模式
 * 不支持集群模式配置
 *
 * @author hxinyu
 * @since  2.11.0
 */
public enum RedisMode {

    SINGLE("single"),

    SENTINEL("sentinel");

    private String mode;

    RedisMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
