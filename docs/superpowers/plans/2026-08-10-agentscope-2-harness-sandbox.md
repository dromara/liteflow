# AgentScope 2.0 Harness and Docker Sandbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增可选的 `liteflow-agent-harness`，完整接入 AgentScope 2.0.2 的 workspace、filesystem、compaction、memory、skills、subagents、task／plan、权限与 Docker 沙箱，同时把可信本地文件模式和不可信容器执行明确分级。

**Architecture:** Harness 复用 core 的组件模板、RuntimeContext、状态 namespace、调用租约、HITL 与输出执行器，只负责组装 `HarnessAgent` 和 Harness 专属能力。默认本地后端必须显式声明可信；不可信文件或 Shell 执行使用 `DockerFilesystemSpec`，通过 AgentScope 的 `SandboxManager`、`SandboxLifecycleMiddleware`、snapshot 与 workspace projection 管理容器生命周期。

**Tech Stack:** Java 17、Maven、LiteFlow、AgentScope Java 2.0.2 `agentscope-harness`、Docker CLI、Reactor、JUnit 5。

## Global Constraints

- 本计划在 `2026-08-10-agentscope-2-core-runtime.md` 完成后执行，并复用其中的 `AbstractAgentComponent`、`LiteFlowAgentContext`、`AgentRuntimeHandle`、`AgentInvocationGuard`、`AgentConfirmationHandler` 与结构化输出执行器；不得复制另一套身份、锁或 HITL 实现。
- Harness 是可选模块。`liteflow-agent-core` 不得传递 `agentscope-harness`、Docker 或 A2A 依赖。
- 依赖只使用 `io.agentscope:agentscope-harness` 2.0.2；禁止 shaded `io.agentscope:agentscope`，也不引入 `docker-java`，因为 2.0.2 Docker 实现调用本机 Docker CLI。
- `IsolationScope.SESSION` 是逻辑路由，不是宿主机安全边界。guarded local 只允许可信单用户／开发；不可信或多租户 Shell／文件执行必须使用容器或服务端强制 namespace 的 remote filesystem。
- Docker 默认 `network=none`，必须配置正数 CPU／内存配额；不开放 bind mount、exposed ports 或用户任意 `additionalRunArgs`。
- 默认测试不得要求 Docker。真实容器测试放在 `agent-docker-it` profile，并明确报告环境不具备时未执行。
- 所有 Maven 测试命令携带 `-DskipTests=false`；定向 reactor 测试携带 `-Dsurefire.failIfNoSpecifiedTests=false`。

---

## Task 1: 创建 Harness 可选模块和依赖边界

**Files:**

- Modify: `liteflow-agent/pom.xml`
- Create: `liteflow-agent/liteflow-agent-harness/pom.xml`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/HarnessDependencyBoundaryTest.java`

- [ ] **Step 1: 写依赖边界编译测试**

测试直接导入并验证精确的 2.0.2 类型：

```java
assertThat(HarnessAgent.class).isNotNull();
assertThat(DockerFilesystemSpec.class).isNotNull();
assertThat(DockerSandboxClientOptions.class).isNotNull();
assertThat(SubagentDeclaration.class).isNotNull();
```

- [ ] **Step 2: 运行测试并确认模块不存在**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessDependencyBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: Maven 因模块不存在而失败。

- [ ] **Step 3: 创建模块 POM**

POM 只声明：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-core</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
</dependency>
```

另加测试范围的 JUnit 5／Reactor Test，不显式写 AgentScope 子模块版本，由根 BOM 统一管理。

- [ ] **Step 4: 运行测试与 dependency tree**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessDependencyBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn dependency:tree -pl liteflow-agent/liteflow-agent-harness \
  -Dincludes=io.agentscope:* -Dverbose
```

Expected: PASS；依赖树只出现 2.0.2 的细粒度 AgentScope 模块，不出现 shaded `agentscope`。

- [ ] **Step 5: 提交模块骨架**

```bash
git add liteflow-agent/pom.xml liteflow-agent/liteflow-agent-harness
git commit -m "build(agent): add optional AgentScope harness module"
```

---

## Task 2: 定义 Harness 和 Docker 的纯 POJO 配置

**Files:**

- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/HarnessConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/HarnessFilesystemBackend.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/DockerSandboxConfig.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/filesystem/HarnessFilesystemConfigurer.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/filesystem/HarnessFilesystemContext.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/config/HarnessConfigTest.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/config/DockerSandboxConfigTest.java`
- Modify: `liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json`

- [ ] **Step 1: 写默认值、校验和 Spring 绑定失败测试**

覆盖以下配置：

```yaml
liteflow:
  agent:
    harness:
      filesystem-backend: docker
      trusted-local: false
      docker:
        image: alpine:3.20
        workspace-root: /workspace
        memory-size-bytes: 268435456
        cpu-count: 1
        network: none
        snapshot-root: ./data/agent-snapshots
        workspace-projection-enabled: true
        workspace-projection-roots: [AGENTS.md, skills, subagents, knowledge, .skills-cache]
```

断言 guarded local 且 `trusted-local=false` 时 fail-fast；Docker CPU 整数／内存非正数、空镜像、空 workspace root、非法 projection `..` 均失败。`cpu-count` 使用 `Long`，与 2.0.2 `DockerFilesystemSpec.cpuCount(Long)` 一致。

固定默认值：backend=`GUARDED_LOCAL`、trustedLocal=false、image=`ubuntu:22.04`、workspaceRoot=`/workspace`、memorySizeBytes=536870912、cpuCount=1、network=`none`、projection enabled=true、projection roots 为上游五个默认根。选择 guarded local 后必须把 trustedLocal 显式设为 true；选择 Docker 时这些安全默认值可直接使用。

- [ ] **Step 2: 运行测试并确认配置类型不存在**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessConfigTest,DockerSandboxConfigTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，配置类与 metadata 尚不存在。

- [ ] **Step 3: 实现配置和文件系统扩展契约**

`HarnessFilesystemBackend` 固定为 `GUARDED_LOCAL`、`DOCKER`、`CUSTOM`。接口固定为：

```java
public interface HarnessFilesystemConfigurer {
    void configure(HarnessAgent.Builder builder, HarnessFilesystemContext context);
}

public record HarnessFilesystemContext(
        Path workspaceRoot,
        long maxFileBytes,
        Duration commandTimeout,
        AgentConfig agentConfig) { }
```

`commandTimeout` 是 LiteFlow 工具执行硬上限，不得伪装为 `DockerSandboxClientOptions` 字段；上游 2.0.2 没有该 option。

- [ ] **Step 4: 运行配置测试**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessConfigTest,DockerSandboxConfigTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS，metadata 的属性名与绑定字段完全一致。

- [ ] **Step 5: 提交配置契约**

```bash
git add liteflow-core/src/main/java/com/yomahub/liteflow/property/agent \
  liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json \
  liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): define harness filesystem policy"
```

---

## Task 3: 实现 `HarnessAgentComponent` 与 runtime 所有权

**Files:**

- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/component/HarnessAgentComponent.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/runtime/HarnessAgentRuntime.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/component/HarnessAgentComponentTest.java`

- [ ] **Step 1: 写组件生命周期失败测试**

测试断言：同一实际组件实例只 build 一次；不同 session 并行；普通文本、POJO 和 JSON Schema 输出复用 core 的调用执行器；`close()` 幂等且只关闭 runtime 拥有的 Agent／repository／sandbox 资源；容器注入的共享资源不被关闭。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessAgentComponentTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，组件与 runtime 尚不存在。

- [ ] **Step 3: 实现与 core 一致的组件扩展点**

```java
public abstract class HarnessAgentComponent
        extends AbstractAgentComponent<HarnessAgentRuntime> {
    @Override
    protected HarnessAgentRuntime buildRuntime(
            AgentRuntimeBuildContext buildContext);

    @Override
    protected Mono<Msg> invokeRuntime(
            HarnessAgentRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext);

    protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) { return builder; }
    protected CompactionConfig compactionConfig() { return null; }
    protected MemoryConfig memoryConfig() { return null; }
    protected List<AgentSkillRepository> skillRepositories() { return List.of(); }
    protected List<SubagentDeclaration> subagents() { return List.of(); }
    protected boolean enablePlanMode() { return false; }
    protected PermissionContextState permissionContext() { return null; }
    protected HarnessFilesystemConfigurer filesystemConfigurer() { /* config selected */ }
    protected TaskRepository taskRepository() { return null; }
    protected ToolResultEvictionConfig toolResultEvictionConfig() { return null; }
}
```

`HarnessAgentComponent` 不覆写 `process()`；identity、两级 guard、Slot attachment、deadline、reply 和清理由 core final 模板负责。`invokeRuntime` 复用 core 的 TEXT／JAVA_TYPE／JSON_SCHEMA 调用执行器。

构建顺序固定为：name／agentId、model、sysPrompt、显式串行 Toolkit、state store、workspace／filesystem、compaction、memory、逐个 skill repository、逐个 subagent、task repository、task list、plan mode、permission context、tool result eviction，最后 `customizeHarness(builder)`。

- [ ] **Step 4: 实现 runtime 并运行测试**

`HarnessAgentRuntime` 持有一个组件级 `HarnessAgent` 和资源 ownership 清单，`close()` 反序关闭。不得把 `Slot` 或一次调用的 `RuntimeContext` 保存为字段。

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessAgentComponentTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

- [ ] **Step 5: 提交组件 runtime**

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): add HarnessAgent LiteFlow component"
```

---

## Task 4: 实现可信本地模式的真实路径保护

**Files:**

- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/filesystem/GuardedLocalFilesystem.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/filesystem/GuardedLocalFilesystemConfigurer.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/filesystem/GuardedLocalFilesystemTest.java`

- [ ] **Step 1: 写路径逃逸失败测试**

覆盖 `/etc/passwd`、`C:\\Windows\\...`、UNC、NUL、空路径、`../`、文件 symlink、目录 symlink、move 源／目标、超大 write／upload；同 `runtimeSessionId` 可见，不同 session 不可见。

- [ ] **Step 2: 运行测试并确认旧 `LocalFilesystem` 不能满足安全断言**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=GuardedLocalFilesystemTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，guarded wrapper 尚不存在。

- [ ] **Step 3: 实现 session root 与 real-path 校验**

`GuardedLocalFilesystem extends LocalFilesystem`，使用 `IsolationScope.SESSION.toNamespaceFactory()`。重写受保护的 `resolvePath(RuntimeContext, String)`：

1. 在创建 `Path` 前拒绝 NUL、空字符串、Unix／Windows／UNC 绝对路径；
2. 拒绝任何 `..` segment；
3. 以安全编码后的 session namespace 计算物理 session root；
4. 对现存目标或最近现存父目录调用 `toRealPath()`；
5. 真实路径必须仍以真实 session root 开头；
6. 任一中间路径是 symlink 时拒绝；
7. write／upload 在写入前检查 `WorkspaceConfig.maxFileBytes`；move 同时校验源与目标。

`GuardedLocalFilesystemConfigurer` 使用 `HarnessAgent.Builder.abstractFilesystem(guardedFilesystem)` 注入该实例；不能传给会创建 `LocalFilesystemWithShell` 的 `filesystem(LocalFilesystemSpec)`。guarded local 不注册宿主机 Shell；`trusted-local=true` 只表示操作者接受本地进程权限，不提升安全等级。

- [ ] **Step 4: 运行路径安全测试**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=GuardedLocalFilesystemTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

- [ ] **Step 5: 提交 guarded local**

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): secure local Harness workspace"
```

---

## Task 5: 映射 Docker filesystem spec 与强制安全策略

**Files:**

- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/sandbox/DockerSandboxConfigurer.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/sandbox/SandboxSnapshotProvider.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/DockerSandboxConfigurerTest.java`

- [ ] **Step 1: 写 option 映射与拒绝策略测试**

从 `DockerFilesystemSpec.toSandboxContext(workspace).getClientOptions()` 取得并强转 `DockerSandboxClientOptions`，精确断言 image、workspaceRoot、memorySizeBytes、cpuCount、network、snapshotSpec、`IsolationScope.SESSION`；再断言非法配额、开放端口、bind mount 和任意 raw args 被拒绝。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=DockerSandboxConfigurerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，Docker 配置映射尚不存在。

- [ ] **Step 3: 使用 2.0.2 精确 builder 字段实现映射**

构造 `DockerFilesystemSpec` 时映射：`.image()`、`.workspaceRoot()`、`.memorySizeBytes()`、`.cpuCount()`、`.network()`、`.snapshotSpec()`、`.isolationScope(IsolationScope.SESSION)`。`snapshot-root` 有值时使用 `LocalSnapshotSpec(Path)`，否则使用 `NoopSnapshotSpec`；远端快照经 `SandboxSnapshotProvider` 或 `customizeHarness()` 注入。

固定 Docker 安全参数仅为：

```text
--cap-drop=ALL
--security-opt=no-new-privileges:true
--pids-limit=64
```

默认 network 为 `none`；非 `none` 必须是显式配置。首版不暴露 exposed ports、bind mount 或用户 raw `additionalRunArgs`。

- [ ] **Step 4: 运行映射测试**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=DockerSandboxConfigurerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

- [ ] **Step 5: 提交 Docker spec**

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): add policy-bound Docker sandbox backend"
```

---

## Task 6: 验证 sandbox 生命周期、snapshot 与 workspace projection

**Files:**

- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/FakeSandboxClient.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/FakeSandbox.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/InMemorySandboxSnapshot.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/SandboxLifecycleTest.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/sandbox/WorkspaceProjectionTest.java`

- [ ] **Step 1: 写 fake sandbox 生命周期失败测试**

固定顺序为：acquire → start → tool → persist state → stop → shutdown → lease close。成功、失败、取消和 HITL continuation 都必须清理；snapshot 后重建 runtime 能恢复文件。

projection 只包含 `AGENTS.md`、`skills`、`subagents`、`knowledge`、`.skills-cache`；路径排序和 hash 必须确定，拒绝 `..` 和 symlink 源。

- [ ] **Step 2: 写两 session 并发回归测试**

上游 2.0.2 `SandboxLifecycleMiddleware` 内部使用单个 `AtomicReference<SandboxAcquireResult>`。测试两个不同 session 同时执行，必须证明资源不串。如果测试失败，先用 Harness runtime 的 session gate 串行规避并记录上游问题，不得引入另一个共享 mutable reference。

- [ ] **Step 3: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=SandboxLifecycleTest,WorkspaceProjectionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，fake 与生命周期断言尚未实现。

- [ ] **Step 4: 复用上游生命周期组件并补并发门禁**

不要重写 `SandboxManager`。通过 `HarnessAgent.Builder.filesystem(DockerFilesystemSpec)` 使用上游 `SandboxManager`、`SessionSandboxStateStore`、`SandboxLifecycleMiddleware` 和 `WorkspaceProjectionApplier`。只在并发回归确实失败时增加按 `runtimeSessionId` 的 gate；gate 必须在所有 terminal signal 上释放并清理 key。

明确记录：上游 cleanup／persist 失败是日志型 best-effort，不能对外承诺事务持久化。

- [ ] **Step 5: 运行生命周期测试**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=SandboxLifecycleTest,WorkspaceProjectionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS，包括两 session 并发回归。

- [ ] **Step 6: 提交生命周期集成**

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): wire Harness sandbox lifecycle and snapshots"
```

---

## Task 7: 分离 Agent 状态与 conversation workspace 状态

**Files:**

- Modify: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/component/HarnessAgentComponent.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/state/HarnessNamespacedAgentStateStore.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/state/CrossAgentWorkspaceGuardTest.java`

- [ ] **Step 1: 写跨 Agent 工作区并发失败测试**

两个不同 nodeId／agentKey、同 namespace＋user＋conversation 并发写同一 workspace，断言 `maxActive=1` 且最终无丢失；不同 conversation 可并行；第二把锁失败时第一把锁被释放；HITL 两次 call 之间不能插队。

状态断言：Docker fake snapshot 在不同 agentKey 间可见，但 `agent_state` 不串。

- [ ] **Step 2: 运行测试并确认单一 namespace 装饰器会错误隔离 workspace**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=CrossAgentWorkspaceGuardTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 实现固定锁顺序和双 namespace 路由**

`HarnessAgentComponent.requiresWorkspaceLease()` 返回 true；固定先获取 conversation workspace lease，再获取 Agent state lease，反序释放。workspace key 不含 agentKey。

`HarnessNamespacedAgentStateStore` 路由规则：

- `agent_state` 使用 core 的 agent namespace；
- 上游 `_sandbox_state` 与 `sandbox/session/<runtimeSessionId>` 使用 conversation workspace namespace；
- 其他 key 明确列入测试后选择其中一种路由，不能默认字符串拼接。

不要修改上游 `SandboxManager`。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=CrossAgentWorkspaceGuardTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "fix(agent): serialize shared Harness workspaces"
```

---

## Task 8: 接入 Harness 权限和 core HITL continuation

**Files:**

- Modify: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/component/HarnessAgentComponent.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/permission/HarnessPermissionHitlTest.java`

- [ ] **Step 1: 写 allow／ask／deny 失败测试**

覆盖 allow、显式 deny、未配置 handler、handler timeout、错误 replyId、未知 tool-use id、handler 异常。ASK 时工具调用次数必须为 0；默认拒绝后发送 denial continuation 清除 ASK 状态；租约贯穿首轮、handler、continuation。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessPermissionHitlTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，Harness 还未复用 core HITL。

- [ ] **Step 3: 组装 permission context 并复用 continuation**

builder 精确调用 `.permissionContext(PermissionContextState)`；默认 `PermissionMode.DEFAULT`，execute／写工具没有 allow rule 时 ASK 或 DENY，不使用 BYPASS。

完全复用 core 的 `AgentConfirmationHandler` 和调用执行器；恢复消息只包含 `Msg.METADATA_CONFIRM_RESULTS`，继续使用同一 `RuntimeContext`、同一 `HarnessAgent`、同一 workspace lease 和 state lease。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessPermissionHitlTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): apply Harness permission and confirmation policy"
```

---

## Task 9: 开放上下文工程、技能、子 Agent 与计划能力

**Files:**

- Modify: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/component/HarnessAgentComponent.java`
- Modify: `liteflow-agent/liteflow-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/runtime/HarnessAgentRuntime.java`
- Create: `liteflow-agent/liteflow-agent-harness/src/test/java/com/yomahub/liteflow/agent/harness/HarnessCapabilitiesTest.java`

- [ ] **Step 1: 写 Harness 能力失败测试**

使用 fake model／filesystem 验证：additional context file 注入；compaction 触发并保留 tail；memory flush 写入 session namespace；skill allowlist 与实际使用跟踪；本地 subagent 继承 parent permission；task list／plan state 按 session 持久；plan mode 下 Shell 默认 denied。

- [ ] **Step 2: 运行测试并确认能力未接线**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessCapabilitiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 使用精确 builder API 完成组装**

逐项调用：`.compaction(CompactionConfig)`、`.memory(MemoryConfig)`、`.skillRepository(AgentSkillRepository)`、`.subagent(SubagentDeclaration)`、`.taskRepository(TaskRepository)`、`.enableTaskList()`、`.enablePlanMode()`、`.toolResultEviction(ToolResultEvictionConfig)`、`.additionalContextFile(String)`。

skill repository 和 subagent 使用逐项 add 语义，不能把它们误当成整体替换。远程 subagent、Channel 与 distributed task repository 只通过 `customizeHarness()` 或容器 Bean 开放，不承诺再复制一套 LiteFlow 配置。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness -am \
  -DskipTests=false -DskipITs \
  -Dtest=HarnessCapabilitiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-agent/liteflow-agent-harness/src
git commit -m "feat(agent): expose Harness context engineering"
```

---

## Task 10: 增加确定性集成测试和显式 Docker IT

**Files:**

- Modify: `liteflow-testcase-el/liteflow-testcase-el-agent/pom.xml`
- Create: `liteflow-testcase-el/liteflow-testcase-el-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/harness/HarnessComponentTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/harness/DockerSandboxIT.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-agent/src/test/resources/feature/harness/application.properties`
- Create: `liteflow-testcase-el/liteflow-testcase-el-agent/src/test/resources/feature/harness/flow.el.xml`

- [ ] **Step 1: 写默认离线 Spring 集成测试**

`HarnessComponentTest` 使用 fake model 和 fake sandbox，不访问网络／Docker；验证 Harness Agent 作为 LiteFlow 节点参与 THEN／WHEN，工作区和状态按 conversation 路由，异常交给 LiteFlow 响应语义。

- [ ] **Step 2: 写 `agent-docker-it` Failsafe profile**

`DockerSandboxIT` 只由显式 profile 执行。前置要求 `docker info` 成功且本地存在 `alpine:3.20`；测试验证容器内文件读写、命令硬超时、snapshot 恢复、`docker inspect` 的 Memory／NanoCpus／NetworkMode，以及成功和异常后的容器删除。

- [ ] **Step 3: 运行默认 Harness 全套测试**

Run:

```bash
mvn test -pl liteflow-agent/liteflow-agent-harness,liteflow-testcase-el/liteflow-testcase-el-agent -am \
  -DskipTests=false -DskipITs
```

Expected: PASS，不调用 Docker。

- [ ] **Step 4: 在具备 Docker 的环境运行真实沙箱**

Run:

```bash
docker pull alpine:3.20
mvn verify -pl liteflow-testcase-el/liteflow-testcase-el-agent -am \
  -Pagent-docker-it -DskipTests=false -Dit.test=DockerSandboxIT \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS；若 Docker 不可用，记录“未执行”，不能记录为通过。

- [ ] **Step 5: 审计依赖并提交**

Run:

```bash
mvn dependency:tree -pl liteflow-agent/liteflow-agent-harness \
  -Dincludes=io.agentscope:* -Dverbose
git diff --check
```

Expected: 只含 2.0.2 的 `agentscope-core` 与 `agentscope-harness` 相关细粒度模块，无聚合包和多版本。

```bash
git add liteflow-agent/liteflow-agent-harness \
  liteflow-testcase-el/liteflow-testcase-el-agent
git commit -m "test(agent): add deterministic and opt-in Docker coverage"
```

---

## Completion Criteria

- `HarnessAgentComponent` 开放 compaction、memory、skills、`List<SubagentDeclaration>`、task／plan、permissions 和 builder escape hatch。
- guarded local 的绝对路径、`..`、symlink 与超大文件逃逸测试全部通过，且宿主 Shell 默认关闭。
- Docker backend 使用 AgentScope 2.0.2 官方 `DockerFilesystemSpec`／`DockerSandboxClientOptions`，有 network、CPU、内存和进程权限约束。
- sandbox 在成功、错误、取消、HITL 与 runtime close 时回收；snapshot 和 workspace projection 有确定性测试。
- 相同 conversation 的不同 Agent 共享 workspace 但隔离 AgentState，并由固定锁顺序防止并发覆盖。
- 默认测试完全离线；真实 Docker IT 只能显式启动。
