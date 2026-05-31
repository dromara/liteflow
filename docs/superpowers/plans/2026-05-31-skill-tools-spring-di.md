# Skill Tools Spring DI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 skill frontmatter 中 `tools` 声明的 Java 工具类，从框架容器（Spring/Solon）按类型取出已注册的 bean，使其依赖注入生效；无容器/未注册/容器未就绪时降级反射实例化。

**Architecture:** 仅改 `SkillToolResolver` 实例化工具的一步，从 `clazz.getDeclaredConstructor().newInstance()` 改为走 LiteFlow 既有的 `ContextAware` SPI（`ContextAwareHolder.loadContextAware()` → `hasBean(Class)` → `getBean(Class)`）。容器访问用 try-catch 包裹，任何异常（含 classpath 含 spring 但 `SpringAware.applicationContext` 未初始化时的 NPE）都降级为反射实例化，保证健壮且不破坏无容器单元测试。

**Tech Stack:** Java 17, Maven, JUnit 5, agentscope, LiteFlow `ContextAware` SPI（`SpringAware`/`SolonContextAware`/`LocalContextAware`）。

---

## File Structure

- **Modify** `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolResolver.java`
  - 职责不变（把 skill 的 `tools` 解析为可注册工具实例），仅把"实例化方式"从反射 new 改为容器优先 + 降级。
- **Modify** `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/unit/SkillBoxFactoryTest.java`
  - 新增受控的 stub `ContextAware`（内部静态类）+ 反射注入 `ContextAwareHolder` 的辅助方法 + 3 个测试（复用 / 降级 / 防御）。

### 关键既有事实（实现者须知）

- `SkillToolResolver` 与 `instantiateTools` 都是 **package-private**（`final class SkillToolResolver`），且位于 `com.yomahub.liteflow.agent.skill` 包；测试在 `com.yomahub.liteflow.test.agent.unit` 包，**不能直接调用**它，只能通过 public 的 `SkillBoxFactory.build(...)` 间接驱动。
- 现有验证手段：`SkillEchoTool` 构造时 `CONSTRUCT_COUNT.incrementAndGet()`，`SkillEchoTool.reset()` 归零；`SkillBoxFactoryTest.setUp()` 已调用 `SkillEchoTool.reset()`。
- 测试用 skill `tool-skill` 的 `SKILL.md` 声明：`tools: com.yomahub.liteflow.test.agent.tool.SkillEchoTool`。
- `liteflow-react-agent-core` 已依赖 `liteflow-core`，可直接 import `com.yomahub.liteflow.spi.ContextAware` 与 `com.yomahub.liteflow.spi.holder.ContextAwareHolder`。
- `ContextAwareHolder` 字段为 `private static ContextAware contextAware;`，并提供 `public static void clean()`（置 null）。无 public setter，故测试用反射写入该字段、用 `clean()` 复原。
- `LocalContextAware`（`com.yomahub.liteflow.spi.local.LocalContextAware`）是 public 类，方法均 public，可被测试 stub 继承并覆写 `hasBean(Class)` / `getBean(Class)`，其余方法沿用其空实现。

---

## Task 1: SkillToolResolver 改为容器优先 + 降级，并以 TDD 驱动复用路径

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolResolver.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/unit/SkillBoxFactoryTest.java`

- [ ] **Step 1: 写失败测试（复用容器 bean，不应再反射构造）**

在 `SkillBoxFactoryTest.java` 顶部 import 区追加（与现有 import 风格一致，放在已有 import 之后）：

```java
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
```

在 `SkillBoxFactoryTest` 类体内（紧接 `setUp()` 之后）新增：受控 stub、反射注入辅助方法、复用测试。

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn -q -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am test \
  -Dtest=SkillBoxFactoryTest#testRegisteredToolBeanIsReusedFromContainer
```
Expected: FAIL — 断言 `expected: <1> but was: <2>`（旧实现仍反射 `new SkillEchoTool()`，CONSTRUCT_COUNT 变为 2）。

- [ ] **Step 3: 实现 SkillToolResolver 容器优先 + 降级**

在 `SkillToolResolver.java` import 区追加：

```java
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
```

将现有 `instantiateTools` 方法整体替换为下面两个方法（保留 `resolveToolClasses`、`toClassNameList`、`handleProblem` 不变）：

```java
    /**
     * 解析并实例化指定技能声明的工具。技能未声明 {@code tools} 时返回空列表。
     *
     * <p>工具类优先从框架容器（Spring/Solon）按类型取已注册的 bean，使其依赖注入生效；
     * 无容器、未注册或容器访问异常时，降级为反射实例化（依赖注入不可用）。
     */
    List<Object> instantiateTools(AgentSkill skill) {
        List<Class<?>> classes = resolveToolClasses(skill);
        if (classes.isEmpty()) {
            return List.of();
        }
        ContextAware contextAware = ContextAwareHolder.loadContextAware();
        List<Object> instances = new ArrayList<>(classes.size());
        for (Class<?> clazz : classes) {
            try {
                instances.add(resolveToolInstance(contextAware, skill, clazz));
            } catch (ReflectiveOperationException e) {
                handleProblem("Skill '" + skill.getName() + "' tool class '" + clazz.getName()
                        + "' instantiation failed", e);
            }
        }
        return List.copyOf(instances);
    }

    private Object resolveToolInstance(ContextAware contextAware, AgentSkill skill, Class<?> clazz)
            throws ReflectiveOperationException {
        try {
            if (contextAware.hasBean(clazz)) {
                return contextAware.getBean(clazz);
            }
        } catch (Exception ex) {
            // 容器未就绪（如 classpath 含 spring 但 ApplicationContext 尚未初始化）等异常：降级反射实例化
            LOG.warn("Skill '{}' resolving tool '{}' from container failed ({}); "
                    + "falling back to reflective instantiation",
                    skill.getName(), clazz.getName(), ex.toString());
        }
        Object instance = clazz.getDeclaredConstructor().newInstance();
        LOG.info("Skill '{}' tool '{}' not found in container; fell back to reflective "
                + "instantiation, dependency injection unavailable", skill.getName(), clazz.getName());
        return instance;
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run:
```bash
mvn -q -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am test \
  -Dtest=SkillBoxFactoryTest#testRegisteredToolBeanIsReusedFromContainer
```
Expected: PASS（hasBean=true → getBean 复用 prebuilt，未再构造，CONSTRUCT_COUNT 保持 1）。

- [ ] **Step 5: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolResolver.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/unit/SkillBoxFactoryTest.java
git commit -m "feat(agent): resolve skill tools from container to enable DI

SkillToolResolver 优先按类型从 ContextAware 容器取已注册的工具 bean，
使其依赖注入生效；容器未就绪/未注册时降级反射实例化。

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: 补充降级路径与防御路径的显式回归测试

**Files:**
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/unit/SkillBoxFactoryTest.java`

- [ ] **Step 1: 新增降级与防御测试**

在 `SkillBoxFactoryTest` 中、`testRegisteredToolBeanIsReusedFromContainer` 之后追加：

```java
    @Test
    public void testToolFallsBackToReflectionWhenAbsentFromContainer() throws Exception {
        StubContextAware emptyContainer = new StubContextAware(); // 不注册任何 bean
        installContextAware(emptyContainer);
        try {
            SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("tool-skill"));

            Assertions.assertEquals(List.of("tool-skill"), result.skillNames());
            // 容器中无该 bean，降级反射实例化一次
            Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
        } finally {
            ContextAwareHolder.clean();
        }
    }

    @Test
    public void testToolFallsBackToReflectionWhenContainerAccessFails() throws Exception {
        StubContextAware brokenContainer = new StubContextAware(true); // hasBean 抛异常，模拟容器未就绪
        installContextAware(brokenContainer);
        try {
            SkillLoadResult result = SkillBoxFactory.build(new Toolkit(), cfg, List.of("tool-skill"));

            Assertions.assertEquals(List.of("tool-skill"), result.skillNames());
            // 容器访问异常被吞掉并降级反射实例化一次
            Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
        } finally {
            ContextAwareHolder.clean();
        }
    }
```

- [ ] **Step 2: 运行整个测试类确认全部通过**

Run:
```bash
mvn -q -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am test -Dtest=SkillBoxFactoryTest
```
Expected: PASS — 包含原有用例（`testFrontmatterToolClassIsInstantiated`、`testInlineArrayToolsAreInstantiated`、`testBrokenSiblingSkillOutsideAllowListDoesNotFailBuild` 等，它们在 stub 未注入时通过 try-catch 降级继续通过）与三个新用例，全绿。

- [ ] **Step 3: 提交**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/unit/SkillBoxFactoryTest.java
git commit -m "test(agent): cover skill tool container fallback and defensive paths

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## 与 spec 的差异说明

spec（`docs/superpowers/specs/2026-05-31-skill-tools-spring-di-design.md`）核心改动给出的是 `if (hasBean) getBean else new` 的骨架；实现额外用 **try-catch 包裹容器访问**，把"容器未就绪导致的异常"也并入降级路径。这是 spec「风险/降级」章节精神的具体化，不改变设计意图——已在 Task 1 Step 3 与本节明示，便于审阅者确认。

## Self-Review 结论

- **Spec coverage**：核心改动（容器按类型取 bean）→ Task 1；降级（无容器/未注册）→ Task 1 实现 + Task 2 测试；防御（容器异常）→ Task 1 实现 + Task 2 测试；测试计划（保留降级用例 + 新增注入用例 assertSame 语义以 CONSTRUCT_COUNT 计数等价实现）→ Task 1/2。无遗漏。
- **Placeholder scan**：无 TBD/TODO，所有步骤含完整代码与确切命令。
- **Type consistency**：`StubContextAware`、`installContextAware`、`ContextAwareHolder.clean()`、`SkillEchoTool.CONSTRUCT_COUNT`、`SkillBoxFactory.build(Toolkit, AgentConfig, List<String>)`、`resolveToolInstance(ContextAware, AgentSkill, Class<?>)` 在各任务间引用一致。
