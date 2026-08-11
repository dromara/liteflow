package com.yomahub.liteflow.test.agent.feature.skills;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import com.yomahub.liteflow.test.agent.support.ForwardingProbeMiddleware;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 验证 AgentScope 2 技能仓库与组件级 {@link SkillFilter}。
 */
@Component("skillsAgent")
public class SkillsAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static final AtomicReference<List<String>> USED_SKILLS_SNAPSHOT = new AtomicReference<>();
    private static final List<String> MODEL_INPUTS = new CopyOnWriteArrayList<>();
    private static final MiddlewareBase FORWARDING_PROBE = new ForwardingProbeMiddleware(() -> {
        AgentProbe probe = PROBE.get();
        return probe == null ? null : probe.middleware();
    });
    public static volatile List<String> allowedSkills = List.of();

    public static void reset() {
        PROBE.set(new AgentProbe());
        USED_SKILLS_SNAPSHOT.set(null);
        MODEL_INPUTS.clear();
        allowedSkills = List.of();
    }

    public static List<String> modelInputs() {
        return List.copyOf(MODEL_INPUTS);
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("deterministic buildModel override must bypass model spec");
    }

    @Override
    protected Model buildModel() {
        return new DeterministicHistoryModel(
                "skills-model", new CopyOnWriteArrayList<>(), MODEL_INPUTS);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
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
    protected List<AgentSkillRepository> skillRepositories() {
        if (!agentConfig().getSkills().isEnabled()) {
            return List.of();
        }
        return List.of(new FileSystemSkillRepository(
                Path.of(agentConfig().getSkills().getPath()), false));
    }

    @Override
    protected boolean ownsSkillRepository(AgentSkillRepository repository) {
        return true;
    }

    @Override
    protected SkillFilter skillFilter() {
        return allowedSkills.isEmpty()
                ? SkillFilter.all()
                : SkillFilter.only(allowedSkills.toArray(String[]::new));
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        return List.of(FORWARDING_PROBE);
    }

    @Override
    protected void handleReply(Msg reply, com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        USED_SKILLS_SNAPSHOT.set(context.getUsedSkills());
        super.handleReply(reply, context);
    }
}
