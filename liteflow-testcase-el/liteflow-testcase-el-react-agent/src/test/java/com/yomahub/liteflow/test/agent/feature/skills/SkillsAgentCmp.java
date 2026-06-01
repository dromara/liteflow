package com.yomahub.liteflow.test.agent.feature.skills;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 验证 skills.enabled=true 时 {@code load_skill_through_path} 工具会被注册到 Agent。
 * 是否真的 load 取决于模型是否选择调用工具；本测试只关注工具集与组件级 skills() 过滤。
 */
@Component("skillsAgent")
public class SkillsAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static final AtomicReference<List<String>> USED_SKILLS_SNAPSHOT = new AtomicReference<>();
    public static volatile List<String> allowedSkills = List.of();

    public static void reset() {
        PROBE.set(new AgentProbe());
        USED_SKILLS_SNAPSHOT.set(null);
        allowedSkills = List.of();
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
    protected List<String> skills() {
        return allowedSkills;
    }

    @Override
    protected boolean enableSkills() {
        return true;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }

    @Override
    protected void handleReply(Msg reply) {
        USED_SKILLS_SNAPSHOT.set(usedSkills());
        super.handleReply(reply);
    }
}
