package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import javax.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;

/**
 * 各场景测试公共基类（共享插管之一）。
 *
 * <p>负责注入 {@link FlowExecutor} / {@link LiteflowConfig}，并为旧场景夹具补齐
 * AgentScope 2 执行所需的测试 namespace。AgentScope 2 运行时
 * 由各组件实例持有，并随容器中的组件 bean 一起关闭，不再维护可反射重置的静态会话缓存。
 *
 * <p>所有场景使用进程内确定性模型或本地 mock HTTP 服务，无须真实凭据。
 */
public abstract class BaseAgentTest {

    @Resource
    protected FlowExecutor flowExecutor;

    @Resource
    protected LiteflowConfig liteflowConfig;

    @BeforeEach
    void configureAgentScope2Runtime() {
        if (liteflowConfig.getAgent().getApplicationName() == null
                || liteflowConfig.getAgent().getApplicationName().isBlank()) {
            liteflowConfig.getAgent().setApplicationName("agent-test");
        }
    }

}
