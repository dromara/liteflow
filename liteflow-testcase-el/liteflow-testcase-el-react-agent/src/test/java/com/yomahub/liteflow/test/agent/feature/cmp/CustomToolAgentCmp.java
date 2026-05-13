package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义工具 Agent。验证 {@link #tools()} 注册的工具能出现在 ReActAgent 的 toolkit 中。
 */
@Component("customToolAgent")
public class CustomToolAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    @Override
    protected List<Object> tools() {
        return List.of(new EchoTool());
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }

    public static class EchoTool {
        @Tool(name = "echo_input", description = "Echo back the provided value")
        public String echo(@ToolParam(name = "value", description = "value to echo") String value) {
            return "echo:" + value;
        }
    }
}
