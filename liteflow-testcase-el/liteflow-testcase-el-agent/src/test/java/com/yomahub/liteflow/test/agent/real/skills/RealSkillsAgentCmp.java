package com.yomahub.liteflow.test.agent.real.skills;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.runtime.SkillRepositoryRegistration;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.message.Msg;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * guide §11 Skills 的真实模型组件：技能仓库 + 过滤 + usedSkills 记录。
 */
final class RealSkillsAgentCmp {

    private RealSkillsAgentCmp() {
    }

    static final List<String> USED_SKILLS = new CopyOnWriteArrayList<>();

    static void reset() {
        USED_SKILLS.clear();
    }

    @Component("realSkillsAgent")
    static class SkillsAgentCmp extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected List<SkillRepositoryRegistration> skillRepositoryRegistrations() {
            return List.of(SkillRepositoryRegistration.owned(
                    new FileSystemSkillRepository(
                            Path.of("src/test/resources/real/skills/skills"), false)));
        }

        @Override
        protected SkillFilter skillFilter() {
            return SkillFilter.only("secret-code");
        }

        @Override
        protected PermissionContextState permissionContext() {
            return PermissionContextState.builder()
                    .addAllowRule("load_skill_through_path", new PermissionRule(
                            "load_skill_through_path", null,
                            PermissionBehavior.ALLOW, "real skill test"))
                    .build();
        }

        @Override
        protected String systemPrompt() {
            return "你是技能测试助手。有可用技能时优先加载并严格遵循技能内容回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected int maxIterations() {
            return 8;
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            USED_SKILLS.addAll(context.getUsedSkills());
            super.handleReply(reply, context);
        }
    }
}
