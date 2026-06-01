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
 *   <li>组件 {@code skills()} 白名单过滤；</li>
 *   <li>SKILL.md frontmatter 中的 tools 在加载时被构造；</li>
 *   <li>严格模式下声明不存在的技能会快速失败。</li>
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
        SkillEchoTool.reset();
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
                "handleReply 中应能读取 usedSkills() 列表（即便为空）");
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

    @Test
    public void testSkillFrontmatterToolClassIsInstantiatedDuringAgentBuild() {
        SkillsAgentCmp.allowedSkills = List.of("tool-skill");
        LiteflowResponse response = flowExecutor.execute2Resp("skillsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get(),
                "tool-skill 的 frontmatter 中声明的 SkillEchoTool 应在 Agent 构建时被实例化");
    }

    @Test
    public void testMissingComponentSkillFailsChainInStrictMode() {
        SkillsAgentCmp.allowedSkills = List.of("does-not-exist");
        LiteflowResponse response = flowExecutor.execute2Resp("skillsChain", "请用一句话作答。");

        Assertions.assertFalse(response.isSuccess(),
                "严格模式下声明不存在技能应让 chain 直接失败");
        Assertions.assertTrue(response.getMessage().contains("does-not-exist"),
                "失败信息应包含缺失技能名");
    }
}
