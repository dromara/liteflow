# AgentScope 2.0 Core Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `liteflow-react-agent-core` 从 AgentScope 1.0.12 迁移到 2.0.2，建立组件级无状态 runtime、显式 `RuntimeContext`、AgentStateStore namespace、原子 HITL 事务和无网络确定性测试基线。

**Architecture:** `AbstractAgentComponent.process()` 统一处理 identity、调用租约、RuntimeContext、超时、结构化回复和清理；每个实际组件实例拥有一个 `AgentRuntimeHandle`。`ReActAgentRuntime` 只保存构建期 Agent、namespaced StateStore 和有所有权资源，调用期 Slot／身份／usage／事件全部从 `LiteFlowAgentContext` 与 `RuntimeContext` 获取。

**Tech Stack:** Java 17、Maven、LiteFlow、AgentScope Java 2.0.2 `agentscope-core`、Reactor、Jackson、JUnit 5、Mockito。

## Global Constraints

- 以 `docs/superpowers/specs/2026-08-10-agentscope-2-upgrade-design.md` 为设计基线。
- AgentScope 版本固定为 `2.0.2`，由根 `agentscope-bom` 管理。core 只依赖 `agentscope-core`，禁止同时依赖 shaded `io.agentscope:agentscope`。
- 核心测试不得访问网络、读取真实密钥或依赖 Docker；使用 `ScriptedChatModel` 固定输入、事件、回复和调用次数。
- 构建期扩展点不能读取 Slot 或调用期 context；Middleware／Tool 不能把 `Slot`、listener、usage tracker 或 `LiteFlowAgentContext` 保存为字段。
- `process()` 与 `close()` 都是 final；每个实际组件实例有自己的 runtime handle，不使用捕获第一份配置的静态 runtime holder。
- 状态租约 key 是 namespace＋userId＋conversationId＋agentKey；工作区租约不含 agentKey。需要两把锁时先 workspace、后 state，反序释放。
- 所有 Maven 测试命令显式携带 `-DskipTests=false`；定向 reactor 测试携带 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 每个 Task 完成后独立提交；不要把 Provider、Harness 或 A2A 代码提前混入本计划。

## Fixed Core Interfaces

```java
public abstract class AbstractAgentComponent<R extends AutoCloseable>
        extends NodeComponent implements AutoCloseable {
    protected abstract R buildRuntime(AgentRuntimeBuildContext buildContext);
    protected abstract Mono<Msg> invokeRuntime(
            R runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext);

    protected abstract String systemPrompt();
    protected abstract String userPrompt(LiteFlowAgentContext context);
    protected String resolveUserId(Slot slot);
    protected String resolveConversationId(Slot slot);
    protected String agentKey();
    protected Mono<String> transformSystemPrompt(
            String currentPrompt, LiteFlowAgentContext context);
    protected Class<?> structuredOutputType();
    protected JsonNode structuredOutputSchema();
    protected void customizeRuntimeContext(
            RuntimeContext.Builder builder, LiteFlowAgentContext context);
    protected void handleReply(Msg reply, LiteFlowAgentContext context);
    protected boolean requiresWorkspaceLease();

    @Override public final void process() throws Exception;
    @Override public final void close();
}
```

```java
public final class AgentRuntimeHandle<R extends AutoCloseable>
        implements AutoCloseable {
    public R getOrCreate(Supplier<? extends R> factory);
    public boolean isInitialized();
    public boolean isClosed();
    @Override public void close();
}

public record AgentOutputSpec(
        Kind kind, Class<?> javaType, JsonNode jsonSchema) {
    public enum Kind { TEXT, JAVA_TYPE, JSON_SCHEMA }
    public static AgentOutputSpec text();
    public static AgentOutputSpec javaType(Class<?> type);
    public static AgentOutputSpec jsonSchema(JsonNode schema);
}
```

```java
public interface AgentInvocationGuard {
    AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout);
}

public interface AgentInvocationLease extends AutoCloseable {
    AgentInvocationKey key();
    default OptionalLong fencingToken();
    default void renew(Duration leaseDuration);
    @Override void close();
}

public record AgentInvocationKey(
        AgentInvocationScope scope,
        String namespace,
        String userId,
        String conversationId,
        String agentKey) { }
```

---

## Task 1: 增加 2.0 runtime、StateStore、Toolkit、事件和 HITL 配置

**Files:**

- Modify: `liteflow-core/pom.xml`
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentConfig.java`
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/ShellConfig.java`
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/WorkspaceConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentRuntimeConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentStateStoreConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentStateStoreType.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentStateStoreFailurePolicy.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentToolkitConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentEventConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentListenerFailureMode.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentInvocationGuardConfig.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentInvocationGuardMode.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/DistributedCoordinationMode.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/WorkspaceBackend.java`
- Create: `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/AgentHitlConfig.java`
- Create: `liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java`

- [ ] **Step 1: 写配置默认值和非法组合失败测试**

固定默认值：

```text
runtime.namespace             = 无默认值，运行前必须非空
runtime.defaultUserId         = anonymous
runtime.timeout               = 2m
stateStore.type               = MEMORY
stateStore.jsonRoot           = ./data/agent-state
stateStore.failurePolicy      = FAIL_FAST
toolkit.parallel              = false
event.listenerFailureMode     = FAIL_FAST
invocationGuard.mode          = LOCAL
invocationGuard.acquireTimeout= 2m
invocationGuard.coordinationMode = NONE
invocationGuard.strictDistributed = true
workspace.backend             = GUARDED_LOCAL
workspace.trustedLocal        = false
hitl.confirmationTimeout      = 2m
hitl.failOnDeniedTool         = false
shell.mode                    = DISABLED
```

旧 `session.memory.*` 若出现，validator 必须返回包含 `session.memory -> state-store` 的迁移错误，不能静默忽略。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-core \
  -DskipTests=false -Dtest=AgentConfigV2Test \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，新配置类型和默认值尚不存在。

- [ ] **Step 3: 实现纯 POJO 配置和 validator**

`liteflow-core` 中的配置类只能依赖 JDK 类型，不得 import AgentScope、Reactor 或厂商 SDK。`AgentConfig` 保留 Provider credential；新增 runtime／stateStore／toolkit／event／invocationGuard／hitl 聚合字段。旧 Session 配置只用于明确迁移诊断，不再参与执行。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-core \
  -DskipTests=false -Dtest=AgentConfigV2Test \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-core/pom.xml liteflow-core/src/main/java/com/yomahub/liteflow/property/agent \
  liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java
git commit -m "feat(agent): add AgentScope 2 runtime configuration"
```

---

## Task 2: 实现不可碰撞 identity 和应用内调用租约

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/AgentInvocationException.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/context/AgentInvocationIdentity.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/context/InvocationIdentityResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception/AgentInvocationErrorType.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/AgentInvocationGuard.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/AgentInvocationLease.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/AgentInvocationKey.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/AgentInvocationScope.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/LocalAgentInvocationGuard.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard/AgentInvocationGuardResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/context/InvocationIdentityResolverTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/guard/LocalAgentInvocationGuardTest.java`

- [ ] **Step 1: 写编码与并发失败测试**

编码使用 UTF-8 长度前缀输入和 SHA-256：

```text
runtimeSessionId = "lf-" + hex(sha256(namespace, userId, conversationId))
agentNamespace   = "lf-" + hex(sha256(namespace, agentKey))
storeSessionId   = agentNamespace + "." + runtimeSessionId
```

断言 `a/b` 与 `a_2Fb` 不碰撞；不同 user／conversation／agentKey 分别隔离；同 key FIFO 串行、不同 key 可并行；超时映射为 `TIMEOUT`；异常后 lease 和无等待者的 key map 被清理；本地 lease fencing token 为空。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=InvocationIdentityResolverTest,LocalAgentInvocationGuardTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，新类型不存在。

- [ ] **Step 3: 实现 resolver、key 和 local guard**

`AgentInvocationKey.workspace(...)` 不含 agentKey；`state(...)` 必须包含 agentKey。多把锁由 coordinator 固定按 workspace → state 获取，失败时关闭已取得的 lease；释放时反序。分布式 Store 配合 `coordinationMode=NONE` 且 strict 时 validator 拒绝启动，非 strict 时告警。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=InvocationIdentityResolverTest,LocalAgentInvocationGuardTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/context \
  liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/guard \
  liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/exception \
  liteflow-react-agent/liteflow-react-agent-core/src/test
git commit -m "feat(agent): add invocation identity and guards"
```

---

## Task 3: 切换 BOM 并完成 StateStore／基础 runtime 纵切

**Files:**

- Modify: `pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSession.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/AgentSessionManager.java`
- Delete directory: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/session/factory`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/state/ResolvedAgentStateStore.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/state/AgentStateStoreResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/state/DefaultAgentStateStoreResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/state/GuardedNamespacedAgentStateStore.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/StateStoreFailureMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/runtime/AgentRuntimeHandle.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/runtime/AgentRuntimeBuildContext.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/runtime/ReActAgentRuntime.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/testsupport/ScriptedChatModel.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/state/GuardedNamespacedAgentStateStoreTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/middleware/StateStoreFailureMiddlewareTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/runtime/AgentRuntimeHandleTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentPlainTextTest.java`

- [ ] **Step 1: 写 StateStore、runtime ownership 和基础调用失败测试**

`ScriptedChatModel` 实现 2.0.2 的 `Model.stream(List<Msg>, List<ToolSchema>, GenerateOptions)`，记录模型调用次数、输入和 RuntimeContext 可见数据，并返回预设 `ChatResponse`。

测试断言：所有 StateStore 方法统一加／移除 agent namespace；`listSessionIds` 只返回本 agent 的 session；delegate 加载异常被记录并向上抛；AgentScope 吞掉加载异常后，最高优先级 Middleware 仍在模型前失败；严格模式模型调用次数为 0；宽松模式告警并清理后继续；runtime 并发初始化只构建一次，初始化失败可重试，close 幂等，关闭后禁止重建；普通文本回复写入 Slot。

- [ ] **Step 2: 运行测试并确认 1.x 依赖无法编译新代码**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=GuardedNamespacedAgentStateStoreTest,StateStoreFailureMiddlewareTest,AgentRuntimeHandleTest,ReActAgentPlainTextTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，新 2.0 API 和实现尚不存在。

- [ ] **Step 3: 一次性完成依赖切换和最小可编译纵切**

根 POM 把 `agentscope.version` 设为 `2.0.2` 并导入 `agentscope-bom`；core POM 将 aggregate `agentscope` 替换为无版本 `agentscope-core`，增加 Mockito／Reactor Test。

`GuardedNamespacedAgentStateStore` 实现 2.0.2 `AgentStateStore` 的全部单值／列表 save、get、getList、delete、exists、listSessionIds 与 close。装饰器 `close()` 不关闭 delegate；`ResolvedAgentStateStore.close()` 只关闭 `owned=true` 的底层 Store。故障通道提供：

```java
Optional<Throwable> takeLoadFailure(String userId, String runtimeSessionId);
void clearLoadFailure(String userId, String runtimeSessionId);
```

`DefaultAgentStateStoreResolver` 的映射固定为：MEMORY 创建 owned `InMemoryAgentStateStore`；JSON 使用配置的 `jsonRoot` 创建 owned `JsonFileAgentStateStore`；BEAN 通过 LiteFlow `ContextAware` 按 `beanName` 取得 borrowed `AgentStateStore`。Redis、MySQL 等连接参数不再复制进 LiteFlow 配置，均由自定义 Bean 自己管理。

`StateStoreFailureMiddleware` 在 AgentScope `beforeAgentExecution` 加载状态之后、任何 model／tool 调用之前检查该记录。

同时删除旧 Session manager/factory，并把 `ReActAgentComponent` 改为能完成一次 `UserMessage -> call(..., RuntimeContext) -> Slot` 的 2.0 最小纵切，确保此 Task 结束时模块可独立编译测试。

- [ ] **Step 4: 运行测试和依赖树**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=GuardedNamespacedAgentStateStoreTest,StateStoreFailureMiddlewareTest,AgentRuntimeHandleTest,ReActAgentPlainTextTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn dependency:tree -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -Dincludes=io.agentscope:* -Dverbose
```

Expected: PASS；依赖树只有 2.0.2 细粒度模块，无 `io.agentscope:agentscope`。

- [ ] **Step 5: 提交核心纵切**

```bash
git add pom.xml liteflow-react-agent/liteflow-react-agent-core
git commit -m "refactor(agent): migrate core runtime to AgentScope 2"
```

---

## Task 4: 提取 Abstract 组件、显式 RuntimeContext 与结构化输出

**Files:**

- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/AbstractAgentComponent.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/context/LiteFlowAgentContext.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/message/AgentOutputSpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/message/AgentReplyHandler.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentContext.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/AbstractAgentComponentTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentStructuredOutputTest.java`

- [ ] **Step 1: 写 process 顺序、context 隔离和结构化输出失败测试**

断言：type 与 schema 同时非空时模型调用前失败；TEXT 写字符串；JAVA_TYPE 写 `Msg.getStructuredData(type)`；JSON_SCHEMA 写 `JsonNode`；null reply 清空旧 responseData；每次调用得到不同 RuntimeContext；动态 prompt 只经 `transformSystemPrompt`；所有异常路径清理 Slot attachment；构建期方法在调用期 context 绑定前执行。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=AbstractAgentComponentTest,ReActAgentStructuredOutputTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，Abstract 模板和输出类型尚不存在。

- [ ] **Step 3: 实现 final process 模板**

固定顺序：验证配置 → 解析 identity → workspace lease → state lease → 绑定 Slot attachment → 构造 RuntimeContext → 获取 runtime → 创建 `UserMessage` → 按 `AgentOutputSpec` 调用 → 写回复 → 清理 attachment／故障记录 → 反序释放 lease。

`LiteFlowAgentContext` 保存原始 identity、安全 identity、Slot、chainId、nodeId、requestId、traceId、deadline、取消状态、chat usage 和 used skills。当前 LiteFlow 没有独立 traceId 时令 `traceId=requestId`，不再生成一个无关联 ID。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=AbstractAgentComponentTest,ReActAgentStructuredOutputTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src
git commit -m "feat(agent): add typed replies and explicit runtime context"
```

---

## Task 5: 迁移 Middleware、类型化事件和 usage 聚合

**Files:**

- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/AgentMiddlewareOrder.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/LiteFlowSystemPromptMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/ModelRoutingMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/FlowEventBridgeMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/ChatUsageMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/SkillTrackingMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/middleware/ReActLoggingMiddleware.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/event/AgentFlowEventData.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/event/AgentEventTypeMapper.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/middleware/MiddlewareOrderTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/middleware/ModelRoutingMiddlewareTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/middleware/FlowEventBridgeMiddlewareTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/middleware/ChatUsageMiddlewareTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/event/AgentEventTypeMapperTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentBuilderConfigurationTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActRetryFallbackTest.java`

- [ ] **Step 1: 写顺序、模型替换、事件和 listener 策略失败测试**

固定 order：state-store failure 10,000；logging 9,000；flow event 8,000；usage／skill 7,000；用户 Middleware 1,000。模型路由保留 messages／tools／options，只替换 model：

```java
next.apply(new ModelCallInput(
        input.messages(), input.tools(), input.options(), selectedModel));
```

事件测试覆盖 start/end、text/thinking delta、tool call/result start/delta/end、confirm required/result、result 和 error，并保留 reasoning／tool_result／summary／result 四个兼容事件名。listener 覆盖 `FAIL_FAST` 与 `LOG_AND_CONTINUE`；无 listener 时不发布。usage 跨多次 model call 聚合，每次 invocation 清零。

builder 配置测试固定 `maxIterations()`、`modelExecutionConfig()`、`toolExecutionConfig()`、`maxRetries()`、`fallbackModel()`、`permissionContext()`、`stopOnReject()` 和 `customizeAgent()` 的映射；customizer 必须最后执行。default／fallback／routing models 都纳入 runtime ownership，关闭时各关闭一次。

`ReActRetryFallbackTest` 使用计数 fake model：`maxRetries()` 必须映射到传给 `Model.stream(...)` 的 `GenerateOptions`，不在 fake 内重复测试 Provider 自己的重试实现；主模型失败后按 AgentScope 2.0.2 原生语义只切换一次 fallback；fallback 成功时返回其结果，fallback 也失败时传播 fallback 错误。AgentScope 2.0.2 的 `ReActAgent.modelForCall()` 会丢弃主错误而不附加 suppressed，本模块明确接受并刻画这一上游限制，不额外发明模型执行包装层。`ExecutionConfig` deadline 到期会取消 subscription、不会继续调用，并映射为 LiteFlow `TIMEOUT`。

- [ ] **Step 2: 运行测试并确认旧 Hook 无法满足断言**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest='*MiddlewareTest,AgentEventTypeMapperTest,ReActAgentBuilderConfigurationTest,ReActRetryFallbackTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 实现无共享可变状态的 Middleware**

每个 Middleware 只保存不可变策略，从 `RuntimeContext.get(LiteFlowAgentContext.class)` 读取调用数据。`AgentFlowEventData` 保存原始 `AgentEvent`、原始 user／conversation／agentKey，以及 chainId／nodeId／requestId／traceId／taskId／replyId。

`ModelRoutingMiddleware` 只能选择 runtime handle 已管理的 default／fallback／routing models；route 返回 null 或未管理 model 时在调用前失败，不得在请求路径 `ModelRegistry.resolve()`。

`ReActAgentComponent` 明确开放并映射 2.0 builder：

```java
protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder);
protected int maxIterations();
protected ExecutionConfig modelExecutionConfig();
protected ExecutionConfig toolExecutionConfig();
protected int maxRetries();
protected Model fallbackModel();
protected List<Model> routingModels();
protected PermissionContextState permissionContext();
protected boolean stopOnReject();
protected Model routeModel(Model defaultModel, LiteFlowAgentContext context);
```

分别调用 `.maxIters(...)`、`.modelExecutionConfig(...)`、`.toolExecutionConfig(...)`、`.maxRetries(...)`、`.fallbackModel(...)`、`.permissionContext(...)`、`.stopOnReject(...)`，最后调用 `customizeAgent(builder)`。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest='*MiddlewareTest,AgentEventTypeMapperTest,ReActAgentBuilderConfigurationTest,ReActRetryFallbackTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src
git commit -m "feat(agent): bridge AgentScope events with middleware"
```

---

## Task 6: 接入串行 Toolkit、RuntimeContext 工具和 MCP 生命周期

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/WorkspaceFileTools.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/ManagedShellCommandTool.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/runtime/McpClientRegistration.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/tool/GuardedWorkspacePathResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/ToolkitRuntimeTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/tool/GuardedWorkspacePathResolverTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/runtime/McpClientLifecycleTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/skill/AgentSkillRepositoryIntegrationTest.java`

- [ ] **Step 1: 写默认串行、工具注入、路径和 MCP ownership 失败测试**

断言 Toolkit 默认 `parallel=false`；工具从 RuntimeContext 读取 `LiteFlowAgentContext`；MCP 注册失败不发布半初始化 runtime；owned MCP 关闭一次，borrowed MCP 不关闭；Shell 默认不注册；绝对路径、`..`、symlink 逃逸被拒绝；文件写入限制 `maxFileBytes`。

Skills 测试使用内存 `AgentSkillRepository`：多个 repository 逐项注册；`SkillFilter` 按调用期 RuntimeContext 过滤；实际加载记录进入本次 `LiteFlowAgentContext.usedSkills`；owned repository 关闭一次，borrowed repository 不关闭；core 的 `skillCodeExecutionEnabled` 固定默认 false，不能绕过 Shell disabled 在宿主机执行代码。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ToolkitRuntimeTest,GuardedWorkspacePathResolverTest,McpClientLifecycleTest,AgentSkillRepositoryIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 实现构建期工具和 MCP 扩展点**

```java
protected List<Object> tools();
protected void customizeToolkit(Toolkit toolkit);
protected List<McpClientWrapper> mcpClients();
protected boolean ownsMcpClient(McpClientWrapper client);
protected List<AgentSkillRepository> skillRepositories();
protected boolean ownsSkillRepository(AgentSkillRepository repository);
protected SkillFilter skillFilter();
protected boolean dynamicSkillsEnabled();
protected boolean enableWorkspaceFileTools();
protected boolean enableShellTool();
```

Toolkit 显式构造：

```java
new Toolkit(ToolkitConfig.builder()
        .parallel(agentConfig().getToolkit().isParallel())
        .build());
```

`Toolkit.registerMcpClient(...)` 在 runtime timeout 内完成；任一注册失败时关闭已经创建的 owned 资源。Shell 默认 disabled。本地路径 resolver 必须校验绝对路径、真实路径、symlink、大小和 session root；文档仍将它标为可信本地防护，不称为沙箱。

每个 repository 使用 `.skillRepository(repository)` 逐项注册，并配置 `.skillFilter(...)`、`.dynamicSkillsEnabled(...)`。core 不自动启用 `.skillCodeExecutionEnabled(true)`；需要代码执行的 skill 放入 Harness Docker backend，或由用户通过受控自定义工具显式实现。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ToolkitRuntimeTest,GuardedWorkspacePathResolverTest,McpClientLifecycleTest,AgentSkillRepositoryIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src
git commit -m "feat(agent): secure toolkit and MCP lifecycle"
```

---

## Task 7: 实现同一租约内的 HITL continuation

**Files:**

- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hitl/AgentConfirmationHandler.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hitl/AgentConfirmationHandlerResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hitl/ConfirmationRequest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hitl/ConfirmationResultValidator.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hitl/ReActCallExecutor.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/hitl/ReActCallExecutorTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/hitl/HitlGuardConcurrencyTest.java`

- [ ] **Step 1: 写 allow、deny、异常、超时和插队失败测试**

覆盖：allow、显式 deny、未配置 handler、handler error／timeout、未知／缺失／重复 tool ID、replyId 不匹配、两次 call 之间第二请求不能插队、continuation 完成后同 session 可再次调用。handler 不能在首轮 event callback 内递归执行。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ReActCallExecutorTest,HitlGuardConcurrencyTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 实现两次完整调用事务**

固定流程：首轮完整结束 → 检查 `GenerateReason.PERMISSION_ASKING` → 比对事件 replyId 与 `Msg.METADATA_CONFIRM_REQUEST_REPLY_ID` → 提取 `ToolUseBlock` → handler 恰好覆盖全部 tool ID → 恢复消息只写 `Msg.METADATA_CONFIRM_RESULTS` → 使用同一 Agent、RuntimeContext 与输出模式 continuation。

TEXT、JAVA_TYPE、JSON_SCHEMA 首轮和 continuation 必须使用同一 `AgentOutputSpec`。未配置、超时或 handler 异常时先生成全拒绝结果执行 denial continuation 清 ASK 状态，再抛 `PERMISSION` 或 `TIMEOUT`。整个流程保持同一 workspace／state leases。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ReActCallExecutorTest,HitlGuardConcurrencyTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src
git commit -m "feat(agent): add guarded HITL continuation"
```

---

## Task 8: 完成资源生命周期、Spring 绑定和 1.x 清理

**Files:**

- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hook/ChatUsageTrackingHook.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/hook/ReActLoggingHook.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillTrackingHook.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillBoxFactory.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillLoadResult.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/SkillToolResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/runtime/AgentComponentLifecycleTest.java`
- Create: `liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java`
- Create: `liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java`
- Modify: `liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- Modify: `liteflow-spring-boot4-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/lifecycle/SolonAgentLifecycleTest.java`

- [ ] **Step 1: 写 close ownership 和属性绑定失败测试**

断言同一组件只有一个 runtime；close 顺序为 Agent → owned MCP → owned Store；borrowed Store／MCP 不关闭；多次 close 幂等；初始化中途失败时已创建资源反序关闭，关闭异常作为 suppressed；Spring Context close 调用组件 close；非 Spring 场景可显式 close；不存在静态 `AgentSessionManagerHolder`。

Spring Boot 2／4 绑定测试覆盖全部新 kebab-case 路径，并验证旧 `session.memory.*` 给出明确迁移异常。Solon 测试启动只使用 fake model 的 Agent 组件，关闭 `AppContext` 后断言同一组件的 runtime `close()` 恰好执行一次。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=AgentComponentLifecycleTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl liteflow-spring-boot-starter,liteflow-spring-boot4-starter -am \
  -DskipTests=false -DskipITs \
  -Dtest=AgentPropertyBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs \
  -Dtest=SolonAgentLifecycleTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，销毁接线／新属性 metadata／旧类清理尚未完成。

- [ ] **Step 3: 接入容器销毁并删除旧实现**

Spring／Solon 生命周期适配只调用组件公开 `close()`；不能通过反射清静态缓存。react-agent testcase POM 以 test scope 增加 `liteflow-solon-plugin` 与 `solon-test-junit5`，不让 core 依赖 Solon。删除旧 Hook、SkillBox factory 和 Session holder，技能只保留 2.0 `AgentSkillRepository`／filter 所需 resolver。

- [ ] **Step 4: 运行测试和旧 API 扫描**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core,liteflow-spring-boot-starter,liteflow-spring-boot4-starter -am \
  -DskipTests=false -DskipITs \
  -Dtest=AgentComponentLifecycleTest,AgentPropertyBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs \
  -Dtest=SolonAgentLifecycleTest \
  -Dsurefire.failIfNoSpecifiedTests=false
rg -n "AgentSessionManager|AgentSession|core\.hook\.|InMemoryMemory|SkillBox|StreamOptions" \
  liteflow-react-agent/liteflow-react-agent-core/src/main/java
```

Expected: 测试 PASS；扫描无命中。

- [ ] **Step 5: 提交生命周期清理**

```bash
git add liteflow-react-agent/liteflow-react-agent-core \
  liteflow-spring-boot-starter liteflow-spring-boot4-starter \
  liteflow-testcase-el/liteflow-testcase-el-react-agent
git commit -m "refactor(agent): finalize runtime lifecycle and remove v1 hooks"
```

---

## Task 9: 建立核心验收闸门

**Files:**

- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/component/ReActAgentCoreContractTest.java`

- [ ] **Step 1: 写跨能力核心契约测试**

一次固定：普通／POJO／Schema 回复；相同组件复用 runtime；不同组件实例不串 prompt／tool／state；同 state key 串行、不同 key 并行；同 user＋conversation 的不同 agentKey 状态隔离；相同 conversation 的 workspace 写由 workspace lease 串行；JSON Store 关闭重建后恢复；状态加载失败时模型调用为 0；listener 两种失败策略；timeout 取消 subscription 并释放 lease；全部异常路径清理 attachment 和故障记录。

- [ ] **Step 2: 运行新测试并修复真实集成缺口**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ReActAgentCoreContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 首次运行暴露各子能力组合缺口；只修复测试证明的缺口，不扩展范围。

- [ ] **Step 3: 运行核心 clean test、依赖树和 package**

Run:

```bash
mvn clean test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs
mvn dependency:tree -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -Dincludes=io.agentscope:* -Dverbose
mvn package -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests
```

Expected: 全部通过；依赖树只有 AgentScope 2.0.2；测试没有 credential skip。

- [ ] **Step 4: 提交核心验收测试**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/test
git commit -m "test(agent): establish deterministic AgentScope 2 core suite"
```

---

## Completion Criteria

- core 只依赖 `agentscope-core` 2.0.2，旧 aggregate／Session／Hook／Memory／SkillBox 主路径全部移除。
- 同一组件 runtime 可服务多个调用，所有调用期状态经 RuntimeContext 传递；不同实际组件实例互不污染。
- AgentStateStore namespace、防吞错 Middleware 与 guard 的确定性测试全部通过；加载失败时模型调用次数严格为 0。
- 普通、POJO、JSON Schema、Middleware、类型化事件、usage、MCP、工具和 HITL 均有离线测试。
- HITL 两次调用处于同一租约，拒绝／异常／超时先执行 denial continuation 清理状态。
- Spring／Spring Boot 4 和非容器生命周期均有明确 close 行为；所有异常路径释放资源和 Slot attachment。
