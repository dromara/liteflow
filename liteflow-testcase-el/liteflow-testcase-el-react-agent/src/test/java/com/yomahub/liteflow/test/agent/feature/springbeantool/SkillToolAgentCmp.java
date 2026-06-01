package com.yomahub.liteflow.test.agent.feature.springbeantool;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 skills 并指定 {@code bean-tool-skill}，不覆写 {@link #tools()}，
 * 让 {@code SkillToolResolver} 从 Spring 容器中解析 {@link SpringBeanEchoTool}。
 */
@Component("skillToolAgent")
public class SkillToolAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static volatile List<String> allowedSkills = List.of("bean-tool-skill");

    public static void reset() {
        PROBE.set(new AgentProbe());
        allowedSkills = List.of("bean-tool-skill");
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
    protected boolean enableSkills() {
        return true;
    }

    @Override
    protected List<String> skills() {
        return allowedSkills;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }
}
