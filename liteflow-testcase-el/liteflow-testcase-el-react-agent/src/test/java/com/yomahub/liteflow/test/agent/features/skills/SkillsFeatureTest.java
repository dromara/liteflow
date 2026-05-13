package com.yomahub.liteflow.test.agent.features.skills;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 覆盖 guide 中 Skills 开启、组件级技能过滤，以及 usedSkills() 的记录语义。
 *
 * <p>本测试通过本地模型桩返回一次 {@code load_skill_through_path} 工具调用，
 * 让 AgentScope 真实加载 filesystem skill，再由 LiteFlow 的 {@code usedSkills()} 读取结果。
 */
@TestPropertySource(value = "classpath:/agent/features/skills/application.properties")
@SpringBootTest(classes = SkillsFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.skills.cmp" })
public class SkillsFeatureTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ReActAgentFeatureTestSupport.ensureAgentConfig(
                liteflowConfig,
                "target/wk_react_agent_skills",
                true,
                resolveSkillsPath(),
                ShellMode.DISABLED);
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
        CompatibleCustomEchoAgentComponent.resetCompatibleProbe();
        SkillsFeatureProbe.reset();
    }

    private static String resolveSkillsPath() {
        Path moduleRelative = Path.of("src/test/resources/agent/features/skills");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative.toAbsolutePath().normalize().toString();
        }
        return Path.of("liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/features/skills")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    @Test
    public void testUsedSkillsTracksFilesystemSkillLoadedInThenChain() {
        LiteflowResponse response = flowExecutor.execute2Resp("skillsFeatureChain", "load-feature-skill");

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Assertions.assertEquals(List.of("feature-demo"), SkillsFeatureProbe.USED_SKILLS.get(),
                "load_skill_through_path 成功后 usedSkills() 应返回技能名");
        Assertions.assertTrue(SkillsFeatureProbe.TOOL_NAMES.get().contains("load_skill_through_path"),
                "开启 skills 后 Toolkit 应包含 AgentScope 的技能加载工具");
        Assertions.assertEquals(1, CompatibleCustomEchoAgentComponent.COMPATIBLE_SPEC_RESOLVE_COUNT.get());
    }
}
