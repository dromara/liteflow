package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.skill.SkillBoxFactory;
import com.yomahub.liteflow.agent.skill.SkillLoadResult;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.test.agent.tool.SkillEchoTool;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SkillBoxFactoryTest {

    private AgentConfig cfg;

    @BeforeEach
    public void setUp() {
        cfg = new AgentConfig();
        cfg.getSkills().setEnabled(true);
        Path moduleRelative = Path.of("src/test/resources/agent/skills");
        Path rootRelative = Path.of("liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills");
        cfg.getSkills().setPath(Files.isRegularFile(moduleRelative.resolve("demo/SKILL.md"))
                ? moduleRelative.toString()
                : rootRelative.toString());
        cfg.getSkills().setStrict(true);
        SkillEchoTool.reset();
    }

    @Test
    public void testEmptyAllowListLoadsAllSkills() {
        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of());
        Set<String> expectedNames = Set.of("demo", "research", "tool-skill");

        Set<String> names = result.skillBox().getAllSkillIds().stream()
                .map(id -> result.skillBox().getSkill(id))
                .map(AgentSkill::getName)
                .collect(Collectors.toSet());

        Assertions.assertEquals(expectedNames, names);
        Assertions.assertEquals(expectedNames, Set.copyOf(result.skillNames()));
    }

    @Test
    public void testAllowListFiltersSkills() {
        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("demo"));

        List<String> names = result.skillBox().getAllSkillIds().stream()
                .map(id -> result.skillBox().getSkill(id))
                .map(AgentSkill::getName)
                .toList();

        Assertions.assertEquals(List.of("demo"), names);
        Assertions.assertEquals(List.of("demo"), result.skillNames());
    }

    @Test
    public void testMissingAllowListedSkillFailsInStrictMode() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> SkillBoxFactory.build(new Toolkit(), cfg, List.of("missing-skill")));

        Assertions.assertTrue(ex.getMessage().contains("missing-skill"));
    }

    @Test
    public void testFrontmatterToolClassIsInstantiated() {
        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("tool-skill"));

        Assertions.assertEquals(List.of("tool-skill"), result.skillNames());
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
    }

    @Test
    public void testSkillBoxUsesConversationWorkspaceForCodeExecution() throws Exception {
        Path workspace = Files.createTempDirectory("liteflow-skill-workspace-test-");

        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("demo"), workspace);

        Assertions.assertEquals(workspace.toAbsolutePath().normalize(), result.skillBox().getCodeExecutionWorkDir());
        Assertions.assertEquals(workspace.toAbsolutePath().normalize().resolve("skills"), result.skillBox().getUploadDir());
    }

    @Test
    public void testMissingSkillsDirectoryFailsInStrictMode() {
        cfg.getSkills().setPath(Path.of("target", "missing-skills-dir").toString());

        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> SkillBoxFactory.build(new Toolkit(), cfg, List.of()));

        Assertions.assertTrue(ex.getMessage().contains("Skills root not found"));
    }
}
