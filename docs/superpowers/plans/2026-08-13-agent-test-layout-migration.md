# AgentScope 2 测试集中迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 AgentScope 2 升级新增的 63 个测试源码与夹具全部迁入 `liteflow-testcase-el` 大模块，同时保持原 package、测试语义、离线边界和依赖隔离。

**Architecture:** `liteflow-testcase-el-react-agent-core` 集中承载无容器的基础配置、Core 与 Solon ServiceLoader 隔离测试；`liteflow-testcase-el-react-agent-harness` 集中承载无容器 Harness 测试；现有 `liteflow-testcase-el-react-agent` 承载 Provider、A2A 与 Spring 纵切。Spring Boot 3、Spring Boot 4 的绑定测试进入各自 testcase 子模块。迁移只改变测试物理位置和测试 classpath，不扩大生产 API，也不改变生产依赖；最终用永久结构契约阻止测试重新进入生产模块。

**Tech Stack:** Java 17、JUnit 5、Maven Surefire／Failsafe、Spring Boot 3／4、Solon、Reactor Test、Mockito、AgentScope 2.0.2。

## Global Constraints

- 本轮只迁移提交 `2655a16b13d7fd95c48575c979cb5b3f45b84b7b` 之后由 AgentScope 2 升级新增的 63 个测试源码与夹具。
- 原 Agent Core 的 30 个文件与 `AgentConfigV2Test` 进入无容器 `liteflow-testcase-el-react-agent-core`；Harness 的 15 个文件进入无容器 `liteflow-testcase-el-react-agent-harness`；Provider 与 A2A 的 14 个文件进入现有 `liteflow-testcase-el-react-agent`。
- Spring Boot 3、Spring Boot 4 的绑定测试分别进入 `liteflow-testcase-el-springboot`、`liteflow-testcase-el-springboot4`；要求 `Solon.context() == null` 的 ServiceLoader 隔离测试进入无容器 Core testcase 子模块。
- 所有迁移文件保留原 Java package；不得为测试增加 public API。
- 生产模块迁移后不得保留 `src/test` 文件；只删除确认仅服务于迁出测试的测试依赖。
- 默认测试必须离线；不得调用真实 Provider、Docker daemon、网络或真实 A2A transport。
- `agent-live` 与 `agent-docker-it` 的显式发现边界保持不变；不得新增 skip、assumption 或测试排除来制造通过。
- AgentScope 依赖全部保持 2.0.2；不得引入 `io.agentscope:agentscope` aggregate、1.x、Provider→Core 反向依赖或 Core→Harness／A2A 污染。
- 使用 Zulu JDK 17 执行最终验证；迁移后 Surefire 汇总不得低于 554 tests，且 failures／errors／skips 均为 0。
- 与本次升级无关的历史测试只做只读审计；未获用户确认不得迁移。

---

## File Map

### 永久新增

- `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/structure/AgentTestLayoutContractTest.java`：检查 AgentScope 2 测试没有回流到生产模块。
- `docs/superpowers/reports/2026-08-13-agent-test-layout-migration-report.md`：记录 RED／GREEN、JDK 17、测试总数、依赖和外部环境边界。
- `docs/superpowers/reports/2026-08-13-non-testcase-test-audit.md`：列出本轮之外仍在 `liteflow-testcase-el` 外的历史测试，等待用户决策。

### Maven 修改

- `liteflow-testcase-el/liteflow-testcase-el-react-agent-core/pom.xml`：无容器 Core、基础配置和 Solon ServiceLoader 隔离测试依赖。
- `liteflow-testcase-el/liteflow-testcase-el-react-agent-harness/pom.xml`：无容器 Harness 测试依赖与 `commons-io 2.16.1` 兼容边界。
- `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`：Provider、A2A 与 Spring 纵切测试依赖；Provider 改为 test scope；显式增加 Agent Core、A2A Server 与 Reactor Test。
- `liteflow-testcase-el/liteflow-testcase-el-springboot/pom.xml`：为迁入的 Spring Boot 3 Agent 绑定测试增加 test-scope Agent Core。
- `liteflow-react-agent/liteflow-react-agent-{core,openai,anthropic,gemini,dashscope,harness,a2a}/pom.xml`：删除迁出测试专用依赖；Agent Core 在升级基线前已有的 JUnit 依赖保持不变。
- `liteflow-core/pom.xml`：删除本次升级为 `AgentConfigV2Test` 新增的 JUnit 依赖。
- `liteflow-spring-boot-starter/pom.xml`：删除本次升级为 Agent 绑定测试新增的 Starter Test 与 Agent Core 测试依赖。
- `liteflow-solon-plugin/pom.xml`：删除本次升级为 Solon 隔离测试新增的 JUnit 依赖。
- `liteflow-spring-boot4-starter/pom.xml`：保留升级前已有的 Spring Boot 4 测试依赖和 Surefire 配置，不做无关清理。

### 迁移目录

- `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/**` → `liteflow-testcase-el/liteflow-testcase-el-react-agent-core/src/test/java/com/yomahub/liteflow/agent/**`。
- Provider 的 `com/yomahub/liteflow/agent/{openai,anthropic,gemini,dashscope}/**` → 集中模块相同 package 路径。
- Harness 的 `com/yomahub/liteflow/agent/harness/**` → `liteflow-testcase-el-react-agent-harness` 相同 package 路径。
- A2A 的 `com/yomahub/liteflow/agent/a2a/**` → 集中模块现有 `com/yomahub/liteflow/agent/a2a/**` 路径。
- `liteflow-core/.../AgentConfigV2Test.java` → Core testcase 子模块 `com/yomahub/liteflow/property/agent/AgentConfigV2Test.java`。
- Spring Boot 3／4 测试 → 各自 testcase 子模块的原 package 路径；Solon ServiceLoader 测试 → 无容器 Core testcase 子模块的原 package 路径。

---

### Task 1: 建立集中测试 classpath 与结构契约 RED

**Files:**
- Create temporarily, then remove before commit: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/structure/AgentTestLayoutContractTest.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-springboot/pom.xml`

**Interfaces:**
- Consumes: Maven reactor 中现有 `liteflow-react-agent-*`、Spring Boot 3 testcase 和 AgentScope BOM 2.0.2。
- Produces: 后续五批集中测试需要的 test classpath；结构契约的确定性 RED 证据。

- [ ] **Step 1: 验证迁移基线恰好为 63 个文件**

Run:

```bash
test "$(find liteflow-react-agent -path '*/src/test/*' -type f -name '*.java' | wc -l | tr -d ' ')" = "59"
test -f liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java
test -f liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java
test -f liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java
test -f liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java
test "$(git ls-tree -r --name-only 2655a16b13d7fd95c48575c979cb5b3f45b84b7b -- liteflow-react-agent | awk '/\/src\/test\// {n++} END {print n+0}')" = "0"
```

Expected: 全部 exit 0。

- [ ] **Step 2: 写入最终形状的结构契约测试**

Create:

```java
package com.yomahub.liteflow.test.agent.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentTestLayoutContractTest {

    private static final List<String> PROHIBITED_ROOTS = List.of(
            "liteflow-react-agent/liteflow-react-agent-core/src/test",
            "liteflow-react-agent/liteflow-react-agent-openai/src/test",
            "liteflow-react-agent/liteflow-react-agent-anthropic/src/test",
            "liteflow-react-agent/liteflow-react-agent-gemini/src/test",
            "liteflow-react-agent/liteflow-react-agent-dashscope/src/test",
            "liteflow-react-agent/liteflow-react-agent-harness/src/test",
            "liteflow-react-agent/liteflow-react-agent-a2a/src/test");

    private static final List<String> PROHIBITED_FILES = List.of(
            "liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java",
            "liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java",
            "liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java",
            "liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java");

    @Test
    void agentScope2TestsLiveOnlyUnderLiteflowTestcaseEl() throws IOException {
        Path root = repositoryRoot();
        List<String> violations = new ArrayList<>();
        for (String relativeRoot : PROHIBITED_ROOTS) {
            Path prohibited = root.resolve(relativeRoot);
            if (!Files.exists(prohibited)) {
                continue;
            }
            try (var paths = Files.walk(prohibited)) {
                paths.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .map(Path::toString)
                        .forEach(violations::add);
            }
        }
        for (String relativeFile : PROHIBITED_FILES) {
            if (Files.isRegularFile(root.resolve(relativeFile))) {
                violations.add(relativeFile);
            }
        }
        violations.sort(Comparator.naturalOrder());
        assertEquals(List.of(), violations,
                () -> "AgentScope 2 tests must live under liteflow-testcase-el: " + violations);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("liteflow-react-agent"))
                    && Files.isDirectory(current.resolve("liteflow-testcase-el"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate LiteFlow repository root");
    }
}
```

- [ ] **Step 3: 运行结构契约并确认自然 RED**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=AgentTestLayoutContractTest
```

Expected: `AgentTestLayoutContractTest` FAIL；错误清单包含 59 个 Agent 模块文件和另外 4 个精确文件，共 63 项。

- [ ] **Step 4: 从工作树删除尚未可提交的结构契约**

用 `apply_patch` 删除 `AgentTestLayoutContractTest.java`。该文件会在 Task 6 以完全相同的内容永久加入；Task 1 只保留 RED 证据，不提交失败测试。

- [ ] **Step 5: 集中 Maven 测试依赖**

在 `liteflow-testcase-el-react-agent/pom.xml` 中：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-core</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```

给 Gemini、OpenAI、Anthropic、DashScope 四个现有依赖增加 `<scope>test</scope>`，并增加：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-a2a-server</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <version>3.8.2</version>
    <scope>test</scope>
</dependency>
```

在 `liteflow-testcase-el-springboot/pom.xml` 中增加：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-core</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 6: 验证集中 classpath 可编译**

Run:

```bash
mvn test-compile \
  -pl liteflow-testcase-el/liteflow-testcase-el-react-agent,liteflow-testcase-el/liteflow-testcase-el-springboot \
  -am -DskipTests=false -DskipITs
```

Expected: BUILD SUCCESS；不得出现 aggregate AgentScope artifact。

- [ ] **Step 7: 提交 classpath 准备**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml \
  liteflow-testcase-el/liteflow-testcase-el-springboot/pom.xml
git diff --cached --check
git commit -m "test(agent): prepare centralized test classpath"
```

---

### Task 2: 迁移基础配置与 Agent Core 测试

**Files:**
- Move: `liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java`
- Move: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/**`
- Modify: `liteflow-core/pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/pom.xml`

**Interfaces:**
- Consumes: Task 1 的 Agent Core test-scope dependency、JUnit、Mockito、Reactor Test。
- Produces: 集中模块中的 28 个 Core／配置测试类和 3 个夹具；两个生产模块不再含本轮测试文件。

- [ ] **Step 1: 移动 31 个源码并保留 package**

Run:

```bash
mkdir -p liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent
mkdir -p liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/property/agent
git mv liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/* \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/
git mv liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java
```

Expected: `git diff --summary` 显示 31 个 rename；文件内 `package` 行无变化。

- [ ] **Step 2: 删除生产模块的测试专用依赖**

从 `liteflow-core/pom.xml` 删除本次升级新增的 `junit-jupiter` test dependency。

从 `liteflow-react-agent-core/pom.xml` 删除本次升级新增的以下两个 test dependency：

```xml
org.mockito:mockito-core
io.projectreactor:reactor-test
```

该模块的 `org.junit.jupiter:junit-jupiter` 在升级基线前已存在，本轮不做无关清理。

- [ ] **Step 3: 运行迁入的 28 个测试类**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=AgentConfigV2Test,AbstractAgentComponentTest,ReActAgentBuilderConfigurationTest,ReActAgentCoreContractTest,ReActAgentPlainTextTest,ReActAgentStructuredOutputTest,ReActRetryFallbackTest,InvocationIdentityResolverTest,AgentEventTypeMapperTest,LocalAgentInvocationGuardTest,HitlGuardConcurrencyTest,ReActCallExecutorTest,ChatUsageMiddlewareTest,FlowEventBridgeMiddlewareTest,MiddlewareOrderTest,ModelRoutingMiddlewareTest,StateStoreFailureMiddlewareTest,CredentialResolverTest,ModelSpecTest,OwnedTransportModelTest,AgentComponentLifecycleTest,AgentRuntimeHandleTest,McpClientLifecycleTest,AgentSkillRepositoryIntegrationTest,GuardedNamespacedAgentStateStoreTest,GuardedWorkspacePathResolverTest,ManagedProcessTreeTest,ToolkitRuntimeTest
```

Expected: 28 classes 全部 PASS；failures／errors／skips 均为 0。

- [ ] **Step 4: 验证来源目录已空**

Run:

```bash
test "$(find liteflow-react-agent/liteflow-react-agent-core/src/test -type f 2>/dev/null | wc -l | tr -d ' ')" = "0"
test ! -f liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java
```

- [ ] **Step 5: 提交 Core 批次**

```bash
git add liteflow-core liteflow-react-agent/liteflow-react-agent-core \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/property/agent
git diff --cached --check
git commit -m "test(agent): centralize core AgentScope tests"
```

---

### Task 3: 迁移四个 Provider 测试

**Files:**
- Move: `liteflow-react-agent/liteflow-react-agent-{openai,anthropic,gemini,dashscope}/src/test/java/com/yomahub/liteflow/agent/**`
- Modify: 四个 Provider `pom.xml`

**Interfaces:**
- Consumes: Task 1 中四个 test-scope Provider 模块依赖。
- Produces: 集中模块中的 8 个 Provider 测试；四个 Provider 生产模块不再含测试文件或 JUnit 依赖。

- [ ] **Step 1: 移动 Provider 测试目录**

Run:

```bash
git mv liteflow-react-agent/liteflow-react-agent-openai/src/test/java/com/yomahub/liteflow/agent/openai \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai
git mv liteflow-react-agent/liteflow-react-agent-anthropic/src/test/java/com/yomahub/liteflow/agent/anthropic \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/anthropic
git mv liteflow-react-agent/liteflow-react-agent-gemini/src/test/java/com/yomahub/liteflow/agent/gemini \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/gemini
git mv liteflow-react-agent/liteflow-react-agent-dashscope/src/test/java/com/yomahub/liteflow/agent/dashscope \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/dashscope
```

- [ ] **Step 2: 删除四个 Provider POM 的 JUnit test dependency**

从 OpenAI、Anthropic、Gemini、DashScope 四个生产 POM 中分别删除：

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: 运行 8 个 Provider 测试**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=FirstPartyModelProviderTest,OpenAICompatibleSpecTest,OpenAISpecTest,AnthropicRuntimeContractTest,AnthropicSpecTest,GeminiLifecycleTest,GeminiSpecTest,DashScopeSpecTest
```

Expected: 8 classes 全部 PASS；不访问 Provider 网络或读取真实 API key。

- [ ] **Step 4: 验证四个来源目录无文件**

Run:

```bash
for module in openai anthropic gemini dashscope; do
  test "$(find "liteflow-react-agent/liteflow-react-agent-$module/src/test" -type f 2>/dev/null | wc -l | tr -d ' ')" = "0"
done
```

- [ ] **Step 5: 提交 Provider 批次**

```bash
git add liteflow-react-agent/liteflow-react-agent-{openai,anthropic,gemini,dashscope} \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/{openai,anthropic,gemini,dashscope}
git diff --cached --check
git commit -m "test(agent): centralize provider AgentScope tests"
```

---

### Task 4: 迁移 Harness 测试与 sandbox 夹具

**Files:**
- Move: `liteflow-react-agent/liteflow-react-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/**`
- Modify: `liteflow-react-agent/liteflow-react-agent-harness/pom.xml`

**Interfaces:**
- Consumes: testcase 中的 Harness、Reactor Test 与 Agent Core 测试依赖。
- Produces: 集中模块中的 12 个 Harness 测试类和 3 个 fake sandbox／snapshot 夹具。

- [ ] **Step 1: 移动完整 Harness 测试树**

Run:

```bash
git mv liteflow-react-agent/liteflow-react-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/harness
```

Expected: 15 个 Java 文件全部成为 rename，package 不变。

- [ ] **Step 2: 删除 Harness POM 的测试专用依赖**

删除：

```xml
org.junit.jupiter:junit-jupiter
io.projectreactor:reactor-test
```

保留生产所需 `agentscope-harness` 和 `commons-io:2.16.1`。

- [ ] **Step 3: 运行 12 个 Harness 测试类**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=HarnessAgentBuilderFilesystemBridgeTest,HarnessAgentComponentTest,DockerSandboxConfigTest,HarnessConfigTest,GuardedLocalFilesystemTest,HarnessCapabilitiesTest,HarnessDependencyBoundaryTest,HarnessPermissionHitlTest,DockerSandboxConfigurerTest,SandboxLifecycleTest,WorkspaceProjectionTest,CrossAgentWorkspaceGuardTest
```

Expected: 12 classes 全部 PASS；fake Docker lifecycle 可运行，但不得执行 `docker`、访问 daemon 或网络。

- [ ] **Step 4: 验证 Harness 来源目录无文件**

```bash
test "$(find liteflow-react-agent/liteflow-react-agent-harness/src/test -type f 2>/dev/null | wc -l | tr -d ' ')" = "0"
```

- [ ] **Step 5: 提交 Harness 批次**

```bash
git add liteflow-react-agent/liteflow-react-agent-harness \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/harness
git diff --cached --check
git commit -m "test(agent): centralize Harness AgentScope tests"
```

---

### Task 5: 迁移 A2A client／server 测试

**Files:**
- Move: `liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/**`
- Modify: `liteflow-react-agent/liteflow-react-agent-a2a/pom.xml`

**Interfaces:**
- Consumes: testcase 中现有 `RecordingA2aRuntimeFactory`、A2A 模块、显式 A2A Server、Mockito 和 Reactor Test。
- Produces: 集中模块中的 6 个 A2A 测试，并保留同 package 的 package-private factory seam 覆盖。

- [ ] **Step 1: 合并 A2A 测试到现有 package 目录**

Run:

```bash
git mv liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/*.java \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/a2a/
git mv liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/server \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/a2a/server
```

Expected: 新文件与 `RecordingA2aRuntimeFactory.java` 共存；不得修改 package-private 生产类型的可见性。

- [ ] **Step 2: 删除 A2A POM 的测试专用依赖**

删除：

```xml
org.junit.jupiter:junit-jupiter
io.projectreactor:reactor-test
org.mockito:mockito-core
```

保留 production client 与 optional server 依赖。

- [ ] **Step 3: 运行 6 个 A2A 测试类**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=A2aAgentComponentTest,A2aClientRuntimeFactoryPublicApiTest,A2aClientRuntimeTest,A2aProtocolEventAdapterTest,LiteFlowA2aAgentRunnerLifecycleTest,LiteFlowA2aAgentRunnerTest
```

Expected: 6 classes全部 PASS；不启动 A2A server，不绑定 Web，不访问网络。

- [ ] **Step 4: 验证 A2A 来源目录无文件并提交**

```bash
test "$(find liteflow-react-agent/liteflow-react-agent-a2a/src/test -type f 2>/dev/null | wc -l | tr -d ' ')" = "0"
git add liteflow-react-agent/liteflow-react-agent-a2a \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/a2a
git diff --cached --check
git commit -m "test(agent): centralize A2A AgentScope tests"
```

---

### Task 6: 迁移框架集成测试并永久启用结构契约

**Files:**
- Move: Spring Boot 3 `AgentPropertyBindingTest.java`
- Move: Spring Boot 4 `AgentPropertyBindingTest.java`
- Move: `SolonCmpAroundAspectTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/structure/AgentTestLayoutContractTest.java`
- Modify: `liteflow-spring-boot-starter/pom.xml`
- Modify: `liteflow-solon-plugin/pom.xml`

**Interfaces:**
- Consumes: Task 1 的 Spring Boot 3 Agent Core test dependency，以及 Spring Boot 4／Solon testcase 现有独立 classpath。
- Produces: 63 个升级测试全部位于 testcase；永久结构契约转为 GREEN。

- [ ] **Step 1: 移动三个框架测试**

Run:

```bash
mkdir -p liteflow-testcase-el/liteflow-testcase-el-springboot/src/test/java/com/yomahub/liteflow/springboot
mkdir -p liteflow-testcase-el/liteflow-testcase-el-springboot4/src/test/java/com/yomahub/liteflow/springboot4
mkdir -p liteflow-testcase-el/liteflow-testcase-el-solon/src/test/java/com/yomahub/liteflow/spi/solon
git mv liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java \
  liteflow-testcase-el/liteflow-testcase-el-springboot/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java
git mv liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java \
  liteflow-testcase-el/liteflow-testcase-el-springboot4/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java
git mv liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java \
  liteflow-testcase-el/liteflow-testcase-el-solon/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java
```

- [ ] **Step 2: 清理本次升级新增的生产测试依赖**

从 `liteflow-spring-boot-starter/pom.xml` 删除：

```xml
org.springframework.boot:spring-boot-starter-test
com.yomahub:liteflow-react-agent-core
```

从 `liteflow-solon-plugin/pom.xml` 删除 `org.junit.jupiter:junit-jupiter`。

`liteflow-spring-boot4-starter/pom.xml` 的 test dependency 与 Surefire 配置在升级基线前已存在，保持不变。

- [ ] **Step 3: 在三个独立 classpath 中运行迁入测试**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=com.yomahub.liteflow.springboot.AgentPropertyBindingTest
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot4 -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=com.yomahub.liteflow.springboot4.AgentPropertyBindingTest
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-solon -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=com.yomahub.liteflow.spi.solon.SolonCmpAroundAspectTest
```

Expected: 三个命令分别 PASS；不得把 Spring Boot 3 与 Spring Boot 4 放进同一 classpath。

- [ ] **Step 4: 永久加入结构契约测试**

Create:

```java
package com.yomahub.liteflow.test.agent.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentTestLayoutContractTest {

    private static final List<String> PROHIBITED_ROOTS = List.of(
            "liteflow-react-agent/liteflow-react-agent-core/src/test",
            "liteflow-react-agent/liteflow-react-agent-openai/src/test",
            "liteflow-react-agent/liteflow-react-agent-anthropic/src/test",
            "liteflow-react-agent/liteflow-react-agent-gemini/src/test",
            "liteflow-react-agent/liteflow-react-agent-dashscope/src/test",
            "liteflow-react-agent/liteflow-react-agent-harness/src/test",
            "liteflow-react-agent/liteflow-react-agent-a2a/src/test");

    private static final List<String> PROHIBITED_FILES = List.of(
            "liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java",
            "liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java",
            "liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java",
            "liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java");

    @Test
    void agentScope2TestsLiveOnlyUnderLiteflowTestcaseEl() throws IOException {
        Path root = repositoryRoot();
        List<String> violations = new ArrayList<>();
        for (String relativeRoot : PROHIBITED_ROOTS) {
            Path prohibited = root.resolve(relativeRoot);
            if (!Files.exists(prohibited)) {
                continue;
            }
            try (var paths = Files.walk(prohibited)) {
                paths.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .map(Path::toString)
                        .forEach(violations::add);
            }
        }
        for (String relativeFile : PROHIBITED_FILES) {
            if (Files.isRegularFile(root.resolve(relativeFile))) {
                violations.add(relativeFile);
            }
        }
        violations.sort(Comparator.naturalOrder());
        assertEquals(List.of(), violations,
                () -> "AgentScope 2 tests must live under liteflow-testcase-el: " + violations);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("liteflow-react-agent"))
                    && Files.isDirectory(current.resolve("liteflow-testcase-el"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate LiteFlow repository root");
    }
}
```

- [ ] **Step 5: 运行结构契约 GREEN 与 shell 双保险**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=AgentTestLayoutContractTest
test -z "$(find liteflow-react-agent -path '*/src/test/*' -type f -print)"
test ! -f liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java
test ! -f liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java
test ! -f liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java
test ! -f liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java
```

Expected: 全部 exit 0。

- [ ] **Step 6: 提交框架批次与结构契约**

```bash
git add liteflow-spring-boot-starter liteflow-spring-boot4-starter liteflow-solon-plugin \
  liteflow-testcase-el/liteflow-testcase-el-springboot \
  liteflow-testcase-el/liteflow-testcase-el-springboot4 \
  liteflow-testcase-el/liteflow-testcase-el-solon \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/structure
git diff --cached --check
git commit -m "test(agent): centralize framework AgentScope tests"
```

---

### Task 7: JDK 17 全量验收与迁移报告

**Files:**
- Create: `docs/superpowers/reports/2026-08-13-agent-test-layout-migration-report.md`

**Interfaces:**
- Consumes: Tasks 1—6 的集中测试布局和永久结构契约。
- Produces: 可审计的测试总数、依赖边界、外部环境边界和最终审查证据。

- [ ] **Step 1: 确认使用 Zulu JDK 17**

Run:

```bash
export JAVA_HOME=/Users/bryan31/.sdkman/candidates/java/17.0.18-zulu
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Expected: `Zulu17.0.18` 或同一工作区已验证的 Zulu 17 安装；不得使用 JDK 21 编译后声称 JDK 17 通过。

- [ ] **Step 2: 运行全部受影响模块的 clean package**

Run:

```bash
mvn clean package \
  -pl liteflow-react-agent,liteflow-testcase-el/liteflow-testcase-el-react-agent,liteflow-testcase-el/liteflow-testcase-el-springboot,liteflow-testcase-el/liteflow-testcase-el-springboot4,liteflow-testcase-el/liteflow-testcase-el-solon \
  -am -DskipTests=false -DskipITs
```

Expected: BUILD SUCCESS；所有已发现测试 failures／errors／skips 为 0；默认流程不执行 Docker IT、live Provider 或真实 A2A transport。

- [ ] **Step 3: 汇总新鲜 Surefire 结果**

Run:

```bash
find liteflow-core liteflow-react-agent liteflow-testcase-el \
  -path '*/target/surefire-reports/TEST-*.xml' \
  \( -path 'liteflow-core/*' \
     -o -path 'liteflow-react-agent/*' \
     -o -path 'liteflow-testcase-el/liteflow-testcase-el-react-agent/*' \
     -o -path 'liteflow-testcase-el/liteflow-testcase-el-springboot/*' \
     -o -path 'liteflow-testcase-el/liteflow-testcase-el-springboot4/*' \
     -o -path 'liteflow-testcase-el/liteflow-testcase-el-solon/*' \) \
  -print0 \
  | xargs -0 rg -o 'tests="[0-9]+"|failures="[0-9]+"|errors="[0-9]+"|skipped="[0-9]+"' \
  | sed -E 's/.*(tests|failures|errors|skipped)="([0-9]+)"/\1 \2/' \
  | awk '{sum[$1]+=$2} END {for (key in sum) print key, sum[key]}' \
  | sort
```

Expected: `tests` 不低于 554；`failures 0`、`errors 0`、`skipped 0`。

- [ ] **Step 4: 验证依赖边界**

Run:

```bash
mvn dependency:tree \
  -pl liteflow-react-agent/liteflow-react-agent-core,liteflow-react-agent/liteflow-react-agent-harness,liteflow-react-agent/liteflow-react-agent-a2a,liteflow-testcase-el/liteflow-testcase-el-react-agent \
  -DskipTests=false \
  -Dincludes=io.agentscope:*,com.yomahub:liteflow-react-agent-*
```

Expected:

- AgentScope artifacts 全部为 2.0.2；
- 无 `io.agentscope:agentscope` aggregate 或 1.x；
- Core 无 Provider、Harness、A2A；
- A2A Server 只在生产 A2A optional 边界和 testcase test classpath 中出现。

- [ ] **Step 5: 执行静态门禁**

Run:

```bash
test -z "$(find liteflow-react-agent -path '*/src/test/*' -type f -print)"
test -z "$(rg -n '@Disabled|Assumptions\.|assumeTrue|assumeFalse' \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java \
  liteflow-testcase-el/liteflow-testcase-el-{springboot,springboot4,solon}/src/test/java/com/yomahub/liteflow || true)"
git diff --check
```

Expected: 生产 Agent 模块扫描无输出；迁入测试没有新增 skip／assumption；`git diff --check` 无输出。

- [ ] **Step 6: 请求独立代码审查**

使用 `superpowers:requesting-code-review`，要求 reviewer 核对：63 文件完整性、package-private 可见性、Spring 版本隔离、POM scope、结构契约根定位、默认离线边界和测试计数。Critical／Important 必须为 0；若有 finding，先用 `superpowers:receiving-code-review` 复现并回到对应 Task 做 RED／GREEN。

- [ ] **Step 7: 写迁移报告**

报告必须写入以下已经实际观察到的内容：

- 63 个来源文件和五批迁移后的目标；
- Task 1 结构契约 RED 的 63 项清单摘要；
- 每批 focused 测试 classes／tests、failures、errors、skips；
- JDK 17 clean package 的 reactor 结果；
- 新鲜 Surefire 总数与 0 failures／errors／skips；
- AgentScope 2.0.2 dependency tree 结论；
- Docker、live Provider、真实 A2A transport 未执行的授权／环境边界；
- 独立审查 verdict。

- [ ] **Step 8: 提交验收报告**

```bash
git add docs/superpowers/reports/2026-08-13-agent-test-layout-migration-report.md
git diff --cached --check
git commit -m "docs(agent): record centralized test verification"
```

---

### Task 8: 审计无关历史测试并等待用户确认

**Files:**
- Create: `docs/superpowers/reports/2026-08-13-non-testcase-test-audit.md`

**Interfaces:**
- Consumes: 完成后的仓库布局；排除 `.git/**`、`.worktrees/**`、`target/**` 和整个 `liteflow-testcase-el/**`。
- Produces: 只读历史测试清单；不移动任何无关测试。

- [ ] **Step 1: 生成迁移后仍在 testcase 外的真实清单**

Run:

```bash
find . -path '*/src/test/*' -type f \
  ! -path './liteflow-testcase-el/*' \
  ! -path './.git/*' \
  ! -path './.worktrees/*' \
  ! -path '*/target/*' \
  | sort
```

Expected after Tasks 1—7: 只剩 `liteflow-benchmark` 的 97 个历史文件；若出现其他路径，先用 `git log --diff-filter=A` 分类，不得擅自迁移。

- [ ] **Step 2: 核对 benchmark 历史边界**

Run:

```bash
for module in liteflow-benchmark/liteflow-benchmark-{common,common-example,compile,script-groovy,script-java,script-javax,script-javax-pro,script-qlexpress}; do
  count=$(find "$module/src/test" -type f | wc -l | tr -d ' ')
  first=$(git log --diff-filter=A --format='%ad|%h|%s' --date=short -- "$module/src/test" | tail -1)
  printf '%s|%s|%s\n' "$module" "$count" "$first"
done
```

Expected table:

| 模块 | 文件数 | 最早日期 | 最早提交 |
|---|---:|---|---|
| `liteflow-benchmark-common` | 10 | 2024-09-29 | `857a6d6ea` |
| `liteflow-benchmark-common-example` | 29 | 2025-09-04 | `09e3dcf6a` |
| `liteflow-benchmark-compile` | 24 | 2025-12-02 | `3b43111f1` |
| `liteflow-benchmark-script-groovy` | 5 | 2024-09-29 | `7c9bf1ebe` |
| `liteflow-benchmark-script-java` | 10 | 2024-09-29 | `7c9bf1ebe` |
| `liteflow-benchmark-script-javax` | 6 | 2024-09-26 | `9334b1e84` |
| `liteflow-benchmark-script-javax-pro` | 5 | 2025-01-16 | `5805a1e53` |
| `liteflow-benchmark-script-qlexpress` | 8 | 2025-12-05 | `6206697c8` |

总计 97 个文件，其中 79 个 Java、18 个测试资源；它们在 AgentScope 2 基线前已存在，与本次升级无关。

- [ ] **Step 3: 写只读审计报告**

报告必须包含：

- 上述 8 个 benchmark 子模块、文件数、Java／resource 数量、最早提交；
- “与 AgentScope 2 无关”的证据：基线 `2655a16...` 中同样存在 97 个文件；
- 迁移风险：JMH／benchmark classpath、资源相对路径、运行 profile、构建时间；
- 建议：如用户批准，单独设计 benchmark testcase 归属，不与本次 AgentScope 2 迁移混合；
- 明确状态：未获用户确认，未移动任何历史文件。

- [ ] **Step 4: 提交审计报告**

```bash
git add docs/superpowers/reports/2026-08-13-non-testcase-test-audit.md
git diff --cached --check
git commit -m "docs(test): audit non-testcase test layout"
```

- [ ] **Step 5: 向用户请求下一阶段授权**

向用户报告 AgentScope 2 的 63 个文件已规范迁移并通过验证；同时列出 benchmark 的 97 个历史文件及风险。明确询问是否另开一轮迁移，收到确认前停止，不修改这些历史测试。
