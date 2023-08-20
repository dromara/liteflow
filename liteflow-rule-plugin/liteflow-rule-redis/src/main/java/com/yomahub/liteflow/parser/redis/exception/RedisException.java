package com.yomahub.liteflow.parser.redis.exception;

/**
 * Redis解析异常
 *
 * @author hxinyu
 * @since  2.11.0
 */

public class RedisException extends RuntimeException{

    private String message;

    public RedisException(String message) {
        super();
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
