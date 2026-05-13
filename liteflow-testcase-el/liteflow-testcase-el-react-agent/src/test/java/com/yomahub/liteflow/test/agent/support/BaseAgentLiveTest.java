package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import org.junit.jupiter.api.BeforeEach;

import javax.annotation.Resource;

/**
 * 真实 apikey 功能测试公共基类。
 *
 * <p>每个具体测试类自己用 {@code @SpringBootTest} 注解切入需要的组件包，
 * 同时继承本类即可获得：
 * <ul>
 *   <li>FlowExecutor 注入；</li>
 *   <li>每个 @Test 前重置 ReActAgentComponent 缓存的 SessionManager；</li>
 *   <li>提供 workspaceRoot 与 shellMode 等可覆写参数，便于子类按需调整。</li>
 * </ul>
 */
public abstract class BaseAgentLiveTest {

    @Resource
    protected FlowExecutor flowExecutor;

    @Resource
    protected LiteflowConfig liteflowConfig;

    /** 子类可覆写以指定专属的 workspace 根，避免不同测试类彼此读写同一目录。 */
    protected String workspaceRoot() {
        return "target/wk_react_agent_" + getClass().getSimpleName();
    }

    /** 默认 Shell 关闭，最大限度避免任何意外触发 shell 工具。 */
    protected ShellMode defaultShellMode() {
        return ShellMode.DISABLED;
    }

    @BeforeEach
    public void resetAgentRuntime() throws Exception {
        LiveTestSupport.ensureMinimalAgentConfig(liteflowConfig, workspaceRoot(), defaultShellMode());
        LiveTestSupport.resetAgentSessionManager();
    }
}
