# liteflow-react-agent Skills Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configuration-driven agent-scope skills support to `liteflow-react-agent`, with optional component-level skill allow-lists and `SKILL.md` Java tool binding.

**Architecture:** Add a `SkillsConfig` under `AgentConfig`, create a focused `com.yomahub.liteflow.agent.skill` package that builds `SkillBox` from `FileSystemSkillRepository`, and integrate the result into `ReActAgentComponent#buildAgent()`. Session reuse remains keyed by `(conversationId, agentKey)`; skill sets are treated as stable component capability declarations.

**Tech Stack:** Java 17, Maven, JUnit 5, Spring Boot tests, agent-scope `SkillBox`, `AgentSkill`, `FileSystemSkillRepository`, LiteFlow `ReActAgentComponent`.

---

## File Structure

- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
  - Add `SkillsConfig skills` property and accessors.
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SkillsConfig.java`
  - Holds `enabled`, `path`, and `strict`.
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillLoadResult.java`
  - Immutable build result for `SkillBox`, selected names, and skill id mapping.
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolManifest.java`
  - Scans `SKILL.md` frontmatter and instantiates skill-bound Java tools.
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillBoxFactory.java`
  - Loads repository skills, filters by component allow-list, validates, and registers skills/tools.
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillTrackingHook.java`
  - Tracks `load_skill_through_path` calls and exposes used skill names.
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java`
  - Store optional `SkillTrackingHook` for a cached ReAct agent.
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
  - Add `skills()`, `enableSkills()`, `usedSkills()`, and integrate `SkillBox` into agent build.
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/AbstractReActAgentSpringbootTest.java`
  - Ensure tests default to skills disabled unless a test enables them.
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`
  - Add skill allow-list and used-skills probes for tests.
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/tool/SkillEchoTool.java`
  - Test skill-bound Java tool.
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillFactoryTest.java`
  - Unit tests for factory behavior.
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillIntegrationTest.java`
  - Integration tests for `ReActAgentComponent` skill integration.
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillTrackingHookTest.java`
  - Hook-level test for skill usage tracking.
- Create test skill fixtures:
  - `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/demo/SKILL.md`
  - `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/research/SKILL.md`
  - `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/tool-skill/SKILL.md`

---

### Task 1: Add skills configuration model

**Files:**
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SkillsConfig.java`
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
- Test: compile through downstream tests in later tasks

- [ ] **Step 1: Write the failing compile target in the implementation notes**

Run this command before implementation to verify current code has no `SkillsConfig` type:

```bash
mvn -pl liteflow-react-agent/liteflow-react-agent-core -DskipTests compile
```

Expected: PASS before this task. After the next test references `AgentConfig#getSkills()`, compilation will fail until this task is implemented.

- [ ] **Step 2: Create `SkillsConfig`**

Create `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SkillsConfig.java` with this exact content:

```java
package com.yomahub.liteflow.property.agent;

/**
 * Agent skills configuration, mapped from {@code liteflow.agent.skills.*}.
 *
 * <p>Skills are disabled by default so existing ReAct agent users do not need
 * to create a skills directory after upgrading.
 */
public class SkillsConfig {

    /** Whether agent-scope skills integration is enabled. */
    private boolean enabled = false;

    /** Filesystem root that contains skill directories with SKILL.md files. */
    private String path = "./skills";

    /** Whether missing skills or invalid tool classes should fail fast. */
    private boolean strict = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
```

- [ ] **Step 3: Add `skills` to `AgentConfig`**

Modify `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`:

Add this field after `LoggingConfig logging`:

```java
    /** Skills configuration for agent-scope SkillBox integration. */
    private SkillsConfig skills = new SkillsConfig();
```

Add these accessors near the other accessors:

```java
    public SkillsConfig getSkills() {
        return skills;
    }

    public void setSkills(SkillsConfig skills) {
        this.skills = skills;
    }
```

- [ ] **Step 4: Run compile**

```bash
mvn -pl liteflow-core -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SkillsConfig.java \
        liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java
git commit -m "feat(agent): add skills configuration"
```

---

### Task 2: Add skill factory unit tests and fixtures

**Files:**
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/demo/SKILL.md`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/research/SKILL.md`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/tool-skill/SKILL.md`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/tool/SkillEchoTool.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillFactoryTest.java`

- [ ] **Step 1: Add test skill fixtures**

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/demo/SKILL.md`:

```markdown
---
name: demo
description: Demo skill for LiteFlow ReAct agent tests
---

# Demo Skill

Use this skill when the request is about a simple demonstration.
```

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/research/SKILL.md`:

```markdown
---
name: research
description: Research skill for LiteFlow ReAct agent tests
---

# Research Skill

Use this skill when the request requires research planning.
```

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/tool-skill/SKILL.md`:

```markdown
---
name: tool-skill
description: Skill that binds a Java tool for LiteFlow ReAct agent tests
tools:
  - com.yomahub.liteflow.test.agent.tool.SkillEchoTool
---

# Tool Skill

Use this skill when a Java tool should be available after loading the skill.
```

- [ ] **Step 2: Add skill-bound test tool**

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/tool/SkillEchoTool.java`:

```java
package com.yomahub.liteflow.test.agent.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.concurrent.atomic.AtomicInteger;

public class SkillEchoTool {

    public static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

    public SkillEchoTool() {
        CONSTRUCT_COUNT.incrementAndGet();
    }

    public static void reset() {
        CONSTRUCT_COUNT.set(0);
    }

    @Tool(name = "skill_echo", description = "Echo text from a skill-bound Java tool")
    public String echo(@ToolParam(name = "text", description = "Text to echo") String text) {
        return text;
    }
}
```

- [ ] **Step 3: Write failing factory tests**

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillFactoryTest.java`:

```java
package com.yomahub.liteflow.test.agent;

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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReActAgentSkillFactoryTest {

    private AgentConfig cfg;

    @BeforeEach
    public void setUp() {
        cfg = new AgentConfig();
        cfg.getSkills().setEnabled(true);
        cfg.getSkills().setPath("src/test/resources/agent/skills");
        cfg.getSkills().setStrict(true);
        SkillEchoTool.reset();
    }

    @Test
    public void testEmptyAllowListLoadsAllSkills() {
        SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of());

        Set<String> names = result.skillBox().getAllSkillIds().stream()
                .map(id -> result.skillBox().getSkill(id))
                .map(AgentSkill::getName)
                .collect(Collectors.toSet());

        Assertions.assertTrue(names.contains("demo"));
        Assertions.assertTrue(names.contains("research"));
        Assertions.assertTrue(names.contains("tool-skill"));
        Assertions.assertTrue(result.skillNames().containsAll(List.of("demo", "research", "tool-skill")));
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
    public void testMissingSkillsDirectoryFailsInStrictMode() {
        cfg.getSkills().setPath(Path.of("target", "missing-skills-dir").toString());

        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> SkillBoxFactory.build(new Toolkit(), cfg, List.of()));

        Assertions.assertTrue(ex.getMessage().contains("Skills root not found"));
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillFactoryTest test
```

Expected: FAIL with compilation errors for missing `SkillBoxFactory` and `SkillLoadResult`.

- [ ] **Step 5: Commit failing tests**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/tool/SkillEchoTool.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillFactoryTest.java
git commit -m "test(agent): cover skill box factory behavior"
```

---

### Task 3: Implement skill loading package

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillLoadResult.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolManifest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillBoxFactory.java`

- [ ] **Step 1: Create `SkillLoadResult`**

Create `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillLoadResult.java`:

```java
package com.yomahub.liteflow.agent.skill;

import io.agentscope.core.skill.SkillBox;

import java.util.List;
import java.util.Map;

public record SkillLoadResult(
        SkillBox skillBox,
        Map<String, String> skillIdToName,
        List<String> skillNames) {
}
```

- [ ] **Step 2: Create `SkillToolManifest`**

Create `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolManifest.java`:

```java
package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.SkillsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SkillToolManifest {

    private static final Logger LOG = LoggerFactory.getLogger(SkillToolManifest.class);

    private final SkillsConfig config;
    private final Map<String, List<Class<?>>> toolClasses = new LinkedHashMap<>();

    public SkillToolManifest(Path skillsRoot, SkillsConfig config) {
        this.config = config;
        scan(skillsRoot);
    }

    public List<Object> instantiateTools(String skillName) {
        List<Class<?>> classes = toolClasses.get(skillName);
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        List<Object> instances = new ArrayList<>(classes.size());
        for (Class<?> clazz : classes) {
            try {
                instances.add(clazz.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                handleProblem("Skill '" + skillName + "' tool class '" + clazz.getName()
                        + "' instantiation failed", e);
            }
        }
        return instances;
    }

    private void scan(Path skillsRoot) {
        if (!Files.isDirectory(skillsRoot)) {
            handleProblem("Skills root not found: " + skillsRoot, null);
            return;
        }
        try (Stream<Path> dirs = Files.list(skillsRoot)) {
            dirs.filter(Files::isDirectory).forEach(this::loadOne);
        } catch (IOException e) {
            handleProblem("Failed to scan skills dir: " + skillsRoot, e);
        }
    }

    private void loadOne(Path skillDir) {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            return;
        }
        try {
            Map<String, Object> frontmatter = parseFrontmatter(Files.readString(skillMd));
            Object nameObj = frontmatter.get("name");
            if (nameObj == null) {
                return;
            }
            Object toolsObj = frontmatter.get("tools");
            if (toolsObj == null) {
                return;
            }
            String skillName = nameObj.toString().trim();
            List<Class<?>> resolved = new ArrayList<>();
            for (String className : toClassNameList(toolsObj)) {
                try {
                    resolved.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    handleProblem("Skill '" + skillName + "' references unknown tool class '"
                            + className + "'", e);
                }
            }
            if (!resolved.isEmpty()) {
                toolClasses.put(skillName, List.copyOf(resolved));
                LOG.info("Skill '{}' bound to tool classes: {}", skillName,
                        resolved.stream().map(Class::getName).toList());
            }
        } catch (IOException e) {
            handleProblem("Failed to read skill file: " + skillMd, e);
        }
    }

    private Map<String, Object> parseFrontmatter(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (content == null || !content.startsWith("---")) {
            return result;
        }
        int end = content.indexOf("\n---", 3);
        if (end < 0) {
            return result;
        }
        String[] lines = content.substring(3, end).split("\R");
        String currentListKey = null;
        List<String> currentList = null;
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (currentListKey != null && trimmed.startsWith("-")) {
                currentList.add(unquote(trimmed.substring(1).trim()));
                continue;
            }
            currentListKey = null;
            currentList = null;
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (value.isEmpty()) {
                currentListKey = key;
                currentList = new ArrayList<>();
                result.put(key, currentList);
            } else {
                result.put(key, unquote(value));
            }
        }
        return result;
    }

    private List<String> toClassNameList(Object field) {
        if (field instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Stream.of(field.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private void handleProblem(String message, Exception e) {
        if (config.isStrict()) {
            if (e == null) {
                throw new AgentConfigException(message);
            }
            throw new AgentConfigException(message, e);
        }
        if (e == null) {
            LOG.warn(message);
        } else {
            LOG.warn("{}: {}", message, e.getMessage());
        }
    }
}
```

- [ ] **Step 3: Create `SkillBoxFactory`**

Create `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillBoxFactory.java`:

```java
package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.SkillsConfig;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillBoxFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SkillBoxFactory.class);

    private SkillBoxFactory() {
    }

    public static SkillLoadResult build(Toolkit toolkit, AgentConfig agentConfig, List<String> allowedSkills) {
        SkillsConfig skillsConfig = agentConfig.getSkills();
        Path root = Path.of(skillsConfig.getPath()).normalize();
        if (!Files.isDirectory(root)) {
            return handleMissingRoot(root, skillsConfig, toolkit);
        }

        try {
            FileSystemSkillRepository repository = new FileSystemSkillRepository(root);
            List<AgentSkill> allSkills = repository.getAllSkills();
            List<AgentSkill> selected = selectSkills(allSkills, allowedSkills, skillsConfig);
            SkillToolManifest manifest = new SkillToolManifest(root, skillsConfig);
            SkillBox skillBox = new SkillBox(toolkit);
            Map<String, String> skillIdToName = new LinkedHashMap<>();
            List<String> skillNames = new ArrayList<>();

            for (AgentSkill skill : selected) {
                skillIdToName.put(skill.getSkillId(), skill.getName());
                skillNames.add(skill.getName());
                List<Object> skillTools = manifest.instantiateTools(skill.getName());
                if (skillTools.isEmpty()) {
                    skillBox.registerSkill(skill);
                } else {
                    for (Object tool : skillTools) {
                        skillBox.registration().skill(skill).tool(tool).apply();
                    }
                }
            }
            return new SkillLoadResult(skillBox, Map.copyOf(skillIdToName), List.copyOf(skillNames));
        } catch (AgentConfigException e) {
            throw e;
        } catch (Exception e) {
            if (skillsConfig.isStrict()) {
                throw new AgentConfigException("Failed to load skills from: " + root, e);
            }
            LOG.warn("Failed to load skills from {}: {}", root, e.getMessage());
            return new SkillLoadResult(new SkillBox(toolkit), Map.of(), List.of());
        }
    }

    private static SkillLoadResult handleMissingRoot(Path root, SkillsConfig skillsConfig, Toolkit toolkit) {
        String message = "Skills root not found: " + root;
        if (skillsConfig.isStrict()) {
            throw new AgentConfigException(message);
        }
        LOG.warn(message);
        return new SkillLoadResult(new SkillBox(toolkit), Map.of(), List.of());
    }

    private static List<AgentSkill> selectSkills(
            List<AgentSkill> allSkills,
            List<String> allowedSkills,
            SkillsConfig skillsConfig) {
        Map<String, AgentSkill> byName = allSkills.stream()
                .collect(Collectors.toMap(
                        AgentSkill::getName,
                        skill -> skill,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Set<String> allowed = normalizeAllowedSkills(allowedSkills);
        if (allowed.isEmpty()) {
            return new ArrayList<>(byName.values());
        }

        List<String> missing = allowed.stream()
                .filter(name -> !byName.containsKey(name))
                .toList();
        if (!missing.isEmpty()) {
            String message = "Declared skills not found: " + missing;
            if (skillsConfig.isStrict()) {
                throw new AgentConfigException(message);
            }
            LOG.warn(message);
        }

        List<AgentSkill> selected = new ArrayList<>();
        for (String name : allowed) {
            AgentSkill skill = byName.get(name);
            if (skill != null) {
                selected.add(skill);
            }
        }
        return selected;
    }

    private static Set<String> normalizeAllowedSkills(List<String> allowedSkills) {
        if (allowedSkills == null || allowedSkills.isEmpty()) {
            return Set.of();
        }
        return allowedSkills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
```

- [ ] **Step 4: Run factory tests**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillFactoryTest test
```

Expected: PASS.

- [ ] **Step 5: Commit implementation**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillLoadResult.java \
        liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolManifest.java \
        liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillBoxFactory.java
git commit -m "feat(agent): load skills into skill box"
```

---

### Task 4: Add skill tracking hook tests and implementation

**Files:**
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillTrackingHookTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillTrackingHook.java`

- [ ] **Step 1: Write failing hook tests**

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillTrackingHookTest.java`:

```java
package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.agent.skill.SkillTrackingHook;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.ToolUseBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ReActAgentSkillTrackingHookTest {

    @Test
    public void testTracksLoadedSkillNames() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-id-1", "demo"));
        ToolUseBlock toolUse = new ToolUseBlock(
                "call-1",
                "load_skill_through_path",
                Map.of("skillId", "skill-id-1"));

        hook.onEvent(new PostActingEvent(null, null, toolUse, null)).block();

        Assertions.assertEquals(java.util.List.of("demo"), hook.getUsedSkills());
    }

    @Test
    public void testDeduplicatesAndClearsSkillNames() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-id-1", "demo"));
        ToolUseBlock toolUse = new ToolUseBlock(
                "call-1",
                "load_skill_through_path",
                Map.of("skillId", "skill-id-1"));

        hook.onEvent(new PostActingEvent(null, null, toolUse, null)).block();
        hook.onEvent(new PostActingEvent(null, null, toolUse, null)).block();
        Assertions.assertEquals(java.util.List.of("demo"), hook.getUsedSkills());

        hook.clear();
        Assertions.assertTrue(hook.getUsedSkills().isEmpty());
    }

    @Test
    public void testIgnoresNonSkillTools() {
        SkillTrackingHook hook = new SkillTrackingHook(Map.of("skill-id-1", "demo"));
        ToolUseBlock toolUse = new ToolUseBlock("call-1", "read_file", Map.of("path", "a.txt"));

        hook.onEvent(new PostActingEvent(null, null, toolUse, null)).block();

        Assertions.assertTrue(hook.getUsedSkills().isEmpty());
    }
}
```

- [ ] **Step 2: Run hook tests to verify they fail**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillTrackingHookTest test
```

Expected: FAIL with missing `SkillTrackingHook`.

- [ ] **Step 3: Implement `SkillTrackingHook`**

Create `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillTrackingHook.java`:

```java
package com.yomahub.liteflow.agent.skill;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.ToolUseBlock;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillTrackingHook implements Hook {

    public static final String LOAD_SKILL_TOOL_NAME = "load_skill_through_path";

    private final Map<String, String> skillIdToName;
    private final Set<String> usedSkills = Collections.synchronizedSet(new LinkedHashSet<>());

    public SkillTrackingHook(Map<String, String> skillIdToName) {
        this.skillIdToName = skillIdToName == null ? Map.of() : Map.copyOf(skillIdToName);
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostActingEvent actingEvent) {
            recordIfSkillLoad(actingEvent.getToolUse());
        }
        return Mono.just(event);
    }

    public List<String> getUsedSkills() {
        synchronized (usedSkills) {
            return List.copyOf(usedSkills);
        }
    }

    public void clear() {
        usedSkills.clear();
    }

    private void recordIfSkillLoad(ToolUseBlock toolUse) {
        if (toolUse == null || !LOAD_SKILL_TOOL_NAME.equals(toolUse.getName())) {
            return;
        }
        Map<String, Object> input = toolUse.getInput();
        Object skillId = input == null ? null : input.get("skillId");
        if (skillId == null) {
            return;
        }
        String id = skillId.toString();
        usedSkills.add(skillIdToName.getOrDefault(id, id));
    }
}
```

- [ ] **Step 4: Run hook tests**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillTrackingHookTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillTrackingHookTest.java \
        liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillTrackingHook.java
git commit -m "feat(agent): track loaded skills"
```

---

### Task 5: Integrate skills into ReActAgentComponent

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/AbstractReActAgentSpringbootTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillIntegrationTest.java`

- [ ] **Step 1: Write failing integration tests**

Create `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillIntegrationTest.java`:

```java
package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import com.yomahub.liteflow.test.agent.tool.SkillEchoTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ReActAgentSkillIntegrationTest extends AbstractReActAgentSpringbootTest {

    @Test
    public void testSkillsDisabledKeepsExistingToolSet() {
        liteflowConfig.getAgent().getSkills().setEnabled(false);

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "tools");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertFalse(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testSkillsEnabledAddsSkillLoadTool() {
        enableTestSkills();

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "skills");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertTrue(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testComponentSkillAllowListStillBuildsAgent() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("demo");

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "filtered-skills");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertTrue(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testMissingComponentSkillFailsInStrictMode() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("missing-skill");

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "missing-skill");

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("missing-skill"));
    }

    @Test
    public void testSkillFrontmatterToolIsInstantiatedDuringAgentBuild() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("tool-skill");
        SkillEchoTool.reset();

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "tool-skill");

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
    }

    private void enableTestSkills() {
        liteflowConfig.getAgent().getSkills().setEnabled(true);
        liteflowConfig.getAgent().getSkills().setPath("src/test/resources/agent/skills");
        liteflowConfig.getAgent().getSkills().setStrict(true);
    }
}
```

- [ ] **Step 2: Extend `StubReActAgentCmp` probes**

Modify `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`.

Add static fields near the other probes:

```java
    public static volatile List<String> allowedSkills = List.of();
    public static final List<List<String>> USED_SKILL_PROBES = new CopyOnWriteArrayList<>();
```

Add to `reset()`:

```java
        allowedSkills = List.of();
        USED_SKILL_PROBES.clear();
```

Add this override near `tools()`:

```java
    @Override
    protected List<String> skills() {
        return allowedSkills;
    }
```

Add this line at the top of `handleReply(Msg reply)`:

```java
        USED_SKILL_PROBES.add(usedSkills());
```

- [ ] **Step 3: Ensure tests default to skills disabled**

Modify `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/AbstractReActAgentSpringbootTest.java` inside `ensureAgentConfig()`:

```java
        agentConfig.getSkills().setEnabled(false);
        agentConfig.getSkills().setPath("src/test/resources/agent/skills");
        agentConfig.getSkills().setStrict(true);
```

- [ ] **Step 4: Run integration tests to verify they fail**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillIntegrationTest test
```

Expected: FAIL with missing `skills()`, `usedSkills()`, or no `load_skill_through_path` in tool schemas.

- [ ] **Step 5: Update `AgentSession` to keep skill hook**

Modify `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java`.

Add import:

```java
import com.yomahub.liteflow.agent.skill.SkillTrackingHook;
```

Add field:

```java
    private volatile SkillTrackingHook skillTrackingHook;
```

Add accessors:

```java
    public SkillTrackingHook getSkillTrackingHook() {
        return skillTrackingHook;
    }

    public void setSkillTrackingHook(SkillTrackingHook skillTrackingHook) {
        this.skillTrackingHook = skillTrackingHook;
    }
```

- [ ] **Step 6: Update `ReActAgentComponent` imports**

Add imports in `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`:

```java
import com.yomahub.liteflow.agent.skill.SkillBoxFactory;
import com.yomahub.liteflow.agent.skill.SkillLoadResult;
import com.yomahub.liteflow.agent.skill.SkillTrackingHook;
import io.agentscope.core.skill.SkillBox;
```

- [ ] **Step 7: Add skill extension methods to `ReActAgentComponent`**

Add constants near `CTX_KEY_PREFIX`:

```java
    private static final String SKILL_HOOK_KEY_PREFIX = "_react_agent_skill_hook_";
```

Add helper:

```java
    private String skillHookKey() {
        String nodeId = getNodeId();
        return SKILL_HOOK_KEY_PREFIX + (nodeId == null ? "default" : nodeId);
    }
```

Add optional overrides near `tools()`:

```java
    /**
     * Return skill names this component may use. Empty means all configured skills.
     */
    protected List<String> skills() { return List.of(); }

    /**
     * Whether agent-scope skills should be enabled for this component.
     */
    protected boolean enableSkills() { return agentConfig().getSkills().isEnabled(); }

    /**
     * Return skill names loaded by this agent during the current invocation.
     */
    protected final List<String> usedSkills() {
        SkillTrackingHook hook = getSlot().getAttachment(skillHookKey());
        return hook == null ? List.of() : hook.getUsedSkills();
    }
```

- [ ] **Step 8: Change agent construction to return hook metadata**

Inside `ReActAgentComponent`, add private record near `buildAgent()`:

```java
    private record BuiltAgent(ReActAgent agent, SkillTrackingHook skillTrackingHook) {
    }
```

Change `private ReActAgent buildAgent()` to:

```java
    private BuiltAgent buildAgent() {
```

Replace the final return statement with builder variable logic:

```java
        ReActAgent.Builder builder = ReActAgent.builder()
                .name(getNodeId() == null ? "liteflow-agent" : getNodeId())
                .sysPrompt(systemPrompt())
                .model(buildModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(iters)
                .hooks(allHooks);

        SkillTrackingHook skillTrackingHook = null;
        if (enableSkills()) {
            SkillLoadResult skillLoadResult = SkillBoxFactory.build(toolkit, cfg, skills());
            SkillBox skillBox = skillLoadResult.skillBox();
            skillTrackingHook = new SkillTrackingHook(skillLoadResult.skillIdToName());
            allHooks.add(skillTrackingHook);
            builder.skillBox(skillBox).hooks(allHooks);
        }

        return new BuiltAgent(builder.build(), skillTrackingHook);
```

When applying this snippet, keep `allHooks` creation before builder creation so the skill hook is included before `build()`.

- [ ] **Step 9: Update `process()` to store and clear the hook**

Replace this block in `process()`:

```java
                ReActAgent agent = (ReActAgent) session.getAgent();
                if (agent == null) {
                    agent = buildAgent();
                    mgr.loadIfExists(session, agent);
                    session.setAgent(agent);
                }
```

with:

```java
                ReActAgent agent = (ReActAgent) session.getAgent();
                if (agent == null) {
                    BuiltAgent built = buildAgent();
                    agent = built.agent();
                    session.setSkillTrackingHook(built.skillTrackingHook());
                    mgr.loadIfExists(session, agent);
                    session.setAgent(agent);
                }
                SkillTrackingHook skillHook = session.getSkillTrackingHook();
                if (skillHook != null) {
                    skillHook.clear();
                    slot.setAttachment(skillHookKey(), skillHook);
                }
```

Inside the `finally` block that removes ctx, also remove the skill hook attachment:

```java
                slot.removeAttachment(skillHookKey());
```

The final cleanup should remove both `ctxKey()` and `skillHookKey()`.

- [ ] **Step 10: Run integration tests**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentSkillIntegrationTest test
```

Expected: PASS.

- [ ] **Step 11: Run existing tool tests**

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentToolTest test
```

Expected: PASS.

- [ ] **Step 12: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java \
        liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/AbstractReActAgentSpringbootTest.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentSkillIntegrationTest.java
git commit -m "feat(agent): integrate skills with react agent component"
```

---

### Task 6: Document public API behavior in JavaDoc and package docs

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java`

- [ ] **Step 1: Add JavaDoc to `ReActAgentComponent` class comment**

Add this paragraph before the existing warning about not caching ctx:

```java
 * <p>When {@code liteflow.agent.skills.enabled=true}, the component can load
 * agent-scope skills from {@code liteflow.agent.skills.path}. Override
 * {@link #skills()} to restrict the component to a fixed allow-list; an empty
 * list means all configured skills are available. The allow-list is treated as
 * a stable component capability declaration and should not depend on request
 * data because ReActAgent instances are cached by conversationId and agentKey.
```

- [ ] **Step 2: Add JavaDoc to `AgentConfig`**

Add this field comment above the `skills` field:

```java
    /** Skills configuration for loading agent-scope SkillBox entries from SKILL.md repositories. */
```

- [ ] **Step 3: Update package info**

Open `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java` and add this paragraph inside the existing package documentation:

```java
 * <p>Skills support is configuration-driven. Set
 * {@code liteflow.agent.skills.enabled=true} and point
 * {@code liteflow.agent.skills.path} at a filesystem skills repository. A
 * component may override {@code skills()} to restrict which skill names are
 * available to that agent. Skill-specific Java tools can be declared in
 * {@code SKILL.md} frontmatter with a {@code tools} field.
```

- [ ] **Step 4: Compile docs changes**

```bash
mvn -pl liteflow-react-agent/liteflow-react-agent-core -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java \
        liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java \
        liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java
git commit -m "docs(agent): describe react agent skills"
```

---

### Task 7: Run full verification for affected modules

**Files:**
- No source changes expected unless verification reveals a concrete failure.

- [ ] **Step 1: Run core agent module tests**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent
```

Expected: PASS.

- [ ] **Step 2: Compile all react-agent provider modules**

```bash
mvn -pl liteflow-react-agent -DskipTests package
```

Expected: PASS.

- [ ] **Step 3: Run a focused full affected build**

```bash
mvn -pl liteflow-core,liteflow-react-agent,liteflow-testcase-el/liteflow-testcase-el-react-agent -DskipTests package
```

Expected: PASS.

- [ ] **Step 4: Inspect git diff**

```bash
git diff --stat HEAD
```

Expected: only files listed in this plan are changed.

- [ ] **Step 5: Finish verification**

If Step 1 through Step 3 pass, record the exact command output in the final implementation summary. If a command fails, return to the task that introduced the failing file, make a focused fix there, rerun the failing command, and then rerun all commands in this verification task.

Expected: no source changes are needed during this task when earlier tasks were implemented correctly.

---

## Self-Review Checklist

- Spec coverage:
  - Config-driven skills path is covered by Tasks 1 and 3.
  - Component-level `skills()` allow-list is covered by Task 5.
  - No `dependentSkills()` API is introduced.
  - Missing declared skills fail in strict mode through Task 3 tests.
  - Frontmatter Java tool binding is covered by Tasks 2 and 3.
  - Skill tracking is covered by Task 4 and exposed by Task 5.
  - Agent-scope code execution remains disabled because no call to `skillBox.codeExecution()` is added.
- Placeholder scan: no implementation step relies on an unspecified class, method, or path.
- Type consistency:
  - `AgentConfig#getSkills()` returns `SkillsConfig`.
  - `SkillBoxFactory.build(Toolkit, AgentConfig, List<String>)` returns `SkillLoadResult`.
  - `SkillTrackingHook#getUsedSkills()` returns `List<String>`.
  - `ReActAgentComponent#usedSkills()` returns `List<String>`.
