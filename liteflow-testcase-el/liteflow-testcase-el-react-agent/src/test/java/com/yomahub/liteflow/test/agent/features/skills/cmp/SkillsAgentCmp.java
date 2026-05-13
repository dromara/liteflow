package com.yomahub.liteflow.test.agent.features.skills.cmp;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.features.skills.SkillsFeatureProbe;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用 compatible-custom 配置解析，同时只允许加载 feature-demo 一个技能。
 */
@Component("skillsAgent")
public class SkillsAgentCmp extends CompatibleCustomEchoAgentComponent {
    @Override
    protected List<String> skills() {
        return List.of("feature-demo");
    }

    @Override
    protected ModelSpec<?> model() {
        return OpenAICompatible.custom(
                ReActAgentFeatureTestSupport.COMPATIBLE_CONFIG_KEY,
                "compatible-custom-skills-test-model");
    }

    @Override
    protected Model buildModel() {
        model().resolve(agentConfig());
        COMPATIBLE_SPEC_RESOLVE_COUNT.incrementAndGet();
        return new SkillsLoadingModel();
    }

    @Override
    protected void handleReply(Msg reply) {
        SkillsFeatureProbe.USED_SKILLS.set(usedSkills());
        super.handleReply(reply);
    }
}
