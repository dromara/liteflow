package com.yomahub.liteflow.parser.redis.mode;

/**
 * 用于定义Redis规则存储和监听方式的枚举类
 *
 * poll轮询拉取模式, sub监听模式
 * @author hxinyu
 * @since  2.11.0
 */
public enum RedisParserMode {

    //poll为轮询模式，subscribe/sub为订阅模式，默认为poll
    POLL("poll"),
    SUB("subscribe"),
    SUBSCRIBE("subscribe");

    private String mode;

    RedisParserMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
