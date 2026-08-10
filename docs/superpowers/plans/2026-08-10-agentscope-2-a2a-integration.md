# AgentScope 2.0 A2A, Integration, and Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在核心运行时、Provider 与 Harness 迁移完成后，增加隔离的 A2A 可选模块，重写 React Agent 集成测试与用户文档，并用完整依赖审计和 JDK 17 构建完成 AgentScope 2.0.2 升级验收。

**Architecture:** A2A 客户端因 AgentScope 2.0.2 的上游实现仍持有单次请求可变状态，每次 LiteFlow 调用创建独立 `A2aAgent`；A2A 服务端从 `ReActAgent.streamEvents(..., RuntimeContext)` 获取类型化事件，仅在上游 `AgentRunner` 边界转换为旧粗粒度 `Event`。集成测试默认全部离线、确定性执行，真实 Provider 与真实 Docker 分别放入显式 profile。

**Tech Stack:** Java 17、Maven、LiteFlow、AgentScope Java 2.0.2、A2A Java SDK、Reactor、JUnit 5、Mockito、Spring Boot Test。

## Global Constraints

- 本计划在 `2026-08-10-agentscope-2-core-runtime.md`、`2026-08-10-agentscope-2-providers.md` 与 `2026-08-10-agentscope-2-harness-sandbox.md` 全部完成后执行。
- 以 `docs/superpowers/specs/2026-08-10-agentscope-2-upgrade-design.md` 为唯一设计基线；若实现发现设计与 2.0.2 源码不符，先补测试和更新设计文档，再修改生产代码。
- A2A 上游边界是唯一允许接触 `io.agentscope.core.agent.Event` 的位置；使用点必须集中在 `A2aProtocolEventAdapter`，并写明删除条件。LiteFlow 自身的运行时、Middleware 和事件桥不得新增旧 Hook／Event API。
- A2A 客户端不得缓存或并发复用 `A2aAgent`。2.0.2 源码中的 `currentRequestId`、`clientEventContext` 和 `a2aClient` 是实例字段，上游 JavaDoc 明确说明一个实例不能同时执行多个线程／任务。
- 默认测试不得访问公网、真实模型或真实密钥。真实 Provider 测试只在 `agent-live` profile 中运行；真实 Docker 测试只在 `agent-docker-it` profile 中运行。
- 所有 Maven 测试命令显式携带 `-DskipTests=false`；使用 `-Dtest=...` 的 reactor 命令同时携带 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 不提交生成的 `.flattened-pom.xml`、日志、状态文件、workspace 内容或测试密钥。

---

## Task 1: 新增每次调用独立实例的 A2A 客户端模块

**Files:**

- Modify: `liteflow-react-agent/pom.xml`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/pom.xml`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/A2aAgentComponent.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/A2aClientRuntime.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/A2aClientRuntimeFactory.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/A2aAgentComponentTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/A2aClientRuntimeTest.java`

- [ ] **Step 1: 写出客户端生命周期失败测试**

测试使用注入的 `A2aClientRuntimeFactory` 记录创建次数，不启动 HTTP 服务。连续两次执行同一组件时断言：

```java
assertThat(factory.createdAgents()).isEqualTo(2);
assertThat(factory.maxConcurrentCallsPerAgent()).isEqualTo(1);
assertThat(replies).containsExactly("reply-1", "reply-2");
assertThat(component.lastContext().conversationId()).isEqualTo("conversation-7");
```

再增加并发测试：两个不同 conversation 同时执行时允许并行，但每个调用拿到不同 `A2aAgent`；超时、远端错误和取消时 `AgentInvocationLease` 均被释放。

- [ ] **Step 2: 运行测试并确认模块尚不存在**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-a2a -am \
  -DskipTests=false -DskipITs \
  -Dtest=A2aAgentComponentTest,A2aClientRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: Maven 因模块／类不存在而失败。

- [ ] **Step 3: 创建细粒度依赖与客户端执行边界**

`liteflow-react-agent-a2a/pom.xml` 只依赖 `liteflow-react-agent-core` 与：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-a2a-client</artifactId>
</dependency>
```

`A2aClientRuntime` 是组件级、无调用状态的 factory runtime；它的稳定契约为：

```java
public interface A2aClientRuntime extends AutoCloseable {
    Mono<Msg> call(A2aClientRequest request);
    @Override default void close() { }
}

public record A2aClientRequest(
        String remoteAgentName,
        AgentCardResolver resolver,
        A2aAgentConfig config,
        UserMessage message,
        Duration timeout,
        LiteFlowAgentContext context) { }
```

默认 runtime 在 `Mono.defer` 内执行 `A2aAgent.builder().name(...).agentCardResolver(...).a2aAgentConfig(...).build()`，把 LiteFlow 的 `userId`、`conversationId`、`agentKey`、`traceId` 放入 `UserMessage.metadata`，并在每次订阅时创建新实例。超时或取消必须调用该实例的 `interrupt()`；完成或失败后不能把实例保存到字段或缓存。runtime 自身只持有不可变 resolver／config 和资源 ownership 信息。

- [ ] **Step 4: 实现 `A2aAgentComponent` 的 final 模板方法**

组件直接复用 core 的 final `process()`、身份解析、`LiteFlowAgentContext`、状态租约、超时和异常映射，但不伪装成 A2A 上游会消费 `RuntimeContext`：

```java
public abstract class A2aAgentComponent
        extends AbstractAgentComponent<A2aClientRuntime> {
    @Override
    protected A2aClientRuntime buildRuntime(
            AgentRuntimeBuildContext buildContext);

    @Override
    protected Mono<Msg> invokeRuntime(
            A2aClientRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext);

    protected abstract String remoteAgentName();
    protected abstract AgentCardResolver agentCardResolver();
    protected A2aAgentConfig a2aAgentConfig() {
        return A2aAgentConfig.builder().build();
    }
    protected abstract String userPrompt(LiteFlowAgentContext context);
}
```

`invokeRuntime` 只支持 `AgentOutputSpec.Kind.TEXT`；A2A 2.0.2 客户端没有 LiteFlow 本地结构化输出契约，其他 kind 必须在发送远端请求前失败。它从 `runtimeContext`／`liteflowContext` 提取允许透传的 metadata，再交给 `A2aClientRuntime.call(...)`。不得在组件字段中保存 `Slot`、`LiteFlowAgentContext`、原始 ID 或调用中的 `A2aAgent`。

- [ ] **Step 5: 运行客户端测试与模块编译**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-a2a -am \
  -DskipTests=false -DskipITs \
  -Dtest=A2aAgentComponentTest,A2aClientRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS；测试证明同一组件从未跨调用复用 `A2aAgent`。

- [ ] **Step 6: 提交客户端模块**

```bash
git add liteflow-react-agent/pom.xml \
  liteflow-react-agent/liteflow-react-agent-a2a/pom.xml \
  liteflow-react-agent/liteflow-react-agent-a2a/src
git commit -m "feat(agent): add isolated A2A client component"
```

---

## Task 2: 用类型化事件实现 A2A 服务端适配边界

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-a2a/pom.xml`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/server/A2aServerAgentFactory.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/server/LiteFlowA2aAgentRunner.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/server/A2aProtocolEventAdapter.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/server/LiteFlowA2aServerFactory.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/server/LiteFlowA2aAgentRunnerTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/test/java/com/yomahub/liteflow/agent/a2a/server/A2aProtocolEventAdapterTest.java`

- [ ] **Step 1: 写类型化执行和取消语义测试**

用 `A2aServerAgentFactory` 的假实现返回可记录的 `ReActAgent`，断言：

- `AgentRequestOptions.userId/sessionId/taskId` 被转换为 `RuntimeContext`；
- 输入 `Msg.metadata` 只允许透传 `liteflow.traceId` 与 `liteflow.tenantId`，未知键和 credential 键不会进入 RuntimeContext；
- 执行调用 `streamEvents(messages, runtimeContext)`，而不是已弃用的 `stream(...)`；
- 每个 taskId 有独立执行句柄，重复 taskId 立即失败；
- `stop(taskId)` 只中断对应任务；终止信号会删除句柄并关闭本次创建的 Agent；
- `TextDeltaEvent`、最终 `AgentEndEvent`、错误事件能转换为 A2A 所需的粗粒度流；
- `A2aProtocolEventAdapter` 之外没有旧 `Event` import。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-a2a -am \
  -DskipTests=false -DskipITs \
  -Dtest=LiteFlowA2aAgentRunnerTest,A2aProtocolEventAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，服务端适配类尚不存在。

- [ ] **Step 3: 增加 server 依赖和每任务 runtime 工厂**

增加非传递到 core 的依赖：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-a2a-server</artifactId>
    <optional>true</optional>
</dependency>
```

server 依赖标为 optional，使用客户端组件的应用不会被传递 A2A server／transport 依赖；使用 `LiteFlowA2aServerFactory` 的应用必须显式加入该 artifact。

工厂契约必须把资源所有权写清楚：

```java
public interface A2aServerAgentFactory {
    String agentName();
    String agentDescription();
    OwnedAgentRuntime open(AgentRequestOptions options);

    interface OwnedAgentRuntime extends AutoCloseable {
        Flux<AgentEvent> stream(List<Msg> messages);
        void interrupt();
    }
}
```

`LiteFlowA2aAgentRunner` 使用 core `InvocationIdentityResolver`：把远端 `AgentRequestOptions.userId/sessionId` 当作原始 userId／conversationId，经应用 namespace 和公开的 server agentKey 生成安全 `runtimeSessionId`；缺失 userId 使用配置默认值，缺失 sessionId 立即拒绝，不生成无法继续的随机远端会话。`RuntimeContext.userId` 使用业务 userId，`sessionId` 使用哈希后的 runtime ID。消息 metadata 采用固定 allowlist，只映射 `liteflow.traceId` 与 `liteflow.tenantId`，不得透传 apiKey、authorization 或任意对象。

默认实现为每个 task 创建 Agent 调用句柄，但可以复用由容器拥有的 Model、Toolkit、StateStore 和 MCP Client；`close()` 只关闭本句柄拥有的 Agent，不能关闭注入的共享资源。

- [ ] **Step 4: 实现唯一的旧事件适配点**

`LiteFlowA2aAgentRunner implements AgentRunner` 只处理 task registry、取消和生命周期。所有 `AgentEvent -> Event` 转换集中在：

```java
@SuppressWarnings("deprecation")
final class A2aProtocolEventAdapter {
    Flux<io.agentscope.core.agent.Event> adapt(Flux<AgentEvent> source) {
        // typed delta/final/error -> A2A server 2.0.2 required coarse wire events
    }
}
```

适配器不能把确认请求自动批准；遇到 `RequireUserConfirmEvent` 时应终止并返回清晰的“不支持跨边界内联确认”错误，除非调用方配置了显式的远程确认协议。

- [ ] **Step 5: 创建不绑定 Web 框架的 server factory**

`LiteFlowA2aServerFactory` 使用真实 2.0.2 API；这里的 `agentCard` 类型是
`io.agentscope.core.a2a.server.card.ConfigurableAgentCard`，`transportProperties` 类型是
`io.agentscope.core.a2a.server.transport.TransportProperties`：

```java
AgentScopeA2aServer server = AgentScopeA2aServer.builder(agentRunner)
        .agentCard(agentCard)
        .withTransport(transportProperties)
        .build();
```

factory 返回 `AgentScopeA2aServer`；endpoint、端口和 Controller 由业务 Web 框架负责。只有 Web endpoint 已监听后，调用方才能调用 `postEndpointReady()`。

- [ ] **Step 6: 运行服务端适配测试并扫描弃用边界**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-a2a -am \
  -DskipTests=false -DskipITs \
  -Dtest=LiteFlowA2aAgentRunnerTest,A2aProtocolEventAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false
rg -n "core\.agent\.(Event|EventType)|core\.hook\.Hook" \
  liteflow-react-agent/liteflow-react-agent-a2a/src/main/java
```

Expected: 测试 PASS；`rg` 只命中 `A2aProtocolEventAdapter.java` 中有说明的协议边界，不命中 Hook。

- [ ] **Step 7: 提交服务端适配**

```bash
git add liteflow-react-agent/liteflow-react-agent-a2a
git commit -m "feat(agent): expose typed runtime through A2A"
```

---

## Task 3: 把 testcase 模块改造成离线确定性集成套件

**Files:**

- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/support/BaseAgentLiveTest.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/support/LiveTestSupport.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/support/ScriptedChatModel.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/support/AgentTestEvents.java`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/hook`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/memorypersistence`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/sessionreuse`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/streaming`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/skills`
- Replace directory: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/shelltool`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/runtimecontext/RuntimeContextIsolationTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/structuredoutput/StructuredOutputChainTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/hitl/HitlChainTest.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/harness/HarnessComponentTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/a2a/A2aAgentCmp.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/feature/a2a/A2aChainTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/runtimecontext/application.properties`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/runtimecontext/flow.el.xml`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/structuredoutput/application.properties`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/structuredoutput/flow.el.xml`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/hitl/application.properties`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/hitl/flow.el.xml`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/harness/application.properties`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/harness/flow.el.xml`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/a2a/application.properties`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/feature/a2a/flow.el.xml`

- [ ] **Step 1: 先写四条 Spring 集成失败测试**

使用 `ScriptedChatModel` 和 fake `A2aClientRuntime`，固定模型输出和类型化事件。五条主链分别断言：

1. 同一组件处理两个 conversation 时 Agent 实例只创建一次，但 `RuntimeContext` 和状态完全隔离；
2. POJO 与 JSON Schema 回复写入 Slot 的是结构化对象，空回复覆盖旧值为 `null`；
3. HITL 在首轮结束后调用 handler，再以只含 `Msg.METADATA_CONFIRM_RESULTS` 的消息 continuation，租约贯穿两次调用；
4. Harness 的 workspace、skill、plan、subagent 和 permission state 从同一 `LiteFlowAgentContext` 路由。
5. A2A 节点复用 core final process，但每次调用创建新的上游 `A2aAgent`；远端错误映射为 LiteFlow 节点失败，TEXT 回复进入 Slot。

- [ ] **Step 2: 运行新测试并确认旧 API 无法满足断言**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs \
  -Dtest=RuntimeContextIsolationTest,StructuredOutputChainTest,HitlChainTest,HarnessComponentTest,A2aChainTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，新测试支持类或迁移后的 Spring 组件尚未完成。

- [ ] **Step 3: 增加 Harness／A2A 测试依赖并重写旧语义测试**

测试 POM 增加 `liteflow-react-agent-harness` 与 `liteflow-react-agent-a2a`。将旧目录按下表改名并重写断言，不能仅用 `@Disabled` 跳过：

| 旧目录 | 新语义 |
|---|---|
| `hook` | `middleware`：固定 `order()` 与异常传播 |
| `memorypersistence`、`sessionreuse` | `statestore`：状态 namespace、多轮延续、加载失败 fail-before-model |
| `streaming` | `events`：类型化 delta／tool／confirm／result／error |
| `skills` | `harnessskills`：`AgentSkillRepository` 与实际使用追踪 |
| `shelltool` | `sandbox`：guarded local 与 Docker profile，不把黑名单称为沙箱 |

保留仍有价值的 `basicchain`、`ifrouting`、`whenparallel`、`multiagent`、`customtool`、`springbeantool`、`conversationid`、`agentkey`、`handlereply`、`chatusage` 和 `maxiterations` 测试，但把组件签名和断言升级为新 API。

- [ ] **Step 4: 让实时 Provider 测试只在显式 profile 中运行**

POM 配置：

- 默认 surefire 排除 `**/platform/**/*Test.java` 和 `**/*LiveTest.java`；
- `agent-live` profile 重新包含 Provider 测试，并继续用环境变量判断单个 Provider 是否具备密钥；
- 默认离线测试中不得调用 `Assumptions.assumeTrue(apiKey != null)` 来伪装覆盖率。

- [ ] **Step 5: 运行完整离线 testcase 套件**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs
```

Expected: PASS；没有网络连接、没有真实密钥也能覆盖核心、Provider builder、Harness 与 A2A 适配契约。

- [ ] **Step 6: 提交确定性集成测试**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent
git commit -m "test(agent): migrate integration suite to AgentScope 2"
```

---

## Task 4: 重写用户指南和破坏式迁移说明

**Files:**

- Modify: `docs/liteflow-react-agent-guide.md`
- Create: `docs/liteflow-react-agent-agentscope-2-migration.md`
- Modify: `README.md`
- Modify: `README.zh-CN.md`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java`
- Create: `liteflow-react-agent/liteflow-react-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/package-info.java`
- Create: `liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/package-info.java`

- [ ] **Step 1: 写文档契约检查并确认当前指南命中旧概念**

Run:

```bash
rg -n "1\.0\.12|SessionManager|AgentSession|SkillBox|hooks\(\)|JDK 21\+|黑名单.*沙箱" \
  docs/liteflow-react-agent-guide.md README.md README.zh-CN.md
```

Expected: 命中旧会话、Hook、SkillBox、旧 JDK 要求或错误沙箱表述。

- [ ] **Step 2: 重写主指南**

`docs/liteflow-react-agent-guide.md` 至少包含可编译示例和以下章节：

- JDK 17、BOM 与 core／Provider／Harness／A2A 的依赖选择；
- `AbstractAgentComponent`、`ReActAgentComponent` 和 `LiteFlowAgentContext`；
- 构建期扩展点与调用期扩展点，明确构建期不得读取 Slot；
- `RuntimeContext`、`userId`、`conversationId`、`agentKey` 与状态 namespace；
- `AgentStateStore` Bean、JSON 本地开发 Store 和多副本协调要求；
- 结构化输出、重试、fallback、MCP 和显式串行 Toolkit；
- Middleware 顺序、类型化事件、usage、skills tracking；
- HITL 的两次完整调用及拒绝／超时策略；
- Harness workspace、compaction、memory、skills、`List<SubagentDeclaration>`、task／plan；
- guarded local、remote filesystem 与 Docker sandbox 的安全等级表；
- A2A 客户端每调用一实例、server endpoint 由业务框架提供，以及 2.0.2 的旧事件边界限制；
- Spring／Solon 销毁和非容器显式 `close()`。

- [ ] **Step 3: 写 1.x 到 2.0.2 迁移表**

`docs/liteflow-react-agent-agentscope-2-migration.md` 必须提供下列直接映射：

| 1.x | 2.0.2 |
|---|---|
| `ReActAgentContext`／隐式 `ctx()` | 显式 `LiteFlowAgentContext` 参数 |
| `hooks()`／`Hook` | `middlewares()`／`MiddlewareBase` |
| `AgentSessionManager`／Memory factory | `AgentStateStore` Bean 与 namespaced decorator |
| `session.memory.*` | `runtime.*` 与 `state-store.*` |
| `SkillBox` | `AgentSkillRepository`／Harness skills |
| 每会话 Agent 缓存 | 组件级无状态 runtime + 每调用 `RuntimeContext` |
| workspace 黑名单 Shell | guarded local 或 Docker sandbox |

同时列出被删除的配置项、旧方法签名、最小迁移前后代码和不兼容原因。

- [ ] **Step 4: 修正 README 和 package JavaDoc**

README 必须把“JDK 21+”修正为该模块实际验证的 JDK 17，并把能力描述更新为“轻量 ReAct + 可选 Harness／sandbox”，不宣称本地 path namespace 是安全沙箱。

- [ ] **Step 5: 运行文档扫描**

Run:

```bash
rg -n "SessionManager|AgentSession|SkillBox|hooks\(\)|JDK 21\+|1\.0\.12" \
  docs/liteflow-react-agent-guide.md README.md README.zh-CN.md \
  liteflow-react-agent/*/src/main/java
```

Expected: 生产 Java 与主指南无旧实现引用；迁移指南中允许出现旧名，但必须位于迁移表或删除说明中。

- [ ] **Step 6: 提交文档**

```bash
git add docs/liteflow-react-agent-guide.md \
  docs/liteflow-react-agent-agentscope-2-migration.md \
  README.md README.zh-CN.md \
  liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/package-info.java \
  liteflow-react-agent/liteflow-react-agent-harness/src/main/java/com/yomahub/liteflow/agent/harness/package-info.java \
  liteflow-react-agent/liteflow-react-agent-a2a/src/main/java/com/yomahub/liteflow/agent/a2a/package-info.java
git commit -m "docs(agent): document AgentScope 2 runtime and sandbox"
```

---

## Task 5: 做依赖、弃用 API 和完整构建验收

**Files:**

- Modify if findings require a fix: `pom.xml`
- Modify if findings require a fix: `liteflow-react-agent/pom.xml`
- Modify if findings require a fix: `liteflow-react-agent/liteflow-react-agent-core/pom.xml`
- Modify if findings require a fix: `liteflow-react-agent/liteflow-react-agent-harness/pom.xml`
- Modify if findings require a fix: `liteflow-react-agent/liteflow-react-agent-a2a/pom.xml`
- Modify if findings require a fix: `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`

- [ ] **Step 1: 验证 AgentScope 依赖图**

Run:

```bash
mvn dependency:tree -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -Dincludes=io.agentscope:*,io.github.a2asdk:* -Dverbose
```

Expected:

- 所有 AgentScope artifact 均由 BOM 收敛到 `2.0.2`；
- 不存在 `io.agentscope:agentscope` shaded 聚合包；
- 不存在 `1.0.12` 或任何 1.x AgentScope artifact；
- Provider SDK 只由对应 Provider 模块传递；
- Harness 与 A2A 不被 core 传递给只使用轻量 ReAct 的项目。

- [ ] **Step 2: 扫描旧 API 和错误安全表述**

Run:

```bash
rg -n "core\.hook\.|SessionManager|AgentSession|InMemoryMemory|SkillBox|StreamOptions" \
  liteflow-react-agent/*/src/main/java
rg -n "core\.agent\.(Event|EventType)" liteflow-react-agent/*/src/main/java
```

Expected: 第一条无命中；第二条只命中 `A2aProtocolEventAdapter.java`。

- [ ] **Step 3: 运行所有离线 Agent 测试**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs
```

Expected: PASS，且测试摘要中没有因缺少密钥而跳过的核心／Harness／A2A 测试。

- [ ] **Step 4: 在 JDK 17 完成模块打包**

Run:

```bash
java -version
mvn package -pl liteflow-react-agent,liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs
```

Expected: `java -version` 为 17；所有相关模块编译、测试和打包成功。

- [ ] **Step 5: 显式执行 Docker 与 live smoke profile**

仅在环境具备条件时运行；缺少条件时在交付报告中标记“未执行”，不得伪装成通过：

```bash
mvn verify -pl liteflow-react-agent/liteflow-react-agent-harness -am \
  -Pagent-docker-it -DskipTests=false
mvn verify -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -Pagent-live -DskipTests=false
```

Docker profile 验证真实容器创建、workspace 隔离、命令执行、资源限制和回收；live profile 至少选择一个已配置 Provider 验证真实请求。

- [ ] **Step 6: 检查工作树和补充最终提交**

Run:

```bash
git diff --check
git status --short
```

若前五步产生修复：

```bash
git add pom.xml liteflow-react-agent liteflow-testcase-el/liteflow-testcase-el-react-agent docs README.md README.zh-CN.md
git commit -m "chore(agent): complete AgentScope 2 migration audit"
```

Expected: `git diff --check` 无输出；工作树只保留用户原有的无关改动，或完全干净。

---

## Completion Criteria

- A2A 客户端同一实例并发风险被每调用实例策略消除；服务端只在单一协议边界使用旧粗粒度 Event。
- 默认测试无需网络／密钥，覆盖 RuntimeContext、状态隔离、结构化输出、Middleware、类型化事件、HITL、Harness 与 A2A。
- 文档准确区分 guarded local 与真正 Docker sandbox，并给出 1.x 破坏式迁移表。
- AgentScope 依赖全部收敛到 2.0.2，core 不传递 Harness、A2A 或 Provider SDK。
- JDK 17 的相关模块 package 成功；具备环境时 Docker 与 live profile 也通过。
