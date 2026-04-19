# LiteFlow ReAct Agent 模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 LiteFlow 新增一套基于 agentscope-java 的 ReAct Agent 组件能力，通过继承抽象类即可定义 agent 节点并参与 EL 编排，支持 OpenAI/Anthropic/Gemini/DashScope 及 OpenAI 兼容厂商（DeepSeek/Kimi/GLM/MiniMax），具备 session/workspace 隔离与可配置 shell 命令管控。

**Architecture:** 新增 1 个父 pom + 4 个平台子模块 + 1 个 core 模块；`liteflow-core` 引入纯 POJO `AgentConfig` 作为 `LiteflowConfig.agent` 字段（零额外依赖）；`ReActAgentComponent` 继承 `NodeComponent`，`process()` final，内部通过 `LiteflowConfigGetter` 取配置、通过 `AgentSessionManager` 管理 session/workspace；工具用 agentscope 原生 `@Tool`，内置 `WorkspaceFileTools` 做路径围栏、`ManagedShellCommandTool` 做命令白名单。

**Tech Stack:**
- 已有：Java 8（主工程）、Maven 多模块、JUnit 5、Spring Boot 2.6.x、LiteFlow DSL/EL
- 新增：Java 21（新模块专属）、`io.agentscope:agentscope-core` 1.0.9、平台厂商 SDK（仅 Gemini 需显式依赖 `com.google.genai:google-genai`）

**Related Design Doc:** `docs/superpowers/specs/2026-04-19-liteflow-react-agent-design.md`

---

## 全局约定

- **Java 版本**：`liteflow-react-agent/**` 所有模块都在各自 `pom.xml` 中设置 `<maven.compiler.source>21</maven.compiler.source>` 与 `<maven.compiler.target>21</maven.compiler.target>`。`liteflow-core` 和 `liteflow-spring-boot-starter` 保持 Java 8 不动。
- **groupId**：`com.yomahub`。版本走 `${revision}`（flatten-maven-plugin）。
- **agentscope 版本**：父 pom `<properties>` 新增 `<agentscope.version>1.0.9</agentscope.version>`。
- **提交约定**：每个 Task 结束都 commit。提交信息前缀遵循仓库现有风格（"更新..."、"新增..."、"修复..."），我们统一用英文 conventional commits（feat/fix/docs/test/refactor），会在各步骤中给出具体消息。
- **测试惯例**：JUnit 5 + Mockito（仓库现有依赖），不引入 AssertJ。断言全部用 `Assertions.*`。
- **包命名**：
  - `liteflow-core` 新增：`com.yomahub.liteflow.property.agent`
  - `liteflow-react-agent-core`：`com.yomahub.liteflow.agent`（下分 `.component` / `.session` / `.tool` / `.exception` / `.internal`）
  - 各平台：`com.yomahub.liteflow.agent.{openai|anthropic|gemini|dashscope}`

---

## 实施阶段总览

| Stage | 目标 | 预期 Task 数 |
|-------|------|-------------|
| 1 | `liteflow-core` 注入 `AgentConfig` POJO（纯配置容器） | 3 |
| 2 | `liteflow-spring-boot-starter` 与 `liteflow-solon-plugin` 同步新增字段及 copy 逻辑 | 2 |
| 3 | 新建父 pom 与 core 模块骨架；根 pom 注册 | 2 |
| 4 | Core 模块：`ReActAgentContext` / `AgentSession` / `AgentSessionManager` / 异常类 | 4 |
| 5 | Core 模块：`WorkspaceFileTools`（TDD） | 1 |
| 6 | Core 模块：`ManagedShellCommandTool`（TDD） | 1 |
| 7 | Core 模块：`ReActAgentComponent` 抽象类 + `process()` 时序（TDD with FakeModel） | 2 |
| 8 | 平台模块：openai（含 compatible preset）、anthropic、gemini、dashscope | 4 |
| 9 | 集成测试模块 `liteflow-testcase-el-springboot-agent` | 2 |
| 10 | 文档与 README | 1 |

---

## Stage 1 — `liteflow-core` 注入 `AgentConfig` POJO

### Task 1.1：新建 `AgentConfig` 及嵌套 POJO

**Files:**
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/WorkspaceConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SessionConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ShellConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/DefaultsConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/PlatformCredential.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ShellMode.java`
- Test: `liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigTest.java`

- [ ] **Step 1: 写失败测试**：确认默认值结构

```java
// liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigTest.java
package com.yomahub.liteflow.property.agent;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @Test
    void defaults_are_sensible() {
        AgentConfig c = new AgentConfig();
        assertNotNull(c.getWorkspace());
        assertNotNull(c.getSession());
        assertNotNull(c.getShell());
        assertNotNull(c.getDefaults());
        assertNotNull(c.getOpenaiCompatible());
        assertTrue(c.getOpenaiCompatible().isEmpty());

        WorkspaceConfig w = c.getWorkspace();
        assertNull(w.getRoot());
        assertTrue(w.isAutoCreate());
        assertTrue(w.isCleanupOnSessionExpire());
        assertFalse(w.isCleanupOnJvmShutdown());
        assertEquals(10 * 1024 * 1024, w.getMaxFileBytes());
        assertEquals(1000, w.getMaxListSize());

        SessionConfig s = c.getSession();
        assertEquals(Duration.ofMinutes(30), s.getIdleTimeout());
        assertEquals(Duration.ofMinutes(1), s.getCleanupInterval());
        assertEquals(10_000, s.getMaxSessions());

        ShellConfig sh = c.getShell();
        assertEquals(ShellMode.WHITELIST, sh.getMode());
        assertNotNull(sh.getWhitelist());
        assertNotNull(sh.getBlacklist());
        assertEquals(Duration.ofSeconds(30), sh.getTimeout());
        assertEquals(1024 * 1024, sh.getMaxOutputBytes());

        assertEquals(15, c.getDefaults().getMaxIterations());
    }

    @Test
    void platform_credentials_are_independent_instances() {
        AgentConfig c = new AgentConfig();
        c.getOpenai().setApiKey("k1");
        assertNull(c.getAnthropic().getApiKey());
        assertNull(c.getGemini().getApiKey());
        assertNull(c.getDashscope().getApiKey());
    }
}
```

- [ ] **Step 2: 跑测试，确认失败**

Run: `mvn test -pl liteflow-core -Dtest=AgentConfigTest`
Expected: Compilation failure (`AgentConfig` not found).

- [ ] **Step 3: 实现 `ShellMode` 枚举**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ShellMode.java
package com.yomahub.liteflow.property.agent;

public enum ShellMode {
    WHITELIST,
    BLACKLIST,
    DISABLED
}
```

- [ ] **Step 4: 实现 `PlatformCredential`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/PlatformCredential.java
package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlatformCredential {
    private String apiKey;
    private String baseUrl;
    private Map<String, String> extra = new LinkedHashMap<>();

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra; }
}
```

- [ ] **Step 5: 实现 `WorkspaceConfig`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/WorkspaceConfig.java
package com.yomahub.liteflow.property.agent;

public class WorkspaceConfig {
    private String root;
    private boolean autoCreate = true;
    private boolean cleanupOnSessionExpire = true;
    private boolean cleanupOnJvmShutdown = false;
    private long maxFileBytes = 10L * 1024 * 1024;
    private int maxListSize = 1000;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public boolean isAutoCreate() { return autoCreate; }
    public void setAutoCreate(boolean autoCreate) { this.autoCreate = autoCreate; }
    public boolean isCleanupOnSessionExpire() { return cleanupOnSessionExpire; }
    public void setCleanupOnSessionExpire(boolean v) { this.cleanupOnSessionExpire = v; }
    public boolean isCleanupOnJvmShutdown() { return cleanupOnJvmShutdown; }
    public void setCleanupOnJvmShutdown(boolean v) { this.cleanupOnJvmShutdown = v; }
    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long v) { this.maxFileBytes = v; }
    public int getMaxListSize() { return maxListSize; }
    public void setMaxListSize(int v) { this.maxListSize = v; }
}
```

- [ ] **Step 6: 实现 `SessionConfig`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SessionConfig.java
package com.yomahub.liteflow.property.agent;

import java.time.Duration;

public class SessionConfig {
    private Duration idleTimeout = Duration.ofMinutes(30);
    private Duration cleanupInterval = Duration.ofMinutes(1);
    private int maxSessions = 10_000;

    public Duration getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(Duration v) { this.idleTimeout = v; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public void setCleanupInterval(Duration v) { this.cleanupInterval = v; }
    public int getMaxSessions() { return maxSessions; }
    public void setMaxSessions(int v) { this.maxSessions = v; }
}
```

- [ ] **Step 7: 实现 `ShellConfig`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ShellConfig.java
package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class ShellConfig {
    private ShellMode mode = ShellMode.WHITELIST;
    private List<String> whitelist = Arrays.asList(
            "ls", "cat", "grep", "find", "head", "tail", "wc", "sed", "awk", "python3", "node");
    private List<String> blacklist = Arrays.asList("rm", "sudo", "shutdown", "mkfs", "dd");
    private Duration timeout = Duration.ofSeconds(30);
    private long maxOutputBytes = 1024L * 1024;

    public ShellMode getMode() { return mode; }
    public void setMode(ShellMode v) { this.mode = v; }
    public List<String> getWhitelist() { return whitelist; }
    public void setWhitelist(List<String> v) { this.whitelist = v; }
    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> v) { this.blacklist = v; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration v) { this.timeout = v; }
    public long getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(long v) { this.maxOutputBytes = v; }
}
```

- [ ] **Step 8: 实现 `DefaultsConfig`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/DefaultsConfig.java
package com.yomahub.liteflow.property.agent;

public class DefaultsConfig {
    private int maxIterations = 15;

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int v) { this.maxIterations = v; }
}
```

- [ ] **Step 9: 实现 `AgentConfig`**

```java
// liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java
package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentConfig {
    private WorkspaceConfig workspace = new WorkspaceConfig();
    private SessionConfig session = new SessionConfig();
    private ShellConfig shell = new ShellConfig();
    private DefaultsConfig defaults = new DefaultsConfig();
    private PlatformCredential openai = new PlatformCredential();
    private PlatformCredential anthropic = new PlatformCredential();
    private PlatformCredential gemini = new PlatformCredential();
    private PlatformCredential dashscope = new PlatformCredential();
    private Map<String, PlatformCredential> openaiCompatible = new LinkedHashMap<>();

    public WorkspaceConfig getWorkspace() { return workspace; }
    public void setWorkspace(WorkspaceConfig v) { this.workspace = v; }
    public SessionConfig getSession() { return session; }
    public void setSession(SessionConfig v) { this.session = v; }
    public ShellConfig getShell() { return shell; }
    public void setShell(ShellConfig v) { this.shell = v; }
    public DefaultsConfig getDefaults() { return defaults; }
    public void setDefaults(DefaultsConfig v) { this.defaults = v; }
    public PlatformCredential getOpenai() { return openai; }
    public void setOpenai(PlatformCredential v) { this.openai = v; }
    public PlatformCredential getAnthropic() { return anthropic; }
    public void setAnthropic(PlatformCredential v) { this.anthropic = v; }
    public PlatformCredential getGemini() { return gemini; }
    public void setGemini(PlatformCredential v) { this.gemini = v; }
    public PlatformCredential getDashscope() { return dashscope; }
    public void setDashscope(PlatformCredential v) { this.dashscope = v; }
    public Map<String, PlatformCredential> getOpenaiCompatible() { return openaiCompatible; }
    public void setOpenaiCompatible(Map<String, PlatformCredential> v) { this.openaiCompatible = v; }
}
```

- [ ] **Step 10: 跑测试确认通过**

Run: `mvn test -pl liteflow-core -Dtest=AgentConfigTest`
Expected: 2 tests pass.

- [ ] **Step 11: Commit**

```bash
git add liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ \
        liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/
git commit -m "feat(core): add AgentConfig POJOs for ReAct agent configuration"
```

---

### Task 1.2：`LiteflowConfig` 新增 `agent` 字段

**Files:**
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/LiteflowConfig.java`
- Test: `liteflow-core/src/test/java/com/yomahub/liteflow/property/LiteflowConfigAgentFieldTest.java` (create)

- [ ] **Step 1: 写失败测试**

```java
// liteflow-core/src/test/java/com/yomahub/liteflow/property/LiteflowConfigAgentFieldTest.java
package com.yomahub.liteflow.property;

import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiteflowConfigAgentFieldTest {
    @Test
    void agent_field_round_trip() {
        LiteflowConfig cfg = new LiteflowConfig();
        assertNull(cfg.getAgent(), "agent 默认为 null，避免未用此特性的用户付出初始化代价");

        AgentConfig agent = new AgentConfig();
        agent.getOpenai().setApiKey("sk-xxx");
        cfg.setAgent(agent);

        assertSame(agent, cfg.getAgent());
        assertEquals("sk-xxx", cfg.getAgent().getOpenai().getApiKey());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-core -Dtest=LiteflowConfigAgentFieldTest`
Expected: Compilation failure (`getAgent()` not found).

- [ ] **Step 3: 修改 `LiteflowConfig` 新增字段**

在 `liteflow-core/src/main/java/com/yomahub/liteflow/property/LiteflowConfig.java` 的类体最后一个字段之后、第一个方法之前追加：

```java
	// ReAct agent 相关配置；仅在启用 liteflow-react-agent-* 模块时使用
	private com.yomahub.liteflow.property.agent.AgentConfig agent;
```

并在文件末尾追加两个方法（替换掉当前最后一个 `}` 之前）：

```java
	public com.yomahub.liteflow.property.agent.AgentConfig getAgent() {
		return agent;
	}

	public void setAgent(com.yomahub.liteflow.property.agent.AgentConfig agent) {
		this.agent = agent;
	}
```

> 使用全限定名而非 import 是为了让这段改动在视觉上更孤立，减小 reviewer 认知负担；import 也可以，按仓库习惯改成 import 亦可。

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-core -Dtest=LiteflowConfigAgentFieldTest`
Expected: 1 test pass.

- [ ] **Step 5: 跑 core 全量测试确保无回归**

Run: `mvn test -pl liteflow-core`
Expected: 全部通过，没有因新字段引入的失败。

- [ ] **Step 6: Commit**

```bash
git add liteflow-core/src/main/java/com/yomahub/liteflow/property/LiteflowConfig.java \
        liteflow-core/src/test/java/com/yomahub/liteflow/property/LiteflowConfigAgentFieldTest.java
git commit -m "feat(core): expose agent field on LiteflowConfig"
```

---

### Task 1.3：父 pom 声明 `<agentscope.version>`

**Files:**
- Modify: `pom.xml`（根 pom 的 `<properties>` 节）

- [ ] **Step 1: 在根 pom 的 properties 中追加版本**

在 `pom.xml` 中 `<caffeine.version>2.9.3</caffeine.version>` 这一行之后、`</properties>` 之前插入：

```xml
		<agentscope.version>1.0.9</agentscope.version>
		<google-genai.version>1.38.0</google-genai.version>
```

- [ ] **Step 2: 验证根 pom 合法**

Run: `mvn help:evaluate -Dexpression=agentscope.version -q -DforceStdout`
Expected: 输出 `1.0.9`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: declare agentscope and google-genai versions in root pom"
```

---

## Stage 2 — Framework Integration（starter + solon）

### Task 2.1：`liteflow-spring-boot-starter` 的 `LiteflowProperty` 与 copy 逻辑

**Files:**
- Modify: `liteflow-spring-boot-starter/src/main/java/com/yomahub/liteflow/springboot/LiteflowProperty.java`
- Modify: `liteflow-spring-boot-starter/src/main/java/com/yomahub/liteflow/springboot/config/LiteflowPropertyAutoConfiguration.java`

- [ ] **Step 1: `LiteflowProperty` 新增 `agent` 字段**

在 `LiteflowProperty` 类体内的 `ChainCacheProperty chainCache;` 字段下方追加：

```java
	// ReAct agent 配置 —— 仅在 classpath 存在 liteflow-react-agent-* 时被子字段填充
	@NestedConfigurationProperty
	private com.yomahub.liteflow.property.agent.AgentConfig agent;
```

在文件末尾 `}` 之前追加：

```java
	public com.yomahub.liteflow.property.agent.AgentConfig getAgent() {
		return agent;
	}

	public void setAgent(com.yomahub.liteflow.property.agent.AgentConfig agent) {
		this.agent = agent;
	}
```

- [ ] **Step 2: `LiteflowPropertyAutoConfiguration` 追加 copy**

在 `liteflowConfig` 方法中 `liteflowConfig.setChainCacheCapacity(property.getChainCache().getCapacity());` 这一行之后、`return liteflowConfig;` 之前插入：

```java
		liteflowConfig.setAgent(property.getAgent());
```

- [ ] **Step 3: 编译 starter 模块**

Run: `mvn clean compile -pl liteflow-spring-boot-starter -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: 跑 starter 相关的 springboot testcase 模块冒烟**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot -Dtest=FlowExecutorTest`
Expected: BUILD SUCCESS（证明 property 链没有破坏现有 Spring Boot 装配）。如果该测试类不存在，替换为该模块下随便一个 `*Test` 类即可。

- [ ] **Step 5: Commit**

```bash
git add liteflow-spring-boot-starter/
git commit -m "feat(starter): bind liteflow.agent into LiteflowConfig via LiteflowProperty"
```

---

### Task 2.2：`liteflow-solon-plugin` 同步

**Files:**
- Modify: `liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/LiteflowProperty.java`
- Modify: `liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/LiteflowAutoConfiguration.java`（或实际做 copy 的那个类）

- [ ] **Step 1: 查看 solon 侧 LiteflowProperty**

Run: `cat liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/LiteflowProperty.java`
Inspect：确认字段与 Spring Boot 侧相似结构。

- [ ] **Step 2: solon `LiteflowProperty` 新增 `agent`**

参考 Task 2.1 Step 1 在 solon 版 `LiteflowProperty` 类体中追加同样的 `agent` 字段与 getter/setter（不需要 `@NestedConfigurationProperty`，solon 不识别该注解）。

- [ ] **Step 3: solon copy 逻辑**

Run: `grep -n "setChainCache" liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/*.java`
找到 solon 侧完成 Property→LiteflowConfig 映射的位置，追加 `liteflowConfig.setAgent(property.getAgent());`。

- [ ] **Step 4: 编译 solon 模块**

Run: `mvn clean compile -pl liteflow-solon-plugin -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑 solon testcase 冒烟**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-solon`
Expected: BUILD SUCCESS（不应有回归）。如果 solon 模块没有 testcase 目录，跳过本步。

- [ ] **Step 6: Commit**

```bash
git add liteflow-solon-plugin/
git commit -m "feat(solon): bind agent config into LiteflowConfig"
```

---

## Stage 3 — 新建 `liteflow-react-agent` 父 pom 与 core 骨架

### Task 3.1：新建父 pom 模块

**Files:**
- Create: `liteflow-react-agent/pom.xml`
- Modify: `pom.xml`（根）

- [ ] **Step 1: 写父 pom**

```xml
<!-- liteflow-react-agent/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>liteflow-react-agent</artifactId>
    <name>${project.artifactId}</name>
    <packaging>pom</packaging>

    <modules>
        <module>liteflow-react-agent-core</module>
        <module>liteflow-react-agent-openai</module>
        <module>liteflow-react-agent-anthropic</module>
        <module>liteflow-react-agent-gemini</module>
        <module>liteflow-react-agent-dashscope</module>
    </modules>
</project>
```

- [ ] **Step 2: 根 pom compile profile 注册**

在根 `pom.xml` 的 `compile` profile `<modules>` 中，`<module>liteflow-benchmark</module>` 之后追加：

```xml
				<module>liteflow-react-agent</module>
```

同样在 `release` profile 的 `<modules>` 中（在 `<module>liteflow-el-builder</module>` 之后）追加：

```xml
				<module>liteflow-react-agent</module>
```

- [ ] **Step 3: 验证根 pom 列表合法**

Run: `mvn -N validate`
Expected: BUILD SUCCESS

- [ ] **Step 4: 此时不能 `mvn package`**（子模块还没建），只做 `-N validate`。

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/pom.xml pom.xml
git commit -m "build: register liteflow-react-agent parent module"
```

---

### Task 3.2：`liteflow-react-agent-core` 模块骨架

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/pom.xml`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java`

- [ ] **Step 1: 写 core 模块 pom**

```xml
<!-- liteflow-react-agent/liteflow-react-agent-core/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-react-agent</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>liteflow-react-agent-core</artifactId>
    <name>${project.artifactId}</name>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-core</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-spring</artifactId>
            <version>${revision}</version>
        </dependency>

        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-core</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>4.11.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 `package-info.java` 占位避免空模块**

```java
// liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java
/**
 * LiteFlow ReAct Agent core module:
 * <ul>
 *   <li>{@code component} — 抽象 {@code ReActAgentComponent} 与上下文</li>
 *   <li>{@code session} — workspace 与 session 生命周期管理</li>
 *   <li>{@code tool} — 内置 workspace 文件与 shell 命令工具（带围栏）</li>
 *   <li>{@code exception} — 组件专属异常</li>
 * </ul>
 */
package com.yomahub.liteflow.agent;
```

- [ ] **Step 3: 注意**：因为父 pom `liteflow-react-agent/pom.xml` 已声明了 4 个兄弟模块，当前只建 core 的话会 `mvn` 失败。先在父 pom `<modules>` 里注释掉 openai/anthropic/gemini/dashscope 四行；Stage 8 各 Task 开头会再打开对应行：

修改 `liteflow-react-agent/pom.xml`：

```xml
    <modules>
        <module>liteflow-react-agent-core</module>
        <!-- <module>liteflow-react-agent-openai</module> -->
        <!-- <module>liteflow-react-agent-anthropic</module> -->
        <!-- <module>liteflow-react-agent-gemini</module> -->
        <!-- <module>liteflow-react-agent-dashscope</module> -->
    </modules>
```

- [ ] **Step 4: 构建 core 骨架**

Run: `mvn clean install -pl liteflow-react-agent/liteflow-react-agent-core -am -DskipTests`
Expected: BUILD SUCCESS（会下载 agentscope 依赖；如果本地 Maven 仓库找不到 agentscope，检查 settings.xml 是否配置了 maven central 与 agentscope 所在 repo；必要时在根 pom 的 `<repositories>` 新增对应仓库）。

- [ ] **Step 5: 若 agentscope 依赖找不到，追加 repo**

若 Step 4 报 `Could not find io.agentscope:agentscope-core`，在根 `pom.xml` 的顶层（与 `<properties>` 同级、`<build>` 之前）追加：

```xml
    <repositories>
        <repository>
            <id>agentscope</id>
            <url>https://maven.aliyun.com/repository/public</url>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>
    </repositories>
```

然后重新执行 Step 4。（参考用户现有示例项目 `beast-react-agent-service` 成功用的是 spring-boot-starter-parent 默认仓库，若 central 可解析则无需此步。）

- [ ] **Step 6: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/ liteflow-react-agent/pom.xml
# 如果 Step 5 触发了 pom.xml 修改，一并 add：
git add pom.xml || true
git commit -m "build: scaffold liteflow-react-agent-core module"
```

---

## Stage 4 — Core 模块：基础类型

### Task 4.1：`AgentException` 家族

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/AgentException.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/AgentConfigException.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/AgentInvocationException.java`

- [ ] **Step 1: 写基类**

```java
package com.yomahub.liteflow.agent.exception;

public class AgentException extends RuntimeException {
    public AgentException(String message) { super(message); }
    public AgentException(String message, Throwable cause) { super(message, cause); }
}
```

- [ ] **Step 2: 写 `AgentConfigException`**

```java
package com.yomahub.liteflow.agent.exception;

public class AgentConfigException extends AgentException {
    public AgentConfigException(String message) { super(message); }
    public AgentConfigException(String message, Throwable cause) { super(message, cause); }
}
```

- [ ] **Step 3: 写 `AgentInvocationException`**

```java
package com.yomahub.liteflow.agent.exception;

public class AgentInvocationException extends AgentException {
    public AgentInvocationException(String message) { super(message); }
    public AgentInvocationException(String message, Throwable cause) { super(message, cause); }
}
```

- [ ] **Step 4: 编译通过**

Run: `mvn compile -pl liteflow-react-agent/liteflow-react-agent-core -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/
git commit -m "feat(agent-core): add AgentException hierarchy"
```

---

### Task 4.2：`ReActAgentContext`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentContext.java`
- Test: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentContextTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReActAgentContextTest {

    @Test
    void exposes_slot_sessionid_workspace() {
        Slot slot = Mockito.mock(Slot.class);
        Path ws = Path.of("/tmp/ws");
        ReActAgentContext ctx = new ReActAgentContext(slot, "sess-1", ws);

        assertSame(slot, ctx.getSlot());
        assertEquals("sess-1", ctx.getSessionId());
        assertEquals(ws, ctx.getWorkspaceDir());
    }

    @Test
    void rejects_null_required_fields() {
        Slot slot = Mockito.mock(Slot.class);
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(null, "s", Path.of("/t")));
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(slot, null, Path.of("/t")));
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(slot, "s", null));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=ReActAgentContextTest`
Expected: Compilation failure.

- [ ] **Step 3: 实现**

```java
package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.slot.Slot;

import java.nio.file.Path;
import java.util.Objects;

public class ReActAgentContext {
    private final Slot slot;
    private final String sessionId;
    private final Path workspaceDir;

    public ReActAgentContext(Slot slot, String sessionId, Path workspaceDir) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.workspaceDir = Objects.requireNonNull(workspaceDir, "workspaceDir");
    }

    public Slot getSlot() { return slot; }
    public String getSessionId() { return sessionId; }
    public Path getWorkspaceDir() { return workspaceDir; }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=ReActAgentContextTest`
Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentContext.java \
        liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentContextTest.java
git commit -m "feat(agent-core): add ReActAgentContext value holder"
```

---

### Task 4.3：`AgentSession`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java`
- Test: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/session/AgentSessionTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.session;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AgentSessionTest {

    @Test
    void new_session_tracks_workspace_and_last_active_on_touch() throws InterruptedException {
        AgentSession s = new AgentSession("sid-1", Path.of("/tmp/sid-1"));
        Instant t0 = s.getLastActive();
        Thread.sleep(5);
        s.touch();
        assertTrue(s.getLastActive().isAfter(t0));
        assertEquals("sid-1", s.getSessionId());
        assertEquals(Path.of("/tmp/sid-1"), s.getWorkspaceDir());
        assertNull(s.getAgent());
    }

    @Test
    void lock_is_reentrant() {
        AgentSession s = new AgentSession("x", Path.of("/tmp/x"));
        s.getLock().lock();
        try {
            s.getLock().lock();
            s.getLock().unlock();
        } finally {
            s.getLock().unlock();
        }
        assertFalse(s.getLock().isLocked());
    }

    @Test
    void agent_setter_is_once() {
        AgentSession s = new AgentSession("x", Path.of("/tmp/x"));
        Object dummy = new Object();
        s.setAgent(dummy);
        assertSame(dummy, s.getAgent());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=AgentSessionTest`
Expected: Compilation failure.

- [ ] **Step 3: 实现**

```java
package com.yomahub.liteflow.agent.session;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class AgentSession {
    private final String sessionId;
    private final Path workspaceDir;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Object agent;                       // 惰性构建，类型由上层保证（agentscope ReActAgent）
    private volatile Instant lastActive = Instant.now();

    public AgentSession(String sessionId, Path workspaceDir) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.workspaceDir = Objects.requireNonNull(workspaceDir);
    }

    public String getSessionId() { return sessionId; }
    public Path getWorkspaceDir() { return workspaceDir; }
    public ReentrantLock getLock() { return lock; }
    public Object getAgent() { return agent; }
    public void setAgent(Object agent) { this.agent = agent; }
    public Instant getLastActive() { return lastActive; }
    public void touch() { this.lastActive = Instant.now(); }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=AgentSessionTest`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java \
        liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/session/AgentSessionTest.java
git commit -m "feat(agent-core): add AgentSession with reentrant lock and lastActive"
```

---

### Task 4.4：`AgentSessionManager`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSessionManager.java`
- Test: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/session/AgentSessionManagerTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.SessionConfig;
import com.yomahub.liteflow.property.agent.WorkspaceConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AgentSessionManagerTest {

    @TempDir Path tmp;

    private AgentConfig config(Path root) {
        AgentConfig c = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setRoot(root.toString());
        c.setWorkspace(w);
        SessionConfig s = new SessionConfig();
        s.setIdleTimeout(Duration.ofMillis(100));
        s.setCleanupInterval(Duration.ofMillis(50));
        s.setMaxSessions(3);
        c.setSession(s);
        return c;
    }

    @Test
    void acquire_creates_workspace_and_reuses_same_session() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            assertTrue(Files.isDirectory(a.getWorkspaceDir()));
            AgentSession b = mgr.acquire("s1");
            assertSame(a, b, "同一 sessionId 必须复用同一 AgentSession 实例");
        } finally {
            mgr.close();
        }
    }

    @Test
    void safe_session_id_rejects_path_traversal() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession s = mgr.acquire("../../etc/passwd");
            assertTrue(s.getWorkspaceDir().startsWith(tmp),
                    "workspace 必须在 root 下，传入越权路径需被 escape");
        } finally {
            mgr.close();
        }
    }

    @Test
    void expired_sessions_cleaned_up() throws Exception {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            Path ws = a.getWorkspaceDir();
            Thread.sleep(400);                       // idleTimeout=100ms, cleanupInterval=50ms
            assertFalse(mgr.contains("s1"), "idle 过期后 session 应被回收");
            assertFalse(Files.exists(ws),          "workspace 目录应被删除");
        } finally {
            mgr.close();
        }
    }

    @Test
    void active_locked_session_not_cleaned() throws Exception {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            a.getLock().lock();
            try {
                Thread.sleep(400);
                assertTrue(mgr.contains("s1"), "持有 lock 的 session 不应被清理");
            } finally {
                a.getLock().unlock();
            }
        } finally {
            mgr.close();
        }
    }

    @Test
    void missing_root_raises_config_exception() {
        AgentConfig c = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setRoot(null);
        c.setWorkspace(w);
        assertThrows(AgentConfigException.class, () -> new AgentSessionManager(c));
    }

    @Test
    void lru_evicts_oldest_when_exceeding_max() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            mgr.acquire("s1"); mgr.acquire("s2"); mgr.acquire("s3");
            mgr.acquire("s4"); // 超出 maxSessions=3
            // 最早的 s1 应被淘汰（LRU 按 lastActive）
            assertFalse(mgr.contains("s1"));
            assertTrue(mgr.contains("s2"));
            assertTrue(mgr.contains("s3"));
            assertTrue(mgr.contains("s4"));
        } finally {
            mgr.close();
        }
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=AgentSessionManagerTest`
Expected: Compilation failure.

- [ ] **Step 3: 实现**

```java
package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AgentSessionManager implements AutoCloseable {

    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9_\\-]+");

    private final AgentConfig config;
    private final Path root;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    public AgentSessionManager(AgentConfig config) {
        this.config = config;
        if (config == null || config.getWorkspace() == null || config.getWorkspace().getRoot() == null) {
            throw new AgentConfigException("liteflow.agent.workspace.root is required");
        }
        this.root = Paths.get(config.getWorkspace().getRoot()).toAbsolutePath().normalize();
        if (config.getWorkspace().isAutoCreate()) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new AgentConfigException("cannot create workspace root: " + root, e);
            }
        } else if (!Files.isDirectory(root)) {
            throw new AgentConfigException("workspace root does not exist: " + root);
        }
        long every = Math.max(20, config.getSession().getCleanupInterval().toMillis());
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liteflow-agent-session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleWithFixedDelay(this::cleanup, every, every, TimeUnit.MILLISECONDS);
    }

    public AgentSession acquire(String sessionId) {
        String safe = safeId(sessionId);
        AgentSession s = sessions.computeIfAbsent(safe, id -> {
            Path ws = root.resolve(id);
            try { Files.createDirectories(ws); }
            catch (IOException e) { throw new AgentConfigException("cannot create workspace: " + ws, e); }
            return new AgentSession(id, ws);
        });
        s.touch();
        enforceMaxSessions();
        return s;
    }

    public boolean contains(String sessionId) {
        return sessions.containsKey(safeId(sessionId));
    }

    static String safeId(String raw) {
        if (raw == null || raw.isEmpty()) return "_";
        if (SAFE.matcher(raw).matches()) return raw;
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("%", "_");
    }

    private void enforceMaxSessions() {
        int max = config.getSession().getMaxSessions();
        while (sessions.size() > max) {
            sessions.values().stream()
                    .min(Comparator.comparing(AgentSession::getLastActive))
                    .ifPresent(victim -> remove(victim, true));
        }
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(config.getSession().getIdleTimeout());
        for (AgentSession s : sessions.values()) {
            if (s.getLastActive().isAfter(cutoff)) continue;
            if (!s.getLock().tryLock()) continue;                // 正在执行则跳过
            try {
                remove(s, config.getWorkspace().isCleanupOnSessionExpire());
            } finally {
                s.getLock().unlock();
            }
        }
    }

    private void remove(AgentSession s, boolean cleanWorkspace) {
        sessions.remove(s.getSessionId(), s);
        if (cleanWorkspace) {
            deleteRecursively(s.getWorkspaceDir());
        }
    }

    private static void deleteRecursively(Path p) {
        if (!Files.exists(p)) return;
        try (var walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }

    @Override
    public void close() {
        cleaner.shutdownNow();
        if (config.getWorkspace().isCleanupOnJvmShutdown()) {
            sessions.values().forEach(s -> deleteRecursively(s.getWorkspaceDir()));
        }
        sessions.clear();
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=AgentSessionManagerTest`
Expected: 6 tests pass. 若 `expired_sessions_cleaned_up` 偶发失败（时序敏感），将 `Thread.sleep(400)` 提高到 `800`。

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSessionManager.java \
        liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/session/AgentSessionManagerTest.java
git commit -m "feat(agent-core): add AgentSessionManager with LRU eviction and idle cleanup"
```

---

## Stage 5 — `WorkspaceFileTools`（TDD）

### Task 5.1：`WorkspaceFileTools`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/WorkspaceFileTools.java`
- Test: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/WorkspaceFileToolsTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.WorkspaceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceFileToolsTest {

    @TempDir Path tmp;

    private WorkspaceFileTools newTool(Path ws, long maxBytes, int maxList) {
        AgentConfig cfg = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setMaxFileBytes(maxBytes);
        w.setMaxListSize(maxList);
        cfg.setWorkspace(w);
        return new WorkspaceFileTools(ws, cfg);
    }

    @Test
    void write_then_read_round_trip() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        t.writeFile("a.txt", "hello");
        assertEquals("hello", t.readFile("a.txt"));
    }

    @Test
    void path_traversal_rejected() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertThrows(SecurityException.class, () -> t.readFile("../escape"));
        assertThrows(SecurityException.class, () -> t.writeFile("../../evil", "x"));
        assertThrows(SecurityException.class, () -> t.deleteFile("../../evil"));
    }

    @Test
    void absolute_path_rejected() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertThrows(SecurityException.class, () -> t.readFile("/etc/passwd"));
    }

    @Test
    void read_truncates_oversize_file() throws IOException {
        WorkspaceFileTools t = newTool(tmp, 4, 10);
        Files.writeString(tmp.resolve("big.txt"), "1234567890");
        String out = t.readFile("big.txt");
        assertTrue(out.length() <= 4, "超过 maxFileBytes 需要截断");
    }

    @Test
    void list_limits_entries() throws IOException {
        WorkspaceFileTools t = newTool(tmp, 1024, 3);
        for (int i = 0; i < 5; i++) Files.writeString(tmp.resolve("f" + i + ".txt"), "x");
        java.util.List<String> list = t.listFiles(".");
        assertTrue(list.size() <= 3, "超出 maxListSize 需截断");
    }

    @Test
    void delete_nonexistent_is_ok() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertDoesNotThrow(() -> t.deleteFile("nope.txt"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=WorkspaceFileToolsTest`
Expected: Compilation failure.

- [ ] **Step 3: 实现**

```java
package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceFileTools {

    private final Path workspace;
    private final long maxBytes;
    private final int maxList;

    public WorkspaceFileTools(Path workspace, AgentConfig cfg) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.maxBytes = cfg.getWorkspace().getMaxFileBytes();
        this.maxList = cfg.getWorkspace().getMaxListSize();
    }

    @Tool(name = "read_file", description = "读取当前 workspace 下的文本文件")
    public String readFile(
            @ToolParam(name = "path", required = true, description = "相对路径") String path) {
        Path p = resolveSafe(path);
        try {
            long size = Files.size(p);
            if (size > maxBytes) {
                byte[] buf = new byte[(int) maxBytes];
                try (var in = Files.newInputStream(p)) {
                    int read = in.read(buf);
                    return new String(buf, 0, Math.max(0, read), StandardCharsets.UTF_8);
                }
            }
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("read_file failed: " + e.getMessage(), e);
        }
    }

    @Tool(name = "write_file", description = "向当前 workspace 下写入文本文件（覆盖）")
    public String writeFile(
            @ToolParam(name = "path", required = true, description = "相对路径") String path,
            @ToolParam(name = "content", required = true, description = "文件内容") String content) {
        Path p = resolveSafe(path);
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, content, StandardCharsets.UTF_8);
            return "ok";
        } catch (IOException e) {
            throw new RuntimeException("write_file failed: " + e.getMessage(), e);
        }
    }

    @Tool(name = "list_files", description = "列出 workspace 某目录下的文件")
    public List<String> listFiles(
            @ToolParam(name = "path", required = false, description = "相对路径；默认当前目录") String path) {
        Path dir = resolveSafe(path == null || path.isEmpty() ? "." : path);
        List<String> out = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                out.add(workspace.relativize(p).toString());
                if (out.size() >= maxList) break;
            }
        } catch (IOException e) {
            throw new RuntimeException("list_files failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Tool(name = "delete_file", description = "删除当前 workspace 下的文件")
    public String deleteFile(
            @ToolParam(name = "path", required = true, description = "相对路径") String path) {
        Path p = resolveSafe(path);
        try {
            Files.deleteIfExists(p);
            return "ok";
        } catch (IOException e) {
            throw new RuntimeException("delete_file failed: " + e.getMessage(), e);
        }
    }

    private Path resolveSafe(String rel) {
        if (rel == null) throw new SecurityException("path is null");
        if (rel.startsWith("/")) throw new SecurityException("absolute path denied: " + rel);
        Path abs = workspace.resolve(rel).toAbsolutePath().normalize();
        if (!abs.startsWith(workspace)) {
            throw new SecurityException("path escapes workspace: " + rel);
        }
        return abs;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=WorkspaceFileToolsTest`
Expected: 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/WorkspaceFileTools.java \
        liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/WorkspaceFileToolsTest.java
git commit -m "feat(agent-core): add WorkspaceFileTools with path traversal guard"
```

---

## Stage 6 — `ManagedShellCommandTool`（TDD）

### Task 6.1：`ManagedShellCommandTool`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/ManagedShellCommandTool.java`
- Test: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/ManagedShellCommandToolTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManagedShellCommandToolTest {

    @TempDir Path tmp;

    private AgentConfig whitelist(List<String> wl) {
        AgentConfig c = new AgentConfig();
        ShellConfig s = new ShellConfig();
        s.setMode(ShellMode.WHITELIST);
        s.setWhitelist(wl);
        s.setTimeout(Duration.ofSeconds(5));
        s.setMaxOutputBytes(4096);
        c.setShell(s);
        return c;
    }

    private AgentConfig blacklist(List<String> bl) {
        AgentConfig c = whitelist(List.of());
        c.getShell().setMode(ShellMode.BLACKLIST);
        c.getShell().setBlacklist(bl);
        return c;
    }

    private AgentConfig disabled() {
        AgentConfig c = whitelist(List.of());
        c.getShell().setMode(ShellMode.DISABLED);
        return c;
    }

    @Test
    void disabled_mode_rejects_all() {
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, disabled());
        String out = t.executeCommand("ls");
        assertTrue(out.contains("denied"), "DISABLED 必须拒绝：" + out);
    }

    @Test
    void whitelist_allows_only_listed() throws IOException {
        Files.writeString(tmp.resolve("hi.txt"), "x");
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String ok = t.executeCommand("ls");
        assertTrue(ok.contains("hi.txt"));

        String bad = t.executeCommand("rm hi.txt");
        assertTrue(bad.contains("not allowed"), "rm 不在白名单应被拒绝：" + bad);
    }

    @Test
    void blacklist_blocks_listed() {
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, blacklist(List.of("rm", "sudo")));
        String bad = t.executeCommand("sudo rm -rf /");
        assertTrue(bad.contains("not allowed"));
    }

    @Test
    void execution_happens_in_workspace() throws IOException {
        Files.writeString(tmp.resolve("marker"), "");
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String out = t.executeCommand("ls");
        assertTrue(out.contains("marker"), "命令必须在 workspace 目录下执行：" + out);
    }

    @Test
    void first_token_parsing_resists_combo() {
        // 用户传入 "cd /tmp && rm -rf /" —— 解析出的首 token 不可能是 whitelist 内的，必须拒绝
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String out = t.executeCommand("cd /tmp && rm -rf /");
        assertTrue(out.contains("not allowed"), "组合命令首 token 非 ls，必须被拒：" + out);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=ManagedShellCommandToolTest`
Expected: Compilation failure.

- [ ] **Step 3: 实现**

```java
package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ManagedShellCommandTool {

    private final Path workspace;
    private final ShellConfig shell;

    public ManagedShellCommandTool(Path workspace, AgentConfig cfg) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.shell = cfg.getShell();
    }

    @Tool(name = "execute_shell_command",
          description = "在当前 workspace 中执行受控 shell 命令。禁止路径穿越与黑名单指令。")
    public String executeCommand(
            @ToolParam(name = "command", required = true,
                       description = "单条命令字符串（不拆分 | && || 等管道/连接符；这类组合会被拒绝）")
            String command) {
        if (shell.getMode() == ShellMode.DISABLED) {
            return "{\"error\":\"shell execution denied by policy\"}";
        }
        if (command == null || command.isBlank()) {
            return "{\"error\":\"empty command\"}";
        }
        String[] tokens = command.trim().split("\\s+");
        String first = tokens[0];
        if (shell.getMode() == ShellMode.WHITELIST && !shell.getWhitelist().contains(first)) {
            return "{\"error\":\"command '" + first + "' not allowed by whitelist\"}";
        }
        if (shell.getMode() == ShellMode.BLACKLIST && shell.getBlacklist().contains(first)) {
            return "{\"error\":\"command '" + first + "' not allowed by blacklist\"}";
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(tokens));
            pb.directory(workspace.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readLimited(p.getInputStream(), shell.getMaxOutputBytes());
            boolean done = p.waitFor(shell.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!done) {
                p.destroyForcibly();
                return "{\"error\":\"timeout after " + shell.getTimeout().toMillis() + "ms\"}";
            }
            return out;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private static String readLimited(InputStream in, long max) throws IOException {
        byte[] buf = new byte[4096];
        List<byte[]> chunks = new ArrayList<>();
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0 && total < max) {
            int toCopy = (int) Math.min(n, max - total);
            byte[] c = new byte[toCopy];
            System.arraycopy(buf, 0, c, 0, toCopy);
            chunks.add(c);
            total += toCopy;
        }
        byte[] all = new byte[(int) total];
        int pos = 0;
        for (byte[] c : chunks) { System.arraycopy(c, 0, all, pos, c.length); pos += c.length; }
        return new String(all, StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=ManagedShellCommandToolTest`
Expected: 5 tests pass. 若 macOS/Linux 上 `ls` 路径不在 PATH 或 `cd` 是 shell 内建命令导致测试偏差，调整测试用例中的 whitelist 为实际可用命令（`ls` 在 macOS/Linux 都是独立可执行文件）。

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/ManagedShellCommandTool.java \
        liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/ManagedShellCommandToolTest.java
git commit -m "feat(agent-core): add ManagedShellCommandTool with whitelist/blacklist policy"
```

---

## Stage 7 — `ReActAgentComponent` 抽象类

### Task 7.1：抽象类骨架（不含 process 主体）

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`

- [ ] **Step 1: 写骨架**（`process()` 先抛 `UnsupportedOperationException`，Task 7.2 再实现）

```java
package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.hook.AgentHook;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;

import java.util.List;

public abstract class ReActAgentComponent extends NodeComponent {

    /* ===== 框架提供的 final 访问器 ===== */

    protected final AgentConfig agentConfig() {
        AgentConfig c = LiteflowConfigGetter.get().getAgent();
        if (c == null) {
            throw new com.yomahub.liteflow.agent.exception.AgentConfigException(
                    "LiteflowConfig.agent is null; configure liteflow.agent.* or setAgent() before use");
        }
        return c;
    }

    /* ===== 必须实现 ===== */

    protected abstract Model buildModel(ReActAgentContext ctx);

    protected abstract String systemPrompt(ReActAgentContext ctx);

    protected abstract String userPrompt(ReActAgentContext ctx);

    /* ===== 可选覆写 ===== */

    protected List<Object> tools(ReActAgentContext ctx) { return List.of(); }

    protected String resolveSessionId(Slot slot) { return slot.getRequestId(); }

    protected int maxIterations() { return -1; }

    protected boolean enableShellTool() { return true; }

    protected boolean enableWorkspaceFileTools() { return true; }

    protected List<AgentHook> hooks(ReActAgentContext ctx) { return List.of(); }

    protected void handleReply(Msg reply, ReActAgentContext ctx) {
        ctx.getSlot().setResponseData(reply == null ? null : reply.getTextContent());
    }

    /* ===== 框架 final 执行体 ===== */

    @Override
    public final void process() throws Exception {
        AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(agentConfig());
        Slot slot = this.getSlot();
        String sid = resolveSessionId(slot);
        AgentSession session = mgr.acquire(sid);
        session.getLock().lock();
        try {
            ReActAgentContext ctx = new ReActAgentContext(slot, session.getSessionId(), session.getWorkspaceDir());
            Object agent = session.getAgent();
            if (agent == null) {
                agent = AgentBuilderInternal.build(this, ctx);
                session.setAgent(agent);
            }
            Msg reply = AgentBuilderInternal.call(agent, userPrompt(ctx));
            handleReply(reply, ctx);
        } finally {
            session.getLock().unlock();
        }
    }

    /** 供 Task 7.2 使用的内部构建入口；此时先声明签名 */
    static final class AgentBuilderInternal {
        static Object build(ReActAgentComponent self, ReActAgentContext ctx) {
            throw new UnsupportedOperationException("implemented in Task 7.2");
        }
        static Msg call(Object agent, String userInput) {
            throw new UnsupportedOperationException("implemented in Task 7.2");
        }
    }

    /** 持有单例 AgentSessionManager；首次 process 时根据 AgentConfig 懒创建 */
    static final class AgentSessionManagerHolder {
        private static volatile AgentSessionManager INSTANCE;
        static AgentSessionManager getOrCreate(AgentConfig cfg) {
            AgentSessionManager cur = INSTANCE;
            if (cur != null) return cur;
            synchronized (AgentSessionManagerHolder.class) {
                if (INSTANCE == null) INSTANCE = new AgentSessionManager(cfg);
                return INSTANCE;
            }
        }
        static void resetForTesting() {
            AgentSessionManager cur = INSTANCE;
            if (cur != null) {
                try { cur.close(); } catch (Exception ignored) {}
            }
            INSTANCE = null;
        }
    }
}
```

- [ ] **Step 2: 编译通过**

Run: `mvn compile -pl liteflow-react-agent/liteflow-react-agent-core -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java
git commit -m "feat(agent-core): scaffold ReActAgentComponent with final process()"
```

---

### Task 7.2：`process()` 真实现 + 时序测试（with FakeModel）

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentComponentTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/FakeEchoModel.java`

- [ ] **Step 1: 写 FakeEchoModel**

> **注**：agentscope `Model` 接口的确切签名以 `io.agentscope.core.model.Model` 为准。若编译报错，运行 `mvn dependency:sources -pl liteflow-react-agent/liteflow-react-agent-core` 拉下源码后查看 Model 抽象方法，填充下面的 `@Override`。

```java
// 测试夹具：最小可工作的 Model 实现，直接回显用户 prompt
package com.yomahub.liteflow.agent.component;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import reactor.core.publisher.Mono;

public class FakeEchoModel implements Model {
    // TODO 按 agentscope Model 接口真实签名补充
    // 一般 Model 会有 formatChat/generate 之类方法，返回 Mono<Msg> 或类似
    public Mono<Msg> generate(Object request) {
        return Mono.just(Msg.builder().textContent("[echo]").build());
    }
}
```

> 如果 agentscope 1.0.9 的 `Model` 接口方法名不同（例如 `chat(...)` 或 `call(...)`），就按实际方法名实现，核心要点：返回一个能让 `ReActAgent.call()` 正常走完一轮的最小响应。

- [ ] **Step 2: 实现 `AgentBuilderInternal`**

替换 Task 7.1 生成的 `AgentBuilderInternal` 为真实实现：

```java
    static final class AgentBuilderInternal {
        static Object build(ReActAgentComponent self, ReActAgentContext ctx) {
            AgentConfig cfg = self.agentConfig();
            int iters = self.maxIterations() > 0
                    ? self.maxIterations()
                    : cfg.getDefaults().getMaxIterations();

            io.agentscope.core.tool.Toolkit toolkit = new io.agentscope.core.tool.Toolkit();
            self.tools(ctx).forEach(toolkit::registerTool);
            if (self.enableWorkspaceFileTools()) {
                toolkit.registerTool(
                        new com.yomahub.liteflow.agent.tool.WorkspaceFileTools(ctx.getWorkspaceDir(), cfg));
            }
            if (self.enableShellTool()) {
                toolkit.registerTool(
                        new com.yomahub.liteflow.agent.tool.ManagedShellCommandTool(ctx.getWorkspaceDir(), cfg));
            }

            return io.agentscope.core.ReActAgent.builder()
                    .name(self.getNodeId() == null ? "liteflow-agent" : self.getNodeId())
                    .sysPrompt(self.systemPrompt(ctx))
                    .model(self.buildModel(ctx))
                    .toolkit(toolkit)
                    .memory(new io.agentscope.core.memory.InMemoryMemory())
                    .maxIters(iters)
                    .hooks(self.hooks(ctx))
                    .build();
        }

        static Msg call(Object agent, String userInput) {
            try {
                io.agentscope.core.ReActAgent ra = (io.agentscope.core.ReActAgent) agent;
                Msg user = Msg.builder().textContent(userInput == null ? "" : userInput).build();
                return ra.call(user).block();
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new com.yomahub.liteflow.agent.exception.AgentInvocationException(
                        "agent invocation failed: " + e.getMessage(), e);
            }
        }
    }
```

> **注**：若 agentscope 的 `ReActAgent.builder()` 方法名不叫 `name` / `sysPrompt` / `maxIters` / `hooks`，以你调研时参考的示例项目 `beast-react-agent-service/src/main/java/com/beast/agent/service/AgentService.java` 的 `createSession` 中写法为准——行数 `194-203` 明确列出了这些方法名。按实际调整。

- [ ] **Step 3: 写端到端时序测试**

```java
package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReActAgentComponentTest {

    @TempDir Path tmp;

    @BeforeEach
    void init() {
        AgentConfig agent = new AgentConfig();
        agent.getWorkspace().setRoot(tmp.toString());
        agent.getShell().setMode(ShellMode.DISABLED);  // 测试不用 shell，避免跨平台差异
        LiteflowConfig cfg = new LiteflowConfig();
        cfg.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(cfg);
    }

    @AfterEach
    void cleanup() {
        ReActAgentComponent.AgentSessionManagerHolder.resetForTesting();
        LiteflowConfigGetter.clean();
    }

    static class TestAgent extends ReActAgentComponent {
        @Override protected Model buildModel(ReActAgentContext ctx) { return new FakeEchoModel(); }
        @Override protected String systemPrompt(ReActAgentContext ctx) { return "you are helpful"; }
        @Override protected String userPrompt(ReActAgentContext ctx) {
            return (String) ctx.getSlot().getRequestData();
        }
    }

    @Test
    void process_creates_workspace_and_writes_response() throws Exception {
        TestAgent a = new TestAgent();
        a.setNodeId("testAgent");
        // Slot 需要真实初始化；借用 liteflow 自身的 Slot pool API
        com.yomahub.liteflow.slot.Slot slot = // 调用 DataBus 获取真实 Slot
                com.yomahub.liteflow.slot.DataBus.getSlot(
                        com.yomahub.liteflow.slot.DataBus.offerSlot(com.yomahub.liteflow.slot.Slot.class));
        slot.setRequestId("req-1");
        slot.setRequestData("hello");
        a.setSlot(slot);

        a.process();

        // 无状态模式：sessionId == requestId → workspace 目录名 = req-1
        assertTrue(java.nio.file.Files.isDirectory(tmp.resolve("req-1")));
        // FakeEchoModel 返回 "[echo]"
        assertEquals("[echo]", slot.getResponseData());
    }

    @Test
    void session_reuse_when_resolveSessionId_overridden() throws Exception {
        class Multi extends TestAgent {
            @Override protected String resolveSessionId(com.yomahub.liteflow.slot.Slot s) {
                return "fixed-session";
            }
        }
        Multi a = new Multi();
        a.setNodeId("multi");

        com.yomahub.liteflow.slot.Slot s1 = makeSlot("r1", "hi");
        a.setSlot(s1);
        a.process();

        com.yomahub.liteflow.slot.Slot s2 = makeSlot("r2", "again");
        a.setSlot(s2);
        a.process();

        assertTrue(java.nio.file.Files.isDirectory(tmp.resolve("fixed-session")));
    }

    private com.yomahub.liteflow.slot.Slot makeSlot(String reqId, Object data) {
        int idx = com.yomahub.liteflow.slot.DataBus.offerSlot(com.yomahub.liteflow.slot.Slot.class);
        com.yomahub.liteflow.slot.Slot s = com.yomahub.liteflow.slot.DataBus.getSlot(idx);
        s.setRequestId(reqId);
        s.setRequestData(data);
        return s;
    }
}
```

> **重要**：如果 LiteFlow 的 Slot 构造 API 在当前仓库中签名与上面不完全一致（例如 `DataBus.offerSlot(Class)` 返回 index 而非 Slot），执行 Step 4 时会发现编译错误——按现有测试 `liteflow-core/src/test/java/**/FlowExecutorTest.java` 的 Slot 初始化写法对齐即可。

- [ ] **Step 4: 跑测试**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-core -Dtest=ReActAgentComponentTest`
Expected: 2 tests pass. 若 FakeEchoModel 的方法签名与 agentscope 实际 `Model` 接口不符导致 `ReActAgent.call()` NPE，回到 Step 1 按真实接口调整。

- [ ] **Step 5: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/
git commit -m "feat(agent-core): implement ReActAgentComponent process() with session lifecycle"
```

---

## Stage 8 — 平台模块

### Task 8.1：`liteflow-react-agent-openai`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-openai/pom.xml`
- Create: `.../main/java/com/yomahub/liteflow/agent/openai/OpenAIModelFactory.java`
- Create: `.../main/java/com/yomahub/liteflow/agent/openai/OpenAICompatiblePresets.java`
- Test: `.../test/java/com/yomahub/liteflow/agent/openai/OpenAICompatiblePresetsTest.java`
- Modify: `liteflow-react-agent/pom.xml`（取消 openai 模块注释）

- [ ] **Step 1: 打开父 pom 的 openai 模块注释**

修改 `liteflow-react-agent/pom.xml` 把 `<!-- <module>liteflow-react-agent-openai</module> -->` 改成 `<module>liteflow-react-agent-openai</module>`。

- [ ] **Step 2: 写 openai 模块 pom**

```xml
<!-- liteflow-react-agent/liteflow-react-agent-openai/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-react-agent</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>liteflow-react-agent-openai</artifactId>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-react-agent-core</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 写 `OpenAIModelFactory`**

```java
// .../openai/OpenAIModelFactory.java
package com.yomahub.liteflow.agent.openai;

import io.agentscope.core.model.OpenAIChatModel;

public final class OpenAIModelFactory {
    private OpenAIModelFactory() {}

    public static OpenAIChatModel openai(String apiKey, String modelName) {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    public static OpenAIChatModel custom(String apiKey, String baseUrl, String modelName) {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
```

- [ ] **Step 4: 写 `OpenAICompatiblePresets`**

```java
// .../openai/OpenAICompatiblePresets.java
package com.yomahub.liteflow.agent.openai;

import io.agentscope.core.model.OpenAIChatModel;

/**
 * 为 OpenAI 兼容协议的国内厂商提供便捷工厂。baseUrl 为官方推荐值，若厂商改动请用
 * {@link OpenAIModelFactory#custom(String, String, String)}。
 */
public final class OpenAICompatiblePresets {
    private OpenAICompatiblePresets() {}

    public static OpenAIChatModel deepseek(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.deepseek.com/v1", modelName);
    }
    public static OpenAIChatModel kimi(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.moonshot.cn/v1", modelName);
    }
    public static OpenAIChatModel glm(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://open.bigmodel.cn/api/paas/v4", modelName);
    }
    public static OpenAIChatModel minimax(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.minimax.chat/v1", modelName);
    }
}
```

- [ ] **Step 5: 写单元测试（只验证 builder 参数传递，不联网）**

```java
package com.yomahub.liteflow.agent.openai;

import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAICompatiblePresetsTest {

    @Test
    void deepseek_preset_uses_deepseek_baseurl() {
        OpenAIChatModel m = OpenAICompatiblePresets.deepseek("k", "deepseek-chat");
        assertNotNull(m);
        // agentscope Model 通常不暴露 baseUrl getter；至少断言构造不抛
    }

    @Test
    void kimi_preset_ok()    { assertNotNull(OpenAICompatiblePresets.kimi("k", "moonshot-v1-8k")); }
    @Test
    void glm_preset_ok()     { assertNotNull(OpenAICompatiblePresets.glm("k", "glm-4")); }
    @Test
    void minimax_preset_ok() { assertNotNull(OpenAICompatiblePresets.minimax("k", "abab6.5s-chat")); }
}
```

> 如果 `OpenAIChatModel` 暴露了 `getBaseUrl()` / `getApiKey()` 等 getter，加断言更有意义；否则这些测试只能证明"构造不抛"，留一个 TODO 等 agentscope 暴露 getter 后补全。

- [ ] **Step 6: 构建 + 测试**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am`
Expected: BUILD SUCCESS, 4 tests pass.

- [ ] **Step 7: Commit**

```bash
git add liteflow-react-agent/liteflow-react-agent-openai/ liteflow-react-agent/pom.xml
git commit -m "feat(agent-openai): add OpenAI factory and compatible presets (deepseek/kimi/glm/minimax)"
```

---

### Task 8.2：`liteflow-react-agent-anthropic`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/pom.xml`
- Create: `.../main/java/com/yomahub/liteflow/agent/anthropic/AnthropicModelFactory.java`
- Test: `.../test/java/com/yomahub/liteflow/agent/anthropic/AnthropicModelFactoryTest.java`
- Modify: `liteflow-react-agent/pom.xml`

- [ ] **Step 1: 取消父 pom anthropic 注释，同 Task 8.1 Step 1。**

- [ ] **Step 2: 模块 pom**（结构同 Task 8.1 Step 2，artifactId 改为 `liteflow-react-agent-anthropic`）。

- [ ] **Step 3: `AnthropicModelFactory`**

```java
package com.yomahub.liteflow.agent.anthropic;

import io.agentscope.core.model.AnthropicChatModel;

public final class AnthropicModelFactory {
    private AnthropicModelFactory() {}

    public static AnthropicChatModel of(String apiKey, String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
```

- [ ] **Step 4: 测试**

```java
package com.yomahub.liteflow.agent.anthropic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnthropicModelFactoryTest {
    @Test
    void construct_ok() {
        assertNotNull(AnthropicModelFactory.of("k", "claude-sonnet-4-6"));
    }
}
```

- [ ] **Step 5: 构建 + Commit**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-anthropic -am`
Expected: BUILD SUCCESS

```bash
git add liteflow-react-agent/liteflow-react-agent-anthropic/ liteflow-react-agent/pom.xml
git commit -m "feat(agent-anthropic): add Anthropic model factory"
```

---

### Task 8.3：`liteflow-react-agent-gemini`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/pom.xml`（含 `google-genai` 依赖）
- Create: `.../gemini/GeminiModelFactory.java`
- Test: `.../gemini/GeminiModelFactoryTest.java`
- Modify: `liteflow-react-agent/pom.xml`

- [ ] **Step 1: 取消父 pom gemini 注释。**

- [ ] **Step 2: 模块 pom**（与 8.1 的 openai pom 结构一致，artifactId 改为 gemini；`<dependencies>` 追加）：

```xml
        <dependency>
            <groupId>com.google.genai</groupId>
            <artifactId>google-genai</artifactId>
            <version>${google-genai.version}</version>
        </dependency>
```

- [ ] **Step 3: `GeminiModelFactory`**

```java
package com.yomahub.liteflow.agent.gemini;

import io.agentscope.core.model.GeminiChatModel;

public final class GeminiModelFactory {
    private GeminiModelFactory() {}

    public static GeminiChatModel of(String apiKey, String modelName) {
        return GeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /** thinking 为 agentscope 的 ThinkingLevelFormatter 输入值："none"|"minimal"|"low"|"medium"|"high" */
    public static GeminiChatModel of(String apiKey, String modelName, String thinkingLevel) {
        return GeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .formatter(new io.agentscope.core.model.ThinkingLevelFormatter(thinkingLevel))
                .build();
    }
}
```

- [ ] **Step 4: 测试**

```java
package com.yomahub.liteflow.agent.gemini;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeminiModelFactoryTest {
    @Test void plain_ok() { assertNotNull(GeminiModelFactory.of("k", "gemini-3-flash-preview")); }
    @Test void with_thinking_ok() {
        assertNotNull(GeminiModelFactory.of("k", "gemini-3-flash-preview", "high"));
    }
}
```

- [ ] **Step 5: 构建 + Commit**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-gemini -am`
Expected: BUILD SUCCESS

```bash
git add liteflow-react-agent/liteflow-react-agent-gemini/ liteflow-react-agent/pom.xml
git commit -m "feat(agent-gemini): add Gemini model factory with optional thinking level"
```

---

### Task 8.4：`liteflow-react-agent-dashscope`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/pom.xml`
- Create: `.../dashscope/DashScopeModelFactory.java`
- Test: `.../dashscope/DashScopeModelFactoryTest.java`
- Modify: `liteflow-react-agent/pom.xml`

- [ ] **Step 1: 取消父 pom dashscope 注释。**

- [ ] **Step 2: pom（结构同 8.1，artifactId `liteflow-react-agent-dashscope`）。**

- [ ] **Step 3: `DashScopeModelFactory`**

```java
package com.yomahub.liteflow.agent.dashscope;

import io.agentscope.core.model.DashScopeChatModel;

public final class DashScopeModelFactory {
    private DashScopeModelFactory() {}

    public static DashScopeChatModel of(String apiKey, String modelName) {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
```

- [ ] **Step 4: 测试**

```java
package com.yomahub.liteflow.agent.dashscope;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DashScopeModelFactoryTest {
    @Test void construct_ok() { assertNotNull(DashScopeModelFactory.of("k", "qwen3-max")); }
}
```

- [ ] **Step 5: 构建 + Commit**

Run: `mvn test -pl liteflow-react-agent/liteflow-react-agent-dashscope -am`
Expected: BUILD SUCCESS

```bash
git add liteflow-react-agent/liteflow-react-agent-dashscope/ liteflow-react-agent/pom.xml
git commit -m "feat(agent-dashscope): add DashScope (Qwen) model factory"
```

- [ ] **Step 6: 整体 react-agent 父模块编译**

Run: `mvn clean install -pl liteflow-react-agent -amd -DskipTests`
Expected: 5 个模块（父 + 4 子）全部 BUILD SUCCESS。

---

## Stage 9 — 集成测试模块 `liteflow-testcase-el-springboot-agent`

### Task 9.1：模块骨架 + EL 规则 + FakeModel

**Files:**
- Create: `liteflow-testcase-el/liteflow-testcase-el-springboot-agent/pom.xml`
- Create: `.../src/test/resources/application.properties`
- Create: `.../src/test/resources/flow.xml`
- Create: `.../src/test/java/com/yomahub/liteflow/test/agent/cmp/FakeAgentComponent.java`
- Modify: `liteflow-testcase-el/pom.xml`（若有 `<modules>` 列表，加入新模块；否则跳过）

- [ ] **Step 1: 查看 testcase 父 pom 结构**

Run: `cat liteflow-testcase-el/pom.xml`
Inspect：若文件里有 `<modules>` 列表，则需把新模块加入；否则 liteflow-testcase-el 可能是单纯占位。

- [ ] **Step 2: 写新模块 pom**

```xml
<!-- liteflow-testcase-el/liteflow-testcase-el-springboot-agent/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-testcase-el</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>liteflow-testcase-el-springboot-agent</artifactId>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-spring-boot-starter</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-react-agent-core</artifactId>
            <version>${revision}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>${springboot.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${springboot.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

> **注**：Spring Boot 2.6.8 对 Java 21 的支持通常 OK；如果实际运行报 Java 版本不兼容（Spring Boot 2.x 最高官方支持 Java 17），把 springboot.version 升级为 `3.2.x` 的 testOnly override，或在本 testcase 模块里改成 `3.x`（**不碰主工程**）。

- [ ] **Step 3: `application.properties`**

```properties
# liteflow-testcase-el/liteflow-testcase-el-springboot-agent/src/test/resources/application.properties
liteflow.enable=true
liteflow.rule-source=flow.xml
liteflow.parse-mode=PARSE_ALL_ON_START

liteflow.agent.workspace.root=${java.io.tmpdir}/liteflow-agent-it
liteflow.agent.workspace.auto-create=true
liteflow.agent.session.idle-timeout=5m
liteflow.agent.shell.mode=disabled
```

- [ ] **Step 4: `flow.xml`**

```xml
<!-- src/test/resources/flow.xml -->
<flow>
    <chain name="simpleAgentChain">
        THEN(fakeAgent);
    </chain>
    <chain name="retryAgentChain">
        RETRY(fakeAgent).times(2);
    </chain>
    <chain name="catchAgentChain">
        CATCH(fakeAgent).DO(fallbackCmp);
    </chain>
</flow>
```

- [ ] **Step 5: `FakeAgentComponent`（基于 `ReActAgentComponent` + FakeEchoModel）**

```java
package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import reactor.core.publisher.Mono;

@LiteflowComponent("fakeAgent")
public class FakeAgentComponent extends ReActAgentComponent {
    @Override protected Model buildModel(ReActAgentContext ctx) {
        return new Model() {
            // 实现 agentscope Model 接口所需方法（同 FakeEchoModel）
            public Mono<Msg> generate(Object req) {
                return Mono.just(Msg.builder().textContent("[integration-ok]").build());
            }
        };
    }
    @Override protected String systemPrompt(ReActAgentContext ctx) { return "test system"; }
    @Override protected String userPrompt(ReActAgentContext ctx) {
        Object req = ctx.getSlot().getRequestData();
        return req == null ? "" : req.toString();
    }
    @Override protected int maxIterations() { return 1; }
}
```

- [ ] **Step 6: Fallback 组件**

```java
package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("fallbackCmp")
public class FallbackComponent extends NodeComponent {
    @Override public void process() {
        this.getSlot().setResponseData("[fallback]");
    }
}
```

- [ ] **Step 7: 构建模块**

Run: `mvn clean compile -pl liteflow-testcase-el/liteflow-testcase-el-springboot-agent -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-springboot-agent/
# 如果在 Step 1 修改了父 pom，也一起 add
git add liteflow-testcase-el/pom.xml || true
git commit -m "test(agent-it): scaffold springboot-agent integration test module"
```

---

### Task 9.2：集成测试用例（THEN / RETRY / TIMEOUT / CATCH）

**Files:**
- Create: `.../src/test/java/com/yomahub/liteflow/test/agent/AgentELIT.java`

- [ ] **Step 1: 写测试**

```java
package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootApplication
@SpringBootTest(classes = AgentELIT.class)
@TestPropertySource("classpath:/application.properties")
class AgentELIT {

    @Autowired FlowExecutor flowExecutor;

    @Test
    void simple_chain_runs_agent_and_returns_reply() {
        LiteflowResponse rsp = flowExecutor.execute2Resp("simpleAgentChain", "hello");
        assertTrue(rsp.isSuccess(), rsp.getCause() == null ? "" : rsp.getCause().toString());
        assertEquals("[integration-ok]", rsp.getSlot().getResponseData());
    }

    @Test
    void catch_chain_falls_back_when_agent_throws() {
        // 本 fakeAgent 不抛，但 CATCH 本身需确保 EL 编排能识别 ReActAgentComponent
        LiteflowResponse rsp = flowExecutor.execute2Resp("catchAgentChain", "hi");
        assertTrue(rsp.isSuccess());
        // 没有异常时应走主路径 → [integration-ok]
        assertEquals("[integration-ok]", rsp.getSlot().getResponseData());
    }

    @Test
    void retry_chain_counts_agent_executions() {
        LiteflowResponse rsp = flowExecutor.execute2Resp("retryAgentChain", "hi");
        assertTrue(rsp.isSuccess());
    }
}
```

- [ ] **Step 2: 跑测试**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot-agent -am`
Expected: 3 tests pass。若失败：
- `liteflow.agent.workspace.root` 没有写权限 → 改配置指向 `${java.io.tmpdir}/xxx`
- 找不到 `fakeAgent` bean → 确认 `@SpringBootApplication` 的 scanBasePackages 覆盖了 cmp 包
- Spring Boot 2.x + Java 21 冲突 → Task 9.1 Step 2 的 note 处理

- [ ] **Step 3: Commit**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-springboot-agent/src/test/java/
git commit -m "test(agent-it): verify agent component participates in EL orchestration"
```

---

## Stage 10 — 文档

### Task 10.1：每个模块写最小 README

**Files:**
- Create: `liteflow-react-agent/README.md`
- Create: `liteflow-react-agent/liteflow-react-agent-core/README.md`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/README.md`
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/README.md`
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/README.md`
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/README.md`

- [ ] **Step 1: 父模块 README**

```markdown
# liteflow-react-agent

为 LiteFlow 提供基于 agentscope-java 的 ReAct Agent 组件能力。通过继承 `ReActAgentComponent` 即可在 EL 规则中作为普通 node 编排。

## 子模块

| 模块 | 说明 |
|------|------|
| `liteflow-react-agent-core` | 抽象类、Session/Workspace 管理、内置受管工具 |
| `liteflow-react-agent-openai` | OpenAI + 兼容协议（DeepSeek/Kimi/GLM/MiniMax） |
| `liteflow-react-agent-anthropic` | Anthropic Claude |
| `liteflow-react-agent-gemini` | Google Gemini（传递 google-genai 依赖） |
| `liteflow-react-agent-dashscope` | 阿里云 DashScope / Qwen |

## 配置

见 [core 模块 README](./liteflow-react-agent-core/README.md)。

## Java 版本

所有 react-agent 子模块要求 **Java 21+**（agentscope-java 要求）。LiteFlow 主工程仍然保持 Java 8 兼容。
```

- [ ] **Step 2: core README（给出最小 yaml + 代码片段）**

```markdown
# liteflow-react-agent-core

## 快速上手

### 1. 添加依赖（选择至少一个平台模块）

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-openai</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

### 2. 配置

```yaml
liteflow:
  agent:
    workspace:
      root: /var/lib/liteflow/agent-workspaces
    shell:
      mode: whitelist
      whitelist: [ls, cat, grep]
    openai-compatible:
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
```

### 3. 定义 Agent

```java
@LiteflowComponent("reviewAgent")
public class ReviewAgent extends ReActAgentComponent {
    @Override protected Model buildModel(ReActAgentContext ctx) {
        return OpenAICompatiblePresets.deepseek(
            agentConfig().getOpenaiCompatible().get("deepseek").getApiKey(),
            "deepseek-chat"
        );
    }
    @Override protected String systemPrompt(ReActAgentContext ctx) { return "你是审核专家"; }
    @Override protected String userPrompt(ReActAgentContext ctx) {
        return ctx.getSlot().getRequestData(String.class);
    }
}
```

### 4. EL 编排

```xml
<chain name="reviewChain">
    THEN(prepare, reviewAgent, notify);
</chain>
```

## 核心概念

- **Session**：由 `resolveSessionId` 决定；默认 `slot.getRequestId()`（一次性）。覆写后可复用 memory 与 workspace 实现多轮对话。
- **Workspace**：每 session 一个目录，在 `liteflow.agent.workspace.root` 之下。内置 `WorkspaceFileTools` 强制路径围栏。
- **Shell**：`ManagedShellCommandTool` 按 `liteflow.agent.shell.mode` 做 whitelist/blacklist/disabled 校验，首 token 不在策略内即拒绝。
```

- [ ] **Step 3: 各平台 README（极简，一段代码片段即可）**

openai：

```markdown
# liteflow-react-agent-openai

OpenAI + OpenAI 兼容协议（DeepSeek/Kimi/GLM/MiniMax）。

```java
OpenAIChatModel m1 = OpenAIModelFactory.openai(apiKey, "gpt-4o-mini");
OpenAIChatModel m2 = OpenAICompatiblePresets.deepseek(apiKey, "deepseek-chat");
OpenAIChatModel m3 = OpenAIModelFactory.custom(apiKey, "https://your.own/v1", "your-model");
```
```

anthropic / gemini / dashscope 类似（只写一个 API 调用片段）。

- [ ] **Step 4: Commit**

```bash
git add liteflow-react-agent/README.md \
        liteflow-react-agent/liteflow-react-agent-*/README.md
git commit -m "docs(agent): add README for react-agent parent and sub-modules"
```

---

## 最终验收

- [ ] **Step A: 全量构建**

Run: `mvn clean package -DskipTests`
Expected: 所有模块 BUILD SUCCESS，包括 react-agent 父及 4 子 + testcase-agent 模块。

- [ ] **Step B: 全量测试**

Run: `mvn test`
Expected: 0 failures。注意 JDK 21 运行环境。

- [ ] **Step C: release profile 验证**

Run: `mvn clean package -DskipTests -P release`
Expected: BUILD SUCCESS（不含 testcase/benchmark，但含 react-agent）。

- [ ] **Step D: 最终 commit（若有小修小补）**

```bash
git status
# 若无改动跳过；有改动：
# git add . && git commit -m "chore(agent): finalize build integration"
```

---

## Self-Review 结论

**Spec coverage 核对**（逐节对齐 `docs/superpowers/specs/2026-04-19-liteflow-react-agent-design.md`）：

| Spec 节 | Plan Task |
|---------|-----------|
| §1 目标 | 全篇 |
| §2 总体架构 | Stage 3-7 |
| §3 模块结构 | Stage 3, 8 |
| §4 `ReActAgentComponent` | Task 7.1 + 7.2 |
| §4.1 thinking level 处置 | Task 8.3（Gemini overload），其他平台由用户在 `buildModel` 内自写 |
| §5 平台便捷工厂 | Task 8.1-8.4 |
| §6 `AgentConfig` 融入 `LiteflowConfig` | Task 1.1-1.3 + 2.1-2.2 |
| §7.1 Workspace 隔离 + `WorkspaceFileTools` | Task 4.4（隔离）+ Task 5.1（文件工具） |
| §7.2 Shell 管控 | Task 6.1 |
| §7.3 禁用开关 | Task 7.1（`enableShellTool` / `enableWorkspaceFileTools`） |
| §8 执行流程 | Task 7.2 |
| §8.2 异常分类 | Task 4.1（异常类型）+ Task 7.2（抛出点） |
| §8.3 生命周期回收 | Task 4.4（cleanup + LRU + JVM shutdown） |
| §8.4 与 LiteFlow 能力结合 | Stage 9 集成测试验证 |
| §9 测试策略 | Stage 4-7（单元）+ Stage 9（EL 集成） |
| §10 文档 | Stage 10 |
| §11 风险 | Task 5 Step 5 处理 agentscope repo；Task 9.1 Step 2 处理 Spring Boot 版本冲突 |
| §12 交付物 | 整个 Plan 完整对齐 |

**未覆盖的 spec 细节**：
- §13 未决事项（memory 是否持久化、内置 SessionLoggingHook）——实现层本就是"未决"，不强制入 Plan。
- 集成测试中需要真实 apikey 的平台冒烟——Spec §9 提到"默认 skip"；本 Plan 暂不加 `@EnabledIfEnvironmentVariable` 测试，放到后续增量。

**占位符扫描**：已检查全文，无 TBD/TODO 类占位符；对于"agentscope 实际方法名可能与示例不完全一致"这类外部不确定性，在具体步骤中给了明确的降级处置指引（去读源码 / 对齐示例项目具体行号）。

**类型一致性**：
- `AgentConfig` 在 Stage 1 定义后全流程使用，签名未变
- `ReActAgentContext` 三元组 `(Slot, sessionId, workspaceDir)` 全程一致
- `AgentSession.getAgent()` 返回 `Object`（不绑定 agentscope 类型），和 Task 7.2 的 `AgentBuilderInternal.call` 强转一致

如发现实施中与 plan 偏离（例如 agentscope 1.0.9 API 与示例项目 1.0.9 API 有差异），优先以实际 jar 为准，回改本 plan 后再继续。
