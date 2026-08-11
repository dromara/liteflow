package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import javax.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * 各场景测试公共基类（共享插管之一）。
 *
 * <p>负责注入 {@link FlowExecutor} / {@link LiteflowConfig}，并为旧场景夹具补齐
 * AgentScope 2 执行所需的测试 namespace。AgentScope 2 运行时
 * 由各组件实例持有，并随容器中的组件 bean 一起关闭，不再维护可反射重置的静态会话缓存。
 *
 * <p>agent 运行配置（workspace / shell / iterations / skills 等）由每个 package
 * 自己的 application.properties 声明；凭据由各测试在 @BeforeEach 中调用
 * {@link LiveTestSupport} 的 applyXxxOrSkip 装入（缺失即 skip）。
 */
@Tag("agentscope-live-smoke")
public abstract class BaseAgentLiveTest {

    @Resource
    protected FlowExecutor flowExecutor;

    @Resource
    protected LiteflowConfig liteflowConfig;

    @BeforeEach
    void configureAgentScope2Runtime() {
        if (liteflowConfig.getAgent().getRuntime().getNamespace() == null
                || liteflowConfig.getAgent().getRuntime().getNamespace().isBlank()) {
            liteflowConfig.getAgent().getRuntime().setNamespace("react-agent-test");
        }
    }

}
