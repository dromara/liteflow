package com.yomahub.liteflow.test.agent.feature.springbeantool;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 验证 guide §8.1 中组件层工具注入在 Spring 环境下走容器路径：
 * <ul>
 *   <li>{@code tools()} 返回通过 {@code @Resource} 注入的 bean，DI 依赖已生效。</li>
 * </ul>
 */
@TestPropertySource("classpath:/feature/springbeantool/application.properties")
@SpringBootTest(classes = SpringBeanToolInjectionTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.springbeantool")
public class SpringBeanToolInjectionTest extends BaseAgentTest {

    @Resource
    private SpringBeanEchoTool toolBeanFromTestContext;

    @BeforeEach
    public void reset() {
        ComponentToolAgentCmp.reset();
        // 注意：不要 reset SpringBeanEchoTool / GreetingService 的静态计数。
        // 这两个是 Spring 单例，INSTANCE/CONSTRUCT_COUNT 只在构造函数里赋值，
        // 而单例在容器启动时就构造完毕（早于 @BeforeEach）。若在此清零，
        // 单例不会被重新构造，instance() 会一直为 null、constructCount() 一直为 0，
        // 反而把要断言的「容器构造证据」擦掉。
    }

    // ===== 测试1：组件层 tools() 注入 Spring bean =====

    @Test
    public void testComponentToolIsInjectedSpringBean() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "componentToolChain", "请直接用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 1. 工具名出现在 toolkit 中
        Set<String> toolNames = ComponentToolAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(toolNames.contains("spring_bean_echo"),
                "Spring bean 工具应出现在 toolkit：实际=" + toolNames);

        // 2. 传入 toolkit 的实例就是 Spring 容器管理的同一个 bean（身份验证）
        Object capturedTool = ComponentToolAgentCmp.CAPTURED_TOOL_INSTANCE.get();
        Assertions.assertNotNull(capturedTool, "CAPTURED_TOOL_INSTANCE 不应为 null");
        Assertions.assertSame(toolBeanFromTestContext, capturedTool,
                "toolkit 中的工具实例应与测试上下文注入的是同一个 Spring singleton");

        // 3. 工具的 @Resource DI 依赖已生效
        Assertions.assertNotNull(((SpringBeanEchoTool) capturedTool).getGreetingService(),
                "工具的 @Resource GreetingService 应已被 Spring 注入");

        // 4. DI 依赖也是容器管理的同一个 singleton
        Assertions.assertSame(GreetingService.instance(),
                ((SpringBeanEchoTool) capturedTool).getGreetingService(),
                "工具注入的 GreetingService 应是容器管理的 singleton");
    }

}
