package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.jupiter.api.BeforeEach;

import javax.annotation.Resource;

/**
 * 各场景测试公共基类（共享插管之一）。
 *
 * <p>只负责两件事：注入 {@link FlowExecutor} / {@link LiteflowConfig}，
 * 以及在每个 @Test 前重置 ReActAgentComponent 缓存的 SessionManager，
 * 避免 JVM 内全局静态状态在不同测试类之间互相污染。
 *
 * <p>agent 运行配置（workspace / shell / iterations / skills 等）由每个 package
 * 自己的 application.properties 声明；凭据由各测试在 @BeforeEach 中调用
 * {@link LiveTestSupport} 的 applyXxxOrSkip 装入（缺失即 skip）。
 */
public abstract class BaseAgentLiveTest {

    @Resource
    protected FlowExecutor flowExecutor;

    @Resource
    protected LiteflowConfig liteflowConfig;

    @BeforeEach
    public void resetAgentRuntime() throws Exception {
        LiveTestSupport.resetAgentSessionManager();
    }
}
