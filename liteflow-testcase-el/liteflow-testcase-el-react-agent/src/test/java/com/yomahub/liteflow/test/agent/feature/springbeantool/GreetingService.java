package com.yomahub.liteflow.test.agent.feature.springbeantool;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 被工具类 {@link SpringBeanEchoTool} 通过 {@code @Resource} 注入的服务 bean。
 * 通过静态字段暴露容器管理的实例，供测试做身份验证和构造计数。
 */
@Component
public class GreetingService {

    private static final AtomicReference<GreetingService> INSTANCE = new AtomicReference<>();
    private static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

    public GreetingService() {
        CONSTRUCT_COUNT.incrementAndGet();
        INSTANCE.compareAndSet(null, this);
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static GreetingService instance() {
        return INSTANCE.get();
    }

    public static int constructCount() {
        return CONSTRUCT_COUNT.get();
    }

    public static void reset() {
        INSTANCE.set(null);
        CONSTRUCT_COUNT.set(0);
    }
}
