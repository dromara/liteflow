package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * ReAct Agent 测试的公共 SpringBoot 基类。
 *
 * <p>所有测试类都继承这个类，保证它们使用同一份 LiteFlow EL 规则、
 * 同一套测试组件扫描路径，以及同一套 agent 基础配置。这样每个测试文件
 * 可以只关注自己的主题，例如平台连通性、Session、Workspace 或工具注册。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = AbstractReActAgentSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.cmp")
public abstract class AbstractReActAgentSpringbootTest {

    @Resource
    protected FlowExecutor flowExecutor;

    @Resource
    protected LiteflowConfig liteflowConfig;

    /**
     * 每个测试方法执行前都清理测试桩和 ReActAgentComponent 内部的单例
     * SessionManager，避免前一个测试残留的 agent 实例、内存或 workspace
     * 影响当前场景的断言。
     */
    @BeforeEach
    public void resetAgentState() throws Exception {
        ensureAgentConfig();
        StubReActAgentCmp.reset();
        resetAgentSessionManager();
    }

    /**
     * 某些单模块测试场景下，SpringBoot 属性绑定可能没有把 liteflow.agent
     * 段落装配到 LiteflowConfig。这里显式补齐测试所需的最小 agent 配置，
     * 让测试聚焦在 ReAct Agent 行为本身，而不是被配置缺失提前中断。
     */
    protected void ensureAgentConfig() {
        if (liteflowConfig.getAgent() == null) {
            liteflowConfig.setAgent(new AgentConfig());
        }
        AgentConfig agentConfig = liteflowConfig.getAgent();
        if (agentConfig.getWorkspace().getRoot() == null || agentConfig.getWorkspace().getRoot().isBlank()) {
            agentConfig.getWorkspace().setRoot("target/wk_root");
        }
        agentConfig.getShell().setMode(ShellMode.BLACKLIST);
        agentConfig.getDefaults().setMaxIterations(20);
        agentConfig.getLogging().setReactEnabled(true);
    }

    /**
     * ReActAgentComponent 为了复用 SessionManager 使用了内部静态 Holder。
     * 测试需要通过反射调用包内测试方法进行重置，以保证每个测试从干净状态开始。
     */
    private void resetAgentSessionManager() throws Exception {
        Class<?> holder = Class.forName("com.yomahub.liteflow.agent.component.ReActAgentComponent$AgentSessionManagerHolder");
        Method reset = holder.getDeclaredMethod("resetForTesting");
        reset.setAccessible(true);
        reset.invoke(null);
    }
}
