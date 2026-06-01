package com.yomahub.liteflow.test.agent.feature.springbeantool;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通过 {@code @Resource} 注入 {@link SpringBeanEchoTool}（Spring bean），
 * 并在 {@link #tools()} 中返回该 bean 实例，验证组件层工具注册走容器路径。
 */
@Component("componentToolAgent")
public class ComponentToolAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static final AtomicReference<Object> CAPTURED_TOOL_INSTANCE = new AtomicReference<>();

    @Resource
    private SpringBeanEchoTool toolBean;

    public static void reset() {
        PROBE.set(new AgentProbe());
        CAPTURED_TOOL_INSTANCE.set(null);
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
        Object tool = toolBean;
        CAPTURED_TOOL_INSTANCE.set(tool);
        return List.of(tool);
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }
}
