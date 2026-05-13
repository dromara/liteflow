package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 maxIterations 为固定值，配合 AgentProbe 在 Pre-Reasoning 中读取
 * ReActAgent.getMaxIters() 来断言组件覆写值已被框架使用。
 */
@Component("maxIterationsAgent")
public class MaxIterationsAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final int OVERRIDDEN_MAX_ITERS = 7;
    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    @Override
    protected int maxIterations() {
        return OVERRIDDEN_MAX_ITERS;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }
}
