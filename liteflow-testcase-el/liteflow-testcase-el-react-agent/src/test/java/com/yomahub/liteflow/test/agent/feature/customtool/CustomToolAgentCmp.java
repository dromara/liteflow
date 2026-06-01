package com.yomahub.liteflow.test.agent.feature.customtool;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
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
public class CustomToolAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
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
