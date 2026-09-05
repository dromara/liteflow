# liteflow-agent 升级 AgentScope 2.0.2 设计

- **日期**：2026-08-10
- **状态**：已批准
- **目标版本**：AgentScope Java 2.0.2
- **作用范围**：`liteflow-agent`、`liteflow-core` 中的 Agent 配置、`liteflow-testcase-el-agent`、相关集成与文档
- **迁移类型**：允许破坏式升级

## 1. 背景

`liteflow-agent` 当前依赖 AgentScope 1.0.12，并围绕 1.x 的有状态 `ReActAgent`、`Memory`、`SessionManager`、`Hook`、`SkillBox` 和粗粒度 `Event` 构建了自己的会话与运行时体系。

AgentScope 2.0 已将核心模型改为：

- Agent 实例无状态，可作为单例服务多个用户和会话；
- 每次调用通过 `RuntimeContext` 传递用户、会话和依赖；
- 会话状态由 `AgentState` 与 `AgentStateStore` 管理；
- `MiddlewareBase` 取代旧 Hook，成为主要横切扩展机制；
- `AgentEvent` 与 `streamEvents()` 提供类型化的完整执行事件；
- 模型实现拆分到独立的 `agentscope-extensions-model-*` 模块；
- `HarnessAgent` 在 `ReActAgent` 之上组合工作区、上下文压缩、长期任务记忆、技能、子 Agent、沙箱、权限和计划模式。

这意味着升级不能通过修改版本号完成。当前模块的会话缓存、锁、持久化工厂、Hook、消息构造、事件桥和模型依赖都需要重新设计。

官方依据：

- [AgentScope 2.0 迁移指南](https://java.agentscope.io/v2/zh/docs/change-log.html)
- [Agent 与 RuntimeContext](https://java.agentscope.io/v2/zh/docs/building-blocks/agent.html)
- [Harness 架构](https://java.agentscope.io/v2/zh/docs/harness/architecture.html)
- [AgentScope Java v2.0.2](https://github.com/agentscope-ai/agentscope-java/tree/v2.0.2)

## 2. 目标

本次升级必须同时实现以下目标：

1. 将 AgentScope 依赖锁定到正式版 `2.0.2`，全面使用稳定的 2.x API。
2. 用无状态 Agent、`RuntimeContext` 和 `AgentStateStore` 取代自定义会话 Agent 缓存与 1.x Session 体系。
3. 保留轻量 `ReActAgent` 集成，同时新增可选的完整 `HarnessAgent` 集成。
4. 使用类型化消息、Middleware 和 AgentEvent，不在新增代码中继续依赖 2.x 中已标记待移除的兼容 API。
5. 充分开放 AgentScope 2.0 的结构化输出、重试与降级、MCP、权限与 HITL、技能、子 Agent、计划模式、上下文压缩和长期任务工作区能力。
6. 保持 LiteFlow 作为确定性流程控制面；AgentScope 负责自然语言理解、受控决策和工具选择，不用 Agent 循环替代 Chain／EL 图。
7. 为核心行为建立无网络、无真实密钥的确定性测试基线。
8. 明确 JDK 17、依赖版本、线程安全、生命周期和安全边界。

## 3. 非目标

本次升级不包含：

1. 对 AgentScope 1.0.12 类型的二进制兼容。旧 `Hook`、`Session`、`Memory`、`SkillBox` 等类型可以从公开扩展点移除。
2. 自动把所有 LiteFlow Chain 或 `NodeComponent` 暴露为 Agent 工具。业务可显式注册受控工具，未来可另行设计 allowlist 桥接。
3. 用 AgentStateStore 保存业务事实或替代业务数据库。
4. 在第一阶段实现跨进程持久化的 LiteFlow 整条 Chain 暂停／恢复。HITL 先通过确认处理器和事件接口接入。
5. 依赖 AgentScope 中已经标记 `forRemoval=true` 的旧 RAG／LongTermMemory builder。需要检索时使用独立适配层或显式工具。
6. 为 AgentScope 每一个 builder 参数复制一份 LiteFlow 配置。高级能力通过 builder customizer 和容器 Bean 开放。

## 4. 版本与依赖决策

### 4.1 版本

目标版本固定为无限定符的 `2.0.2`。

不使用 `2.0.2-subagent-bugfix` 作为通用依赖。该版本是面向 Agent Protocol SSE 事件总线的限定版本，并非新的标准维护版本。若未来确实依赖对应修复，应通过独立变更评估，而不是让 Maven 的 `LATEST`／`RELEASE` 自动选择。

### 4.2 BOM 与细粒度模块

根 POM 使用：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-bom</artifactId>
            <version>${agentscope.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

各模块按需依赖：

| LiteFlow 模块 | AgentScope 依赖 |
|---|---|
| `liteflow-agent-core` | `agentscope-core` |
| `liteflow-agent-harness` | `agentscope-harness` |
| `liteflow-agent-a2a` | `agentscope-extensions-a2a-client`，服务端能力按需依赖 `agentscope-extensions-a2a-server` |
| `liteflow-agent-openai` | `agentscope-extensions-model-openai`，包含其 first-party compatible ModelProvider |
| `liteflow-agent-anthropic` | `agentscope-extensions-model-anthropic` |
| `liteflow-agent-gemini` | `agentscope-extensions-model-gemini` |
| `liteflow-agent-dashscope` | `agentscope-extensions-model-dashscope` |

不得同时依赖 shaded 聚合包 `io.agentscope:agentscope` 与细粒度模块，避免重复类和不完整的传递依赖。

### 4.3 Java 与基础依赖

- 模块最低运行时为 JDK 17。
- 不沿用旧文档中的“JDK 21+”描述，除非后续发现具体功能确实需要更高版本。
- AgentScope 2.0.2 引入的 Reactor、Jackson、SLF4J、OkHttp、OpenTelemetry、MCP、SQLite 和 native 依赖必须做依赖树与运行时验证。
- 不为了迎合旧 Spring Boot BOM，未经验证地单独降级 Reactor 或某一个 Jackson 模块。

## 5. 总体架构

```text
LiteFlow Chain／EL
        │
        ▼
AbstractAgentComponent.process() 〔final〕
        │
        ├── InvocationIdentityResolver
        │     ├── userId
        │     ├── conversationId
        │     └── agentKey
        │
        ├── AgentInvocationGuard
        │     └── 覆盖首轮 call／HITL／continuation／保存
        │
        ├── RuntimeContextFactory
        │     ├── LiteFlowAgentContext
        │     ├── Slot／trace／listener
        │     └── AgentStateStore 路由信息
        │
        ├── AgentRuntimeHandle 〔组件实例拥有〕
        │     ├── AgentRuntime
        │     ├── HarnessAgentRuntime
        │     └── A2aAgentRuntime〔可选〕
        │
        ├── Middleware
        │     ├── StateStoreFailureMiddleware
        │     ├── SystemPrompt／ModelRouting Middleware
        │     ├── FlowEventBridgeMiddleware
        │     ├── ChatUsageMiddleware
        │     ├── SkillTrackingMiddleware
        │     └── Logging／Tracing Middleware
        │
        └── ReplyHandler
              ├── 文本回复
              └── 结构化回复
```

### 5.1 控制面边界

- LiteFlow 负责顺序、并行、重试、超时、分支、回滚和业务生命周期。
- AgentScope 负责一次 Agent 节点内部的模型推理、工具选择、权限检查、子 Agent 和上下文管理。
- Harness 的 plan mode 是 Agent 内部的任务计划，不替代 LiteFlow Chain。
- Agent 调用失败应转换为清晰的 LiteFlow 节点异常，由现有 LiteFlow 错误处理语义接管。

## 6. 模块结构

升级后的模块结构为：

```text
liteflow-agent/
├── liteflow-agent-core
├── liteflow-agent-harness        # 新增，可选
├── liteflow-agent-a2a            # 新增，可选
├── liteflow-agent-openai
├── liteflow-agent-anthropic
├── liteflow-agent-gemini
└── liteflow-agent-dashscope
```

### 6.1 `liteflow-agent-core`

负责：

- `AbstractAgentComponent` 与 `AgentComponent`；
- LiteFlow Slot 与 RuntimeContext 桥接；
- AgentStateStore namespace／故障通道与资源所有权；
- AgentInvocationGuard 与 HITL 外层事务边界；
- Toolkit、Middleware、消息、事件和异常抽象；
- 模型公共接口和 Provider Spec 契约；
- 轻量 Agent 能力。

core 只依赖 `agentscope-core`，不传递 Harness 的完整依赖面。

### 6.2 `liteflow-agent-harness`

负责：

- `HarnessAgentComponent`；
- workspace／filesystem／sandbox；
- compaction／memory；
- skills repository；
- subagents／task repository／message bus；
- plan mode；
- Harness 权限与高风险工具配置。

### 6.3 Provider 模块

Provider 模块保持薄适配：

- 使用 AgentScope 2.0.2 的原生模型 extension；
- 保留 `ModelSpec` 与 `buildModel()` 逃生口；
- 支持显式模型 builder 和 AgentScope 模型字符串 ID；
- 不在 core 中引入厂商 SDK；
- DeepSeek、GLM、Kimi、MiniMax 优先使用 AgentScope OpenAI extension 内置的 first-party `ModelProvider`，LiteFlow 不再自行重复维护固定 baseUrl preset；通用 OpenAI Compatible builder 仅作为自定义端点的后备。

### 6.4 `liteflow-agent-a2a`

A2A 是独立的跨服务 Agent 协议，不等同于本地 subagent 或 Agent Protocol。可选模块负责：

- 提供基于 `A2aAgent` 的远程 Agent 节点适配；
- 透传 RuntimeContext 中允许跨服务传递的 trace／tenant 元数据；
- 将远程任务状态和事件桥接为 LiteFlow FlowEvent；
- 提供 `AgentScopeA2aServer` 与 LiteFlow Agent runtime 的服务端适配示例；
- 不在 core 中传递 A2A 客户端、服务端和 Web 框架依赖。

AgentScope 2.0.2 的 A2A 扩展尚未完全迁移到 2.0 的无状态执行契约：客户端
`A2aAgent` 明确持有单次请求状态且不允许并发复用，服务端 `AgentRunner` 仍以已弃用的
粗粒度 `Event` 作为协议内适配类型。因此本模块采用隔离策略：客户端每次 LiteFlow
调用创建独立 `A2aAgent`；服务端从 `streamEvents(..., RuntimeContext)` 获取类型化事件，
只在 A2A 边界转换为粗粒度 `Event`。除这一上游协议边界外，core、Harness 和 Provider
代码不得新增旧 Hook／Event API。该限制必须写入迁移文档，并由依赖升级测试监控，待
AgentScope A2A 完成 2.0 原生迁移后删除边界适配层。

## 7. 组件 API

### 7.1 `AbstractAgentComponent`

`AbstractAgentComponent` 继承 `NodeComponent`，统一承载两种 Agent 的执行模板。

核心约束：

- `process()` 保持 `final`；
- 每次调用创建新的 `RuntimeContext` 和 `LiteFlowAgentContext`；
- Agent 实例不捕获 Slot、conversationId 或其他调用期对象；
- 模型、Toolkit、Middleware 等构建期对象按实际 LiteFlow 组件实例缓存；
- 调用完成后清理 Slot attachment 和调用期引用；
- 组件实现 `AutoCloseable`，并在容器销毁或非容器调用方显式关闭时释放 Agent runtime。

扩展点必须区分构建期与调用期，避免无状态 Agent 永久捕获第一次执行的 Slot。

构建期扩展点只能读取组件配置和稳定的节点身份：

```java
protected abstract String systemPrompt();

protected String agentKey();

protected List<Object> tools();

protected List<MiddlewareBase> middlewares();

protected void customizeToolkit(Toolkit toolkit);
```

调用期扩展点显式接收 Slot 或 `LiteFlowAgentContext`：

```java
protected String resolveUserId(Slot slot);

protected String resolveConversationId(Slot slot);

protected abstract String userPrompt(LiteFlowAgentContext context);

protected Mono<String> transformSystemPrompt(
        String currentPrompt,
        LiteFlowAgentContext context);

protected Class<?> structuredOutputType();

protected JsonNode structuredOutputSchema();

protected void customizeRuntimeContext(
        RuntimeContext.Builder builder,
        LiteFlowAgentContext context);

protected void handleReply(Msg reply, LiteFlowAgentContext context);
```

`structuredOutputType()` 与 `structuredOutputSchema()` 互斥；同时返回非空属于配置错误。

`systemPrompt()` 的返回值是构建期基础提示词，不能读取 Slot。需要按调用动态追加租户、会话或业务信息时，使用由 Middleware 驱动的 `transformSystemPrompt(...)`。

### 7.2 `AgentComponent`

保留现有类名作为主要轻量入口，并继续支持：

- `model()`／`buildModel()`；
- `systemPrompt()`；
- `userPrompt(LiteFlowAgentContext)`；
- `tools()`；
- `maxIterations()`；
- `resolveConversationId(Slot)`；
- `agentKey()`；
- `handleReply(Msg, LiteFlowAgentContext)`。

旧 `AgentContext` 重命名为 `LiteFlowAgentContext`。隐式 `ctx()`、`usedSkills()` 和 chat usage 访问器不再作为主要接口；调用期数据从显式传入的 context 读取。这是为了避免异步 Middleware／Tool 依赖 ThreadLocal 或误读池化后的 Slot。

新增原生 2.0 扩展点：

```java
protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder);

protected ExecutionConfig modelExecutionConfig();

protected ExecutionConfig toolExecutionConfig();

protected Model fallbackModel();

protected int maxRetries();

protected List<McpClientWrapper> mcpClients();

protected Model routeModel(
        Model defaultModel,
        LiteFlowAgentContext context);
```

`model()`、`buildModel()`、`tools()`、`middlewares()`、`skills()`、`enableSkills()`、`enableShellTool()`、`enableWorkspaceFileTools()` 和 `maxIterations()` 都定义为构建期纯配置，不得读取请求或 Slot。需要按调用选择模型时，`ModelRoutingMiddleware` 使用 `routeModel(...)` 返回已经构建并由组件 runtime 管理的 Model；不得每次请求新建未关闭的模型客户端。需要按调用过滤技能时，通过 RuntimeContext 驱动的 skill filter 实现。

旧的 `hooks()` 不再作为主扩展点；如果短期保留，只能标记为弃用兼容层，模块自身所有逻辑必须迁移到 Middleware。

### 7.3 `HarnessAgentComponent`

新增 `HarnessAgentComponent`，提供与 Agent 组件一致的基本体验，并增加：

```java
protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder);

protected CompactionConfig compactionConfig();

protected MemoryConfig memoryConfig();

protected List<AgentSkillRepository> skillRepositories();

protected List<SubagentDeclaration> subagents();

protected boolean enablePlanMode();

protected PermissionContextState permissionContext();
```

高级 Harness 能力通过 `customizeHarness()` 暴露，避免 LiteFlow 重复封装全部上游 builder。

## 8. Agent 生命周期

### 8.1 无状态 Agent

AgentScope 2.0 的 `ReActAgent` 与 `HarnessAgent` 均按无状态方式使用：

- 同一个组件实例只构建一个 Agent runtime；
- 同一个 Agent runtime 服务多个用户和会话；
- 同一个 Agent 实例内，相同 `(userId, sessionId)` 的单次 `call()` 由 AgentScope 串行化；
- 不同会话允许并行；
- 运行状态只从 RuntimeContext／AgentStateStore 获取。

不得再按每个会话缓存一个 Agent。

### 8.2 `AgentRuntimeHandle`

每个实际 Agent 组件实例拥有一个惰性创建的 `AgentRuntimeHandle`：

- 按组件实例而非组件 Class 构建 runtime；
- 不使用捕获首个 `AgentConfig` 的静态 holder；
- `AgentComponent`／`HarnessAgentComponent` 实现 `AutoCloseable`；
- Spring／Solon 组件销毁时调用 `close()`，并用集成测试验证；
- 非容器场景由创建并注册组件的调用方显式调用 `close()`；
- runtime handle 关闭自身创建的 Agent、MCP Client、Harness 资源和本地 Store；
- 从容器注入的共享 StateStore／MCP Client 由容器拥有，handle 只关闭不拥有资源的轻量装饰器；
- 测试可显式创建、关闭和重建组件，不通过反射重置全局状态。

LiteFlow 当前仍使用全局 `LiteflowConfigGetter`、`FlowBus` 和 `FlowExecutorHolder`，本次升级不承诺在同一 JVM 内提供多个完全隔离的 LiteFlow Context。规格只保证不再额外引入“永久捕获第一份 AgentConfig”的 Agent 静态单例。若未来 LiteFlow 核心提供 per-executor 配置归属和统一 close 协议，Agent runtime 可以再迁移到 executor-owned 生命周期。

### 8.3 `AgentInvocationGuard`

AgentScope 的 `callGates` 只属于单个 Agent 实例，不能保护不同组件实例、不同 JVM 或 HITL 的两次独立 `call()`。LiteFlow 适配层在整个逻辑调用外增加：

```java
public interface AgentInvocationGuard {
    AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout);
}
```

guard 支持两种 key：

- 状态租约：`STATE＋namespace＋userId＋conversationId＋agentKey`；
- 工作区租约：`WORKSPACE＋namespace＋userId＋conversationId`。

Agent 且不使用共享工作区时只获取状态租约。Harness 或启用共享文件工具时，必须先获取 conversation 级工作区租约，再获取 Agent 状态租约；所有代码遵循这个固定顺序，释放时反序，避免不同 Agent 之间形成死锁。租约在以下完整事务结束前一直持有：

```text
首次 Agent call
  → 可选 HITL 等待
  → 确认／拒绝 continuation call
  → 最终状态保存与回复处理
```

工作区租约确保不同 agentKey 共享同一 conversation workspace 时不会并行执行写操作。若 remote filesystem 明确提供原子 CAS、事务或服务端共享锁，可以通过自定义 guard 缩小临界区；默认实现优先保证正确性。即使用户显式开启 Toolkit 并行，同一工作区的写工具仍必须使用 backend 原子能力或共享锁。

默认提供应用内共享的 `LocalAgentInvocationGuard`：容器场景使用单例 Bean，非容器场景使用不捕获配置或 Agent 资源的轻量进程级 guard。其 key map 在租约释放且无等待者后移除。多副本部署必须选择以下一种策略：

- 对同一 key 使用粘性路由，再由本地 guard 串行；
- 注入带租约续期和 fencing token 的分布式 guard；
- 使用具备版本化 CAS 的自定义 StateStore／协调器。

仅共享 Redis／MySQL StateStore 不提供并发安全保证。若检测到分布式 Store 却没有声明协调策略，启动验证应告警；严格模式直接拒绝启动。guard 获取超时按 `TIMEOUT` 分类，所有异常路径必须释放租约。

## 9. 身份、会话与工作区

### 9.1 三个独立维度

必须分别建模：

| 维度 | 语义 | 默认来源 |
|---|---|---|
| `userId` | 业务用户或租户 | `resolveUserId()` |
| `conversationId` | 跨请求延续的业务对话 | Slot／请求数据／生成器 |
| `agentKey` | 同一对话中的 Agent 节点身份 | nodeId 或显式覆盖 |

原始 ID 用于响应、日志和 FlowEvent；存储键使用安全编码后的 ID。

### 9.2 安全编码

不再使用 URL 编码后替换字符或字符串分隔符拼接。统一使用长度前缀输入加 SHA-256：

```text
runtimeSessionId = "lf-" + hex(sha256(namespace, userId, conversationId))
agentNamespace = "lf-" + hex(sha256(namespace, agentKey))
storeSessionId = agentNamespace + "." + runtimeSessionId
```

- 编码结果只包含固定长度十六进制字符；
- 不存在 `a/b` 与 `a_2Fb`、`a__b` 等碰撞；
- 原始 ID 保存在 `LiteFlowAgentContext`，便于追踪；
- `namespace` 防止多个应用共享同一 StateStore 时串话。

### 9.3 状态隔离

- `RuntimeContext.userId` 使用业务用户／租户；
- `RuntimeContext.sessionId` 使用 `runtimeSessionId`，使同一 conversationId 的 Harness 调用共享会话工作区；
- AgentScope 2.0.2 内部使用固定的状态 key `agent_state`，Agent 名称不会自动隔离 StateStore；
- 每个 Agent runtime 因此使用一个不拥有底层资源的 `GuardedNamespacedAgentStateStore` 装饰器，把传入的 `runtimeSessionId` 映射为 `storeSessionId`；
- 装饰器对 `save`、`get`、`exists`、`delete` 和 `listSessionIds` 使用同一可逆前缀规则；装饰器不关闭共享底层 Store，资源由容器或创建它的 runtime handle 关闭；
- 因此同一用户、同一会话下，不同 Agent 的持久状态互相隔离；
- 同一 Agent 的多轮调用延续同一状态。

### 9.4 工作区路由与安全边界

Harness 文件系统使用 `IsolationScope.SESSION` 做逻辑 namespace 路由：

- 同一 userId、conversationId 下的不同 Agent 使用同一逻辑工作区；
- 不同 userId 或 conversationId 映射到不同的 `runtimeSessionId`；
- 原始 conversationId 不直接成为文件路径。

这不是本地多租户安全边界。AgentScope 2.0.2 的 `LocalFilesystem` 对绝对路径不会添加 session namespace；宿主机 Shell 也能绕过文件 API。因而：

- 本地模式只用于可信单用户／开发场景；
- LiteFlow 的 guarded local filesystem 包装器必须拒绝绝对路径、检查真实路径与符号链接，并把相对路径限制在当前 session 目录；
- 本地宿主机 Shell 默认关闭，不能依赖 namespace 或命令黑名单实现隔离；
- 不可信用户、多租户和生产代码执行必须使用独立容器沙箱或具备服务端 namespace 强制的 remote filesystem；
- 安全测试必须证明绝对路径、符号链接和 Shell 无法跨越物理边界。

本地工作区根目录仍由 `liteflow.agent.workspace.root` 指定，但只有 guarded wrapper 解析出的 session 子目录可写。远程文件系统或沙箱通过 Harness builder 配置。

## 10. RuntimeContext 与调用期上下文

新增 `LiteFlowAgentContext`，只存放本次调用所需数据：

- `Slot`；
- 原始 `userId`、`conversationId`、`agentKey`；
- 安全编码后的 RuntimeContext 标识；
- chainId、nodeId、requestId、traceId；
- `FlowEventListener`；
- chat usage tracker；
- used skills tracker；
- 结构化输出元数据；
- 调用 deadline／取消信号。

每次执行构造：

```java
RuntimeContext context = RuntimeContext.builder()
        .userId(runtimeUserId)
        .sessionId(runtimeSessionId)
        .put(LiteFlowAgentContext.class, liteFlowContext)
        .put(Slot.class, slot)
        .build();
```

工具和 Middleware 通过 RuntimeContext 注入访问调用期对象，不捕获 `ctx()` 返回值，不依赖共享字段，也不假设 ThreadLocal 一定跨越所有异步边界传播。

## 11. 状态存储

### 11.1 替换内容

删除或停止使用：

- `AgentSessionManager`；
- `AgentSession`；
- `AgentSessionFactoryRegistry`；
- 旧 LOCAL_FILE／Redis／MySQL Session factory；
- `.memory(new InMemoryMemory())`；
- 1.x `SessionManager.load()`／`save()`。

改用 `AgentStateStore`：

- `InMemoryAgentStateStore`：测试和显式的进程内状态；
- `JsonFileAgentStateStore`：单机开发；
- Redis／MySQL／其他后端：使用 AgentScope extension 或用户提供的 Bean；
- 自定义存储：实现 `AgentStateStore` 并通过容器注入。

在共享底层 Store 与具体 Agent 之间增加 `GuardedNamespacedAgentStateStore`：

```text
ReActAgent／HarnessAgent
        │  userId, runtimeSessionId, "agent_state"
        ▼
GuardedNamespacedAgentStateStore(agentNamespace)
        │  userId, agentNamespace.runtimeSessionId, "agent_state"
        ▼
共享 AgentStateStore
```

该装饰器不缓存 Agent、不维护调用锁，也不拥有底层 Store 的关闭权。它承担两项职责：

1. 对所有 StateStore 操作做确定性的 Agent namespace 路由；
2. 当底层 `get()` 抛错时，按当前 state key 记录本次加载故障并继续向上抛出。

AgentScope 2.0.2 会捕获 StateStore 加载异常并降级为空状态，因此严格模式还必须注册最高优先级的 `StateStoreFailureMiddleware`。AgentScope 完成 `beforeAgentExecution` 的状态加载后，该 Middleware 在进入模型推理前检查装饰器记录；存在加载故障时立即抛出 `STATE_STORE` 异常，确保模型和工具尚未执行。成功加载、调用结束和异常清理都会移除记录。

Store 的保存异常由 AgentScope reactive 链向外传播；适配层统一包装为 `STATE_STORE`。该机制需要确定性测试证明“加载失败时模型调用次数为 0”，不能只检查最终响应失败。

### 11.2 配置

保留 `liteflow.agent.*` 前缀，但将旧 `session.memory.*` 重构为：

```yaml
liteflow:
  agent:
    runtime:
      namespace: my-app
      default-user-id: anonymous
      timeout: 2m
    state-store:
      type: memory       # memory | json | bean
      bean-name:
      json-root: ./data/agent-state
```

Redis、MySQL 连接信息不再复制到 LiteFlow 配置中；由相应 StateStore Bean 自己管理。

### 11.3 持久化语义

- AgentState 只保存 Agent 对话与运行状态；
- 业务事实仍写业务数据库；
- 单机 JSON Store 不用于多副本部署；
- 状态存储失败默认通过装饰器故障通道与 Middleware 使本次 Agent 节点失败，避免产生“回复成功但上下文悄悄丢失”的假成功；
- 可通过明确的 failure policy 改为告警，但不能静默吞掉错误。

## 12. 消息与结构化输出

### 12.1 消息

用户输入使用 `UserMessage`，不再任意组合 `MsgRole`：

```java
List<Msg> input = List.of(new UserMessage(userPrompt()));
```

系统提示词由 Agent builder 的 `sysPrompt` 管理；工具结果、Assistant 回复和内容块遵循 AgentScope 2.0 的严格类型。

### 12.2 结构化输出

组件可选择：

- `structuredOutputType()` 返回 POJO Class；
- `structuredOutputSchema()` 返回 JSON Schema；
- 两者都为空时返回普通文本。

调用分别使用：

```java
agent.call(messages, runtimeContext)
agent.call(messages, outputType, runtimeContext)
agent.call(messages, schema, runtimeContext)
```

默认回复处理：

- 普通回复写入 `Slot.responseData` 的文本；
- POJO 结构化回复写入反序列化后的对象；
- JSON Schema 回复写入结构化数据节点；
- 空回复会明确写入 `null`，不保留 Slot 中的旧 responseData。

结构化数据仍必须经过业务校验，不能因为符合 JSON Schema 就默认业务合法。

## 13. Middleware 与观测

### 13.1 原则

模块自身不再使用旧 Hook。所有横切逻辑迁移为无共享可变状态的 Middleware：

- Middleware 实例只保存不可变配置；
- 调用期数据从 RuntimeContext 获取；
- 使用 `order()` 明确顺序；
- 不把 Slot、usage tracker 或 listener 保存在成员字段。

### 13.2 内置 Middleware

1. `StateStoreFailureMiddleware`
   - 在 AgentScope 状态加载完成、模型推理开始前检查记录的加载故障；
   - 严格模式下立即终止调用，宽松模式下记录告警并清理故障；
   - 优先级高于所有会触发模型或工具的 Middleware。

2. `LiteFlowSystemPromptMiddleware`
   - 从 RuntimeContext 读取 `LiteFlowAgentContext`；
   - 调用组件的 `transformSystemPrompt(...)`；
   - 支持调用期动态提示词，但不把组件或 Slot 数据写入共享字段。

3. `ModelRoutingMiddleware`
   - 在每次 model call 前从 RuntimeContext 读取调用信息；
   - 调用 `routeModel(...)` 选择已管理的 Model；
   - 不在请求路径临时创建未关闭的 HTTP／SDK 客户端。

4. `FlowEventBridgeMiddleware`
   - 观察 AgentEvent；
   - 转换为 LiteFlow FlowEvent；
   - 保留原始 AgentEvent 作为类型化 payload。

5. `ChatUsageMiddleware`
   - 在 model call 周期聚合 usage；
   - 结果写入本次 `LiteFlowAgentContext`。

6. `SkillTrackingMiddleware`
   - 记录加载和实际使用的 skill；
   - 返回去重且保持顺序的列表。

7. `AgentLoggingMiddleware`
   - 输出 userId、conversationId、agentKey、chainId、nodeId；
   - 不记录 apiKey 和未经脱敏的敏感工具参数。

### 13.3 Middleware 顺序

顺序从外到内建议为：

```text
StateStore failure guard
  → Tracing／Logging
    → FlowEventBridge
      → Usage／Skill tracking
        → 用户自定义 Middleware
          → AgentScope／Harness 内置 Middleware
```

最终顺序以 AgentScope 的洋葱调用语义写测试固定。

## 14. 类型化事件与 FlowEvent

### 14.1 保留事件

为降低不必要的行为变化，继续发送：

- `agent.reasoning`
- `agent.tool_result`
- `agent.summary`
- `agent.result`

### 14.2 新增事件

增加至少以下事件：

- `agent.start`
- `agent.end`
- `agent.text.delta`
- `agent.thinking.delta`
- `agent.tool.call.start`
- `agent.tool.call.delta`
- `agent.tool.call.end`
- `agent.tool.result.start`
- `agent.tool.result.delta`
- `agent.tool.result.end`
- `agent.confirm.required`
- `agent.confirm.result`
- `agent.result`
- `agent.error`

每个事件包含：

- 原始 userId、conversationId、agentKey；
- chainId、nodeId、requestId、traceId；
- AgentScope event type；
- AgentScope 原始 `AgentEvent` payload；
- taskId、parentSessionId、replyId 等可用元数据。

### 14.3 Listener 失败

事件 listener 属于调用路径的一部分。默认策略为 `FAIL_FAST`，保持错误可见；可配置为 `LOG_AND_CONTINUE` 供纯观测场景使用。无论采用哪种策略，必须有测试固定行为。

## 15. Toolkit、工具和并发

### 15.1 默认串行

AgentScope 2.0.2 的 Toolkit 默认可以并行执行工具。LiteFlow 工具经常修改同一个 Slot 或调用非幂等业务接口，因此模块必须显式使用：

```java
new Toolkit(ToolkitConfig.builder().parallel(false).build())
```

只有同时满足以下条件时才允许并行：

- Toolkit 显式开启 parallel；
- 工具声明 `concurrencySafe=true`；
- 业务确认工具不修改共享 Slot 或非线程安全对象；
- 测试覆盖多工具同时调用。

模型层的 `GenerateOptions.parallelToolCalls` 与 Toolkit 的实际并行执行是两个独立开关，不能混为一谈。

### 15.2 RuntimeContext 注入

自定义 Tool 应通过 RuntimeContext 获取：

- `LiteFlowAgentContext`；
- 当前 Slot；
- 业务服务 Bean；
- trace／幂等键；
- 用户和会话信息。

工具对象按构建期创建，不捕获一次调用中的 Slot。

### 15.3 MCP

`AgentComponent` 和 `HarnessAgentComponent` 都支持注册 `McpClientWrapper`：

- MCP Client 按所有权由组件的 runtime handle 或容器关闭；
- 注册失败在 Agent 构建阶段显式报错；
- 本 JVM 内的 LiteFlow 工具优先直接注册，不绕 MCP；
- 远程工具使用 STDIO、SSE 或 Streamable HTTP 时应用权限和超时策略。

## 16. Workspace、文件系统与 Shell

### 16.1 Agent 组件

轻量 Agent 默认不自动提供宿主机 Shell。若启用文件或命令工具：

- 优先使用 AgentScope 官方 filesystem／sandbox 能力；
- 无法使用 Harness 时，自定义工具必须改进真实路径、符号链接、文件大小、进程树和输出消费安全；
- 原有黑名单不能继续被视作沙箱。

### 16.2 Harness 组件

Harness 使用官方：

- workspace manager；
- filesystem isolation；
- sandbox lifecycle；
- permission engine；
- dangerous files／directories 标记；
- tool allow／ask／deny 规则。

高风险能力默认关闭或要求确认，不默认授予任意宿主机文件与网络权限。

`IsolationScope.SESSION` 只负责路由语义；安全等级按 backend 明确标注：

| backend | 用途 | 安全要求 |
|---|---|---|
| guarded local | 可信单用户、测试、开发 | 禁止绝对路径和 symlink 逃逸；宿主 Shell 默认关闭 |
| remote filesystem | 多副本共享文件 | 服务端强制 namespace 与访问控制 |
| container sandbox | 不可信文件／Shell 执行 | 独立物理 root、受限网络、资源配额、生命周期回收 |

文档不得把 LocalFilesystem 的逻辑 namespace 描述为安全沙箱。

## 17. Skills

旧 `SkillBox` 与自定义 SkillBox factory 改为 AgentScope 2.0 的 `AgentSkillRepository` 体系。

支持：

- classpath repository；
- filesystem／workspace repository；
- 用户注入的 Git、MySQL、Nacos 等 repository；
- skill allowlist／filter；
- Harness 动态 skill middleware；
- 现有 Spring／Solon Bean 优先的 skill tool 解析行为。

Skill 工具仍受统一 Toolkit、并发和权限策略约束。Skill 自我写入、skill curator 和代码执行属于显式高风险 opt-in。

`usedSkills()` 继续作为本次调用的只读结果，不存放在线程不安全的组件字段中。

## 18. Harness 能力

`HarnessAgentComponent` 首批支持：

1. workspace 与 session 隔离；
2. context file；
3. compaction；
4. Harness memory；
5. skill repository；
6. task list 与 plan mode；
7. local subagents；
8. permission context；
9. sandbox filesystem；
10. tool-result eviction；
11. builder customizer。

Agent Protocol、Channel、远程 subagent 和分布式 task repository 通过 customizer／Bean 开放，不在 LiteFlow core 中复制协议配置。

### 18.1 A2A 与 Agent Protocol

- local subagent：同一 Harness 进程内的任务委派；
- Agent Protocol／Channel：AgentScope Harness 的任务与事件协议；
- A2A：跨服务、可发现的标准 Agent-to-Agent 协议。

三者不能互相混称。`liteflow-agent-a2a` 首批提供：

1. `A2aAgentComponent`，把远程 `A2aAgent` 作为 LiteFlow 节点调用；
2. RuntimeContext 中 tenant、trace、task 元数据的显式 allowlist 透传；
3. 远程任务状态、取消和事件的 FlowEvent 桥接；
4. `AgentScopeA2aServer` 适配示例和 Agent factory 接口，但不在组件库中自行启动 Web 服务器；
5. 使用 fake transport 的客户端契约测试。

A2A 依赖保持可选；不使用跨服务 Agent 的应用不会传递客户端、服务端或 Web 协议依赖。

## 19. HITL 与权限

### 19.1 确认处理器

新增：

```java
public interface AgentConfirmationHandler {
    Mono<List<ConfirmResult>> confirm(
            RequireUserConfirmEvent event,
            LiteFlowAgentContext context);
}
```

- 检测到 `RequireUserConfirmEvent` 时发送 `agent.confirm.required`；
- `FlowEventBridgeMiddleware` 只记录事件，不在首轮 event stream 尚未结束时调用 handler；
- 首轮 `call()` 返回 `GenerateReason.PERMISSION_ASKING` 后，校验返回 Msg 中的待确认 `ToolUseBlock` 与记录的事件一致；
- 在首轮 `call()` 已完全终止后调用 handler，为每个待确认工具获取决定；
- 校验 `ConfirmResult` 恰好覆盖所有待确认 `ToolUseBlock.id`，不允许未知、缺失或重复 ID；
- 构造只携带 `Msg.METADATA_CONFIRM_RESULTS` 的恢复消息，使用同一 Agent、RuntimeContext 和首轮结构化输出类型／Schema 发起第二次 `call()`；
- 完成后发送 `agent.confirm.result`；
- `replyId` 用于适配层和外部 handler 校验当前确认请求，并与首轮 Msg 的 `Msg.METADATA_CONFIRM_REQUEST_REPLY_ID` 比对；它不写入恢复消息；
- AgentScope 根据已持久化的暂停 Assistant Msg 自动生成关联的 `UserConfirmResultEvent`，核心工具关联使用 `ToolUseBlock.id`；taskId 只用于链路追踪。

整个“首轮暂停→等待确认→continuation→最终保存”过程持有同一个 `AgentInvocationGuard` 租约，防止另一个请求在两次 `call()` 之间进入同一状态槽。handler 不得在首轮 event 回调内递归调用 Agent，否则会等待 AgentScope 自身的 session gate。

### 19.2 默认策略

- 未配置 handler：为全部待确认工具生成拒绝结果，先执行 continuation 清除 `ASKING` 状态，再抛出权限异常；
- handler 超时：为全部待确认工具生成拒绝结果，先执行 continuation 清除 `ASKING` 状态，再抛出超时异常；
- handler 异常：同样先用拒绝 continuation 清理状态，再让调用失败；
- 用户显式拒绝：把拒绝结果交给 Agent 继续推理，由 Agent 生成最终回复；可配置 `fail-on-denied-tool=true` 改为权限失败；
- 自动允许只能通过显式 permission rule 配置。

第一阶段不持久化整条 LiteFlow Chain 的暂停点。需要长时间异步人工审批时，由自定义 handler 负责外部持久化和等待，后续再设计 LiteFlow 原生可恢复执行。

## 20. 模型与容错

### 20.1 `ModelSpec`

保留 `ModelSpec` 作为 LiteFlow 配置友好的模型描述，但实现迁移到 2.0 extension 包。

每个 Provider Spec 必须支持：

- apiKey；
- modelName；
- baseUrl／endpoint；
- stream；
- GenerateOptions；
- provider 特有 reasoning／thinking 配置；
- formatter；
- native structured output 能力；
- builder escape hatch。

已存在但不生效的配置必须修复或删除，例如 Anthropic thinking enabled 和部分 Provider 未读取 baseUrl 的问题。

### 20.2 重试与降级

组件开放：

- `maxRetries`；
- `fallbackModel`；
- model execution timeout；
- tool execution timeout；
- stop-on-reject；
- pending tool recovery。

LiteFlow 外层 RETRY 与 Agent 内部模型重试的语义必须区分：

- Agent 内部重试用于瞬时模型／网络错误；
- LiteFlow RETRY 会重新执行整个 Agent 节点，可能重复工具副作用；
- 有副作用的工具需要幂等键。

## 21. 配置模型

配置仍位于 `liteflow-core` 的 `com.yomahub.liteflow.property.agent`，保持纯 POJO，不引用 AgentScope 类型。

保留：

- provider credentials；
- workspace root；
- shell／工具安全默认值；
- defaults；
- logging；
- skills 基本开关。

重构：

- `session.memory` → `state-store`；
- 会话 idle cleanup／maxSessions 等旧 Agent 缓存配置删除；
- 增加 runtime namespace、default userId、调用超时；
- 增加 toolkit parallel 默认值；
- 增加 listener failure mode；
- 增加 state-store failure policy。
- 增加 invocation guard 模式、Bean 名称、租约超时和分布式协调声明；
- 增加 workspace backend／trusted-local 标识，禁止把默认 local namespace 误配置为多租户沙箱。

Harness 的复杂对象不全部做成配置 POJO，优先使用 `customizeHarness()` 与容器 Bean。

## 22. 异常模型

统一使用 `AgentInvocationException`，至少区分：

- `CONFIGURATION`；
- `MODEL`；
- `TOOL`；
- `PERMISSION`；
- `TIMEOUT`；
- `CANCELLED`；
- `STATE_STORE`；
- `EVENT_LISTENER`；
- `STRUCTURED_OUTPUT`；
- `INTERNAL`。

规则：

- 保留根因；
- 附加 userId、conversationId、agentKey、chainId、nodeId，但不泄露 apiKey；
- 超时应取消上游 Reactor subscription 和在途工具；
- 关闭资源时的异常作为 suppressed exception 附加；
- 不把明确的配置错误包装成含糊的模型调用失败；
- FlowEvent 发送失败遵循配置的 listener failure mode。

## 23. 执行数据流

```text
1. LiteFlow 调用 Agent 组件 process()
2. 读取 AgentConfig，验证必填配置
3. 解析 userId、conversationId、agentKey
4. 生成安全 runtime sessionId／agent namespace
5. 按固定顺序获取所需的 conversation workspace 租约和 Agent 状态租约
6. 构造 LiteFlowAgentContext 并绑定 Slot
7. 构造 RuntimeContext，注入调用期对象
8. 当前组件的 AgentRuntimeHandle 获取或惰性构建无状态 Agent，并使用 GuardedNamespacedAgentStateStore
9. 构造 UserMessage
10. 根据输出模式执行首轮普通／POJO／JSON Schema call
11. Middleware 转发 AgentEvent、统计 usage／skills，并在推理前检查状态加载故障
12. 若进入 PERMISSION_ASKING，首轮终止后执行 handler 和同输出模式 continuation call
13. handleReply 将文本或结构化对象写入 Slot
14. 清理 Slot attachment、故障记录和调用期引用，并释放 guard 租约
15. 异常按 AgentInvocationException 分类交给 LiteFlow
```

## 24. 测试策略

### 24.1 测试原则

- 先写失败测试，再实现生产代码；
- 核心回归不依赖网络和真实密钥；
- live 平台测试放入显式开启的 smoke profile；
- 测试断言行为和公开契约，不绑定内部实现细节。

### 24.2 确定性测试

使用 fake model／fake tool／fake state store 覆盖：

1. 普通文本调用与 Slot 写回；
2. POJO 与 JSON Schema 结构化输出；
3. RuntimeContext 中 userId、sessionId 和 LiteFlowAgentContext；
4. 同一组件复用 Agent，不同组件实例不串 prompt／tool；
5. 同一会话串行、不同会话并行；
6. 同一 key 跨不同组件实例仍由 LocalAgentInvocationGuard 串行；
7. distributed guard 租约、超时、续期／fencing 契约；
8. 同 conversationId 下不同 agentKey 状态隔离；
9. workspace 按 user＋conversation 共享和逻辑路由，不同 agentKey 并发写由 conversation 级租约串行；
10. memory／json state store 保存、关闭、重建和恢复；
11. StateStore 加载失败时模型调用次数为 0；
12. 所有关键 AgentEvent 的 FlowEvent 映射；
13. listener FAIL_FAST 与 LOG_AND_CONTINUE；
14. usage／usedSkills 每次调用隔离；
15. Tool RuntimeContext 注入；
16. Toolkit 默认串行和显式并行；
17. model routing、retry、fallback、timeout、cancel；
18. HITL 允许、拒绝、超时、replyId 校验、准确的 confirm metadata 和两次 call 之间无插队；
19. Middleware 顺序；
20. Agent／MCP／StateStore 资源所有权和组件 close 生命周期；
21. 任何异常路径都清理 Slot attachment、状态故障记录和 guard 租约。

### 24.3 Provider 契约测试

无网络构造并检查：

- OpenAI；
- Anthropic；
- Gemini；
- DashScope；
- DeepSeek；
- GLM；
- Kimi；
- MiniMax；
- 自定义 OpenAI Compatible endpoint。

覆盖 apiKey、baseUrl、stream、GenerateOptions、thinking／reasoning 和缺失凭据错误。

### 24.4 安全测试

- 符号链接逃逸；
- `..` 与绝对路径；
- 写入文件大小限制；
- Shell 绝对可执行文件绕过；
- 子进程树清理；
- 输出截断时持续消费管道；
- 危险文件／目录权限；
- 未配置 HITL handler 时默认拒绝；
- 并发工具修改 Slot。

### 24.5 集成测试

- Spring Boot 3／4 配置绑定；
- 非 Spring FlowExecutor；
- 容器 Bean 优先解析；
- JDK 17 编译与测试；
- 各平台真实调用作为可选 smoke test；
- Harness workspace、skills、compaction、plan mode 和 subagent 的本地确定性测试。
- A2A 客户端 fake transport、远程事件和取消契约测试。

### 24.6 依赖审计

执行 dependency tree，重点检查：

```text
io.projectreactor
com.fasterxml.jackson.core
org.slf4j
com.squareup.okhttp3
io.opentelemetry
io.modelcontextprotocol.sdk
org.xerial:sqlite-jdbc
```

验收时需要证明不存在多版本 AgentScope 类、聚合包重复类和明显的运行时不兼容。

## 25. 兼容与迁移策略

### 25.1 尽量保留

- `AgentComponent` 类名；
- `liteflow.agent.*` 配置前缀；
- `model()`、`buildModel()`、`systemPrompt()`、`userPrompt()` 等核心使用习惯；
- `conversationId＋agentKey` 的业务语义；
- 同一会话共享 workspace、不同 Agent 隔离状态；
- 默认把最终回复写入 Slot；
- 已有四个 FlowEvent 名称；
- provider `ModelSpec` 的 fluent 使用方式。

### 25.2 明确破坏

- AgentScope 1.x `Hook`／`Session`／`Memory`／`SkillBox` 类型；
- 自定义 AgentSessionManager 与 factory SPI；
- 每会话一个 ReActAgent 实例的行为；
- 旧 memory storage 配置；
- 默认启用不安全宿主机 Shell；
- Tool 默认串行依赖上游默认值的隐式行为。

### 25.3 迁移文档

交付时提供：

- 1.0.12 → 2.0.2 API 对照；
- 配置项对照；
- Agent 示例；
- Harness 示例；
- A2A 远程 Agent 示例；
- 结构化输出示例；
- Skill／MCP／HITL 示例；
- 状态存储 Bean 示例；
- 依赖与 JDK 要求。

## 26. 验收标准

完成必须同时满足：

1. 所有 AgentScope 模块精确使用 `2.0.2`。
2. 生产代码不再依赖旧 `SessionManager`、`Memory`、粗粒度 `Event`、旧 stream API 和模块自有 Hook 实现。
3. `AgentComponent` 使用无状态 Agent＋RuntimeContext＋AgentStateStore。
4. 新增可用的 `HarnessAgentComponent`，至少覆盖 workspace、compaction、skills、plan mode、subagent 和权限配置。
5. Toolkit 默认串行，显式并发行为有测试。
6. 普通文本、结构化输出、流式事件和 HITL 有确定性测试。
7. state store 跨 runtime 重建恢复测试通过。
8. 不存在静态捕获首个 AgentConfig 的 runtime holder。
9. 所有 Agent／Store／MCP／Harness 资源可关闭且生命周期测试通过。
10. Provider 模块使用 2.0 extension 包且无网络契约测试通过。
11. `liteflow-testcase-el-agent` 核心测试不依赖真实密钥；live 测试单独启用。
12. 完成 Maven dependency tree 审计与 JDK 17 验证。
13. 更新用户文档和迁移指南。
14. 同一逻辑状态槽和 conversation workspace 由外层 AgentInvocationGuard 分层保护；HITL 两次 call 之间不能插入其他请求，不同 Agent 不能并发覆盖共享文件。
15. StateStore 加载失败在模型调用前终止，确定性测试证明模型调用次数为 0。
16. 本地 workspace 明确标注为逻辑 namespace；绝对路径和 symlink 无法跨 session，宿主 Shell 默认关闭。
17. 可选 A2A 模块能够调用远程 Agent、桥接事件和取消，且不污染 core 依赖。

## 27. 实施原则

后续实施计划遵循：

1. 先建立确定性测试与 fake model；
2. 再切换 BOM 和最小 core API；
3. 逐步迁移状态、RuntimeContext、Middleware、事件与工具；
4. 分别迁移 Provider；
5. 最后增加 Harness、A2A 模块与高级能力；
6. 每一阶段保持可编译、可测试，避免一次性大爆炸式替换；
7. 完成后进行独立代码审查和全量验证。
