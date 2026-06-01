package com.yomahub.liteflow.test.agent.feature.springbeantool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 同时标注 {@code @Component} 和 {@code @Tool} 的工具类，
 * 用于验证：
 * <ul>
 *   <li>组件层 {@code tools()} 通过 {@code @Resource} 注入此 bean 后注册到 Toolkit；</li>
 *   <li>Skill 层 {@code SkillToolResolver} 从容器按类型取到此 bean（而非反射降级实例化）。</li>
 * </ul>
 *
 * <p>通过静态 {@code INSTANCE} 和 {@code CONSTRUCT_COUNT} 暴露内部状态供测试断言。
 */
@Component
public class SpringBeanEchoTool {

    private static final AtomicReference<SpringBeanEchoTool> INSTANCE = new AtomicReference<>();
    private static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

    @Resource
    private GreetingService greetingService;

    public SpringBeanEchoTool() {
        CONSTRUCT_COUNT.incrementAndGet();
        INSTANCE.compareAndSet(null, this);
    }

    @Tool(name = "spring_bean_echo", description = "Echo using Spring-injected service")
    public String echo(@ToolParam(name = "text", description = "text to echo") String text) {
        return greetingService.greet(text);
    }

    public GreetingService getGreetingService() {
        return greetingService;
    }

    public static SpringBeanEchoTool instance() {
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
