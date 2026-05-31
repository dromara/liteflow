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

import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 测试用的受控容器：可注册指定类型的 bean，可模拟容器访问异常。
     * 继承 LocalContextAware 以省去实现 ContextAware 全部方法的样板。
     */
    private static final class StubContextAware extends LocalContextAware {
        private final Map<Class<?>, Object> beans = new HashMap<>();
        private final boolean throwOnAccess;

        StubContextAware() {
            this(false);
        }

        StubContextAware(boolean throwOnAccess) {
            this.throwOnAccess = throwOnAccess;
        }

        void register(Class<?> type, Object bean) {
            beans.put(type, bean);
        }

        @Override
        public boolean hasBean(Class<?> clazz) {
            if (throwOnAccess) {
                throw new IllegalStateException("container not ready");
            }
            return beans.containsKey(clazz);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getBean(Class<T> clazz) {
            return (T) beans.get(clazz);
        }
    }

    /** 反射写入 ContextAwareHolder 的静态缓存，绕过 ServiceLoader，使测试可控且隔离。 */
    private static void installContextAware(ContextAware contextAware) throws Exception {
        Field field = ContextAwareHolder.class.getDeclaredField("contextAware");
        field.setAccessible(true);
        field.set(null, contextAware);
    }

    @Test
    public void testRegisteredToolBeanIsReusedFromContainer() throws Exception {
        SkillEchoTool prebuilt = new SkillEchoTool(); // 模拟容器中已注册的单例，CONSTRUCT_COUNT -> 1
        StubContextAware stub = new StubContextAware();
        stub.register(SkillEchoTool.class, prebuilt);
        installContextAware(stub);
        try {
            SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("tool-skill"));

            Assertions.assertEquals(List.of("tool-skill"), result.skillNames());
            // 复用容器 bean，build 不应再反射构造新实例
            Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
        } finally {
            ContextAwareHolder.clean();
        }
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

    @Test
    public void testInlineArrayToolsAreInstantiated() throws Exception {
        Path skillsRoot = Files.createTempDirectory("liteflow-inline-tools-");
        writeSkill(skillsRoot, "inline",
                "name: inline",
                "description: Skill declaring tools with YAML inline-array syntax",
                "tools: [" + SkillEchoTool.class.getName() + "]");
        cfg.getSkills().setPath(skillsRoot.toString());

        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of());

        Assertions.assertEquals(List.of("inline"), result.skillNames());
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
    }

    @Test
    public void testBrokenSiblingSkillOutsideAllowListDoesNotFailBuild() throws Exception {
        Path skillsRoot = Files.createTempDirectory("liteflow-sibling-skills-");
        writeSkill(skillsRoot, "good",
                "name: good",
                "description: Allow-listed skill with a valid tool",
                "tools: " + SkillEchoTool.class.getName());
        writeSkill(skillsRoot, "broken",
                "name: broken",
                "description: Sibling skill referencing a missing tool class",
                "tools: com.yomahub.liteflow.test.agent.tool.NoSuchTool");
        cfg.getSkills().setPath(skillsRoot.toString());

        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("good"));

        Assertions.assertEquals(List.of("good"), result.skillNames());
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
    }

    private static void writeSkill(Path skillsRoot, String dirName, String... frontmatterLines) throws Exception {
        Path skillDir = Files.createDirectories(skillsRoot.resolve(dirName));
        StringBuilder sb = new StringBuilder("---\n");
        for (String line : frontmatterLines) {
            sb.append(line).append('\n');
        }
        sb.append("---\n\n# ").append(dirName).append("\n");
        Files.writeString(skillDir.resolve("SKILL.md"), sb.toString());
    }
}
