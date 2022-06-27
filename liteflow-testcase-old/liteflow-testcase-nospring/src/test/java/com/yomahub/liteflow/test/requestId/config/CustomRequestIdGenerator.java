package com.yomahub.liteflow.test.requestId.config;

import com.yomahub.liteflow.flow.id.RequestIdGenerator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tangkc
 */
public class CustomRequestIdGenerator implements RequestIdGenerator {

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    public String generate() {
        return atomicInteger.incrementAndGet() + "";
    }
}
