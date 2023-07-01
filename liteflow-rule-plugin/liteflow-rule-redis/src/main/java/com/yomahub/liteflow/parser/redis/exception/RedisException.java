package com.yomahub.liteflow.parser.redis.exception;

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
