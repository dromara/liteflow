package com.yomahub.liteflow.test.agent.feature.skills;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SkillsFeatureTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        SkillsAgentCmp.reset();
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
    @Order(1)
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
        String modelInput = String.join("\n", SkillsAgentCmp.modelInputs());
        Assertions.assertTrue(modelInput.contains("Demo skill for LiteFlow ReAct agent tests"));
        Assertions.assertTrue(modelInput.contains("Research skill for LiteFlow ReAct agent tests"));
    }

    @Test
    @Order(2)
    public void testComponentSkillAllowListFiltersToOnlyOneSkill() {
        AgentSkill demo;
        AgentSkill research;
        try (FileSystemSkillRepository repository =
                     new FileSystemSkillRepository(Path.of(resolveSkillsPath()), false)) {
            demo = repository.getAllSkills().stream()
                    .filter(skill -> "demo".equals(skill.getName()))
                    .findFirst()
                    .orElseThrow();
            research = repository.getAllSkills().stream()
                    .filter(skill -> "research".equals(skill.getName()))
                    .findFirst()
                    .orElseThrow();
            Assertions.assertFalse(repository.skillExists("missing"));
        }
        SkillsAgentCmp.allowedSkills = List.of(demo.getSkillId());
        LiteflowResponse response = flowExecutor.execute2Resp("skillsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Set<String> tools = SkillsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(tools.contains("load_skill_through_path"));
        String modelInput = String.join("\n", SkillsAgentCmp.modelInputs());
        Assertions.assertTrue(modelInput.contains("Demo skill for LiteFlow ReAct agent tests"));
        Assertions.assertFalse(modelInput.contains("Research skill for LiteFlow ReAct agent tests"));

        SkillFilter onlyDemo = SkillFilter.only(demo.getSkillId());
        Assertions.assertTrue(onlyDemo.isAllowed(demo.getSkillId()));
        Assertions.assertFalse(onlyDemo.isAllowed(research.getSkillId()));
        Assertions.assertFalse(onlyDemo.isAllowed("missing"));
    }

}
