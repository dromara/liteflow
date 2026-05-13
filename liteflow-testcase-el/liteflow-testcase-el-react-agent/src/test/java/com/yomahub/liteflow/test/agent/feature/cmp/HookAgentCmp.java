package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简单的 Hook 注册 Agent：用 AgentProbe.hook() 抓取 reasoning 事件，
 * 用以断言组件覆写的 {@code hooks()} 被注册并真实生效。
 */
@Component("hookAgent")
public class HookAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }
}
