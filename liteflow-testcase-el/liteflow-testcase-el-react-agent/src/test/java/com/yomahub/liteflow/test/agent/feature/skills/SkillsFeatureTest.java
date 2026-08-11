package com.yomahub.liteflow.test.agent.feature.skills;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 覆盖 guide §7 Skills 集成：
 * <ul>
 *   <li>{@code skills.enabled=true} 时 toolkit 应包含 {@code load_skill_through_path}；</li>
 *   <li>组件 {@code skillFilter()} 白名单通过 AgentScope 2 仓库生效。</li>
 * </ul>
 */
@TestPropertySource("classpath:/feature/skills/application.properties")
@SpringBootTest(classes = SkillsFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.skills")
public class SkillsFeatureTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        SkillsAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "SkillsFeatureTest");
        liteflowConfig.getAgent().getSkills().setEnabled(true);
        liteflowConfig.getAgent().getSkills().setPath(resolveSkillsPath());
        liteflowConfig.getAgent().getSkills().setStrict(true);
    }

    private static String resolveSkillsPath() {
        Path moduleRelative = Path.of("src/test/resources/feature/skills/skills");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative.toAbsolutePath().normalize().toString();
        }
        return Path.of("liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/skills/skills")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    @Test
    public void testSkillsEnabledRegistersLoadSkillToolInToolkit() {
        SkillsAgentCmp.allowedSkills = List.of();
        LiteflowResponse response = flowExecutor.execute2Resp("skillsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Set<String> tools = SkillsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(tools.contains("load_skill_through_path"),
                "开启 skills 后 Toolkit 应包含 AgentScope 的 load_skill_through_path 工具");
        Assertions.assertNotNull(SkillsAgentCmp.USED_SKILLS_SNAPSHOT.get(),
                "handleReply 中应能读取当前调用的技能使用记录（即便为空）");
    }

    @Test
    public void testComponentSkillAllowListFiltersToOnlyOneSkill() {
        SkillsAgentCmp.allowedSkills = List.of("demo");
        LiteflowResponse response = flowExecutor.execute2Resp("skillsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Set<String> tools = SkillsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(tools.contains("load_skill_through_path"));
    }

}
