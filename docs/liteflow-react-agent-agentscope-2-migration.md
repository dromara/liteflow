# LiteFlow ReAct Agent：AgentScope 1.x 到 2.0.2 迁移指南

本文只讨论破坏式迁移。新项目请直接阅读[《LiteFlow ReAct Agent 使用指南》](liteflow-react-agent-guide.md)。

目标运行时为 JDK 17、AgentScope Java 2.0.2 与当前 LiteFlow Agent 细粒度模块。迁移不是包名替换：调用上下文、runtime ownership、状态持久化、事件、skills、HITL 和 filesystem 安全模型都已经改变。

## 1. 一页迁移表

| 1.x | 2.0.2 | 迁移动作 |
| --- | --- | --- |
| `ReActAgentContext`／隐式 `ctx()` | 显式 `LiteFlowAgentContext` 参数 | 给调用期 hook 增加 `LiteFlowAgentContext` 参数；工具／Middleware 从本次 `RuntimeContext` 取值 |
| `hooks()`／`Hook` | `middlewares()`／`MiddlewareBase` | 改写为 Reactor publisher 链，返回 `next.apply(input)`；不要自行 `subscribe()` |
| `AgentSessionManager`／Memory factory | `AgentStateStore` Bean 与 namespaced decorator | 选择 `MEMORY`、`JSON` 或 `BEAN` Store；共享后端同时配置跨进程协调 |
| `session.memory.*` | `runtime.*` 与 `state-store.*` | 删除旧键，设置必填 `runtime.namespace`、稳定 user／conversation identity 和新的 Store |
| `SkillBox` | `AgentSkillRepository`／Harness skills | 显式创建 repository 和 `SkillFilter`；Java 工具仍由 `tools()` 注册 |
| 每会话 Agent 缓存 | 组件级无状态 runtime + 每调用 `RuntimeContext` | 构建期能力固定在组件实例；会话数据只放进调用上下文与 StateStore |
| workspace 黑名单 Shell | guarded local 或 Docker sandbox | 默认关闭本地工具；可信本地显式 opt-in，不可信执行使用 Docker／远端 sandbox |
| core 包里的 Provider model | `agentscope-extensions-model-*` | 使用 LiteFlow Provider 模块或 AgentScope 2 对应 extension，不要加入 aggregate artifact |
| coarse `Event`／`EventType` | 类型化 `AgentEvent` + LiteFlow `FlowEvent` | 自定义执行观察迁移到 `MiddlewareBase`；旧 coarse 类型只保留在 A2A 2.0.2 wire adapter |
| 旧确认 sink／隐式批准 | `RequireUserConfirmEvent` + `AgentConfirmationHandler` | 为 ASK 工具返回完整 `ConfirmResult`；缺 handler 时 fail-closed |

## 2. 依赖与 JDK

Agent 模块使用 JDK 17。先确认构建和运行时都选择 JDK 17 或更高版本，再让所有 AgentScope 工件收敛到 2.0.2。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-bom</artifactId>
            <version>2.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

只选择需要的 LiteFlow 模块：

- 常规 ReAct：一个 Provider 模块，例如 `liteflow-react-agent-openai`。
- 上下文工程／sandbox：追加 `liteflow-react-agent-harness`。
- A2A client：追加 `liteflow-react-agent-a2a`。
- A2A server：除 LiteFlow A2A 模块外，应用直接声明 optional 的 `agentscope-extensions-a2a-server`。

删除直接依赖 `io.agentscope:agentscope`、旧 1.x model 类和应用自己钉住的旧 vendor SDK。LiteFlow 当前 Provider extension 分别为 OpenAI、Anthropic、Gemini 与 DashScope 2.0.2；它们的 SDK 版本由 AgentScope 依赖图带入。

## 3. 组件签名迁移

### 3.1 最小组件

迁移前：

```java
public final class AssistantCmp extends ReActAgentComponent {
    @Override protected ModelSpec<?> model() { return DeepSeek.of("deepseek-chat"); }
    @Override protected String systemPrompt() { return "Be concise."; }
    @Override protected String userPrompt() {
        return ctx().getSlot().getChainReqData(ctx().getSlot().getChainId()).toString();
    }
    @Override protected void handleReply(Msg reply) {
        ctx().getSlot().setResponseData(reply.getTextContent());
    }
}
```

迁移后：

```java
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import io.agentscope.core.message.Msg;

public final class AssistantCmp extends ReActAgentComponent {
    @Override protected ModelSpec<?> model() { return DeepSeek.of("deepseek-chat"); }
    @Override protected String systemPrompt() { return "Be concise."; }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        Object request = context.getSlot().getChainReqData(context.getChainId());
        return request == null ? "" : request.toString();
    }

    @Override
    protected void handleReply(Msg reply, LiteFlowAgentContext context) {
        context.getSlot().setResponseData(reply == null ? null : reply.getTextContent());
    }
}
```

为什么不兼容：1.x 的隐式上下文依赖 Slot attachment 与当前组件执行时机，缓存对象很容易持有陈旧 Slot。2.0.2 为每次 `process()` 创建新的 `LiteFlowAgentContext` 和 AgentScope `RuntimeContext`，并把参数显式传入调用期扩展点。

### 3.2 旧方法签名对照

| 旧签名 | 新签名／替代 |
| --- | --- |
| `userPrompt()` | `userPrompt(LiteFlowAgentContext context)` |
| `handleReply(Msg reply)` | `handleReply(Msg reply, LiteFlowAgentContext context)` |
| `resolveConversationId()` | `resolveConversationId(Slot slot)` |
| `hooks()` | `middlewares()` |
| `skills()`／`enableSkills()` | `skillRepositories()`、`skillFilter()`、`dynamicSkillsEnabled()` |
| `usedSkills()` | `context.getUsedSkills()` |
| `ctx().getChatUsage()` | `context.getChatUsage()` |
| 调用期动态 `systemPrompt()` | 构建期 `systemPrompt()` + 调用期 `transformSystemPrompt(prompt, context)` |
| 临时构造模型并返回 | 在 `routingModels()` 预登记，再由 `routeModel(defaultModel, context)` 选择 |

`process()` 仍然是 final，不能通过覆写绕过 identity、lease、deadline、StateStore、HITL 或清理。

### 3.3 构建期不得读取 Slot

每个组件实例只懒构建一个 runtime。以下方法通常只执行一次：

```text
model / buildModel / systemPrompt / tools / customizeToolkit
mcpClients / skillRepositories / skillFilter / middlewares / customizeAgent
```

它们不能读取 `getSlot()`，也不能依赖 request、conversation 或用户。本轮动态数据应迁移到：

- `userPrompt(context)`：用户输入。
- `transformSystemPrompt(prompt, context)`：动态系统提示词。
- `routeModel(defaultModel, context)`：从已登记模型中选择。
- `customizeRuntimeContext(builder, context)`：添加类型化调用数据。
- Middleware 回调：从参数中的 `RuntimeContext` 读取。

## 4. Hook 迁移为 Middleware

迁移前：

```java
@Override
protected List<Hook> hooks() {
    return List.of(new AuditHook());
}
```

迁移后：

```java
@Override
protected List<MiddlewareBase> middlewares() {
    return List.of(new MiddlewareBase() {
        @Override
        public Flux<AgentEvent> onModelCall(
                Agent agent,
                RuntimeContext runtime,
                ModelCallInput input,
                Function<ModelCallInput, Flux<AgentEvent>> next) {
            audit(runtime, input);
            return next.apply(input);
        }
    });
}
```

重要差异：

- Middleware 是 Reactor 链的一部分；不能在内部调用 `subscribe()` 或把 publisher 丢掉。
- AgentScope 2 按 `order()` 降序构造洋葱链。LiteFlow 会把业务 Middleware 规范到 user order `1000`，外层仍保留状态故障、日志、事件、usage 与 skills tracking。
- 类型化事件替代旧枚举分支。文本增量、tool、result、confirm 与 error 通过实际 `AgentEvent` 子类或 Reactor error 处理。
- listener 异常由 `liteflow.agent.event.listener-failure-mode` 控制，值为 `FAIL_FAST` 或 `LOG_AND_CONTINUE`。

## 5. 会话、StateStore 与多副本

### 5.1 新 identity

2.0.2 把原始业务 identity 与物理 key 分开：

```text
runtimeSessionId = hash(namespace, userId, conversationId)
agentNamespace   = hash(namespace, agentKey)
storeSessionId   = agentNamespace + "." + runtimeSessionId
```

`runtimeSessionId` 决定 workspace 与 conversation lease，不包含 `agentKey`；Agent state 还按 `agentNamespace` 隔离。多个 Agent 可以共享会话 workspace，但不会默认共享 AgentState。

### 5.2 配置前后

迁移前：

```properties
liteflow.agent.session.idle-timeout=30m
liteflow.agent.session.cleanup-interval=1m
liteflow.agent.session.max-sessions=10000
liteflow.agent.session.memory.mode=redis
liteflow.agent.session.memory.redis.bean-name=redissonClient
liteflow.agent.session.memory.redis.key-prefix=liteflow:agent:session
liteflow.agent.session.memory.load-on-first-use=true
liteflow.agent.session.memory.save-after-call=true
liteflow.agent.session.memory.save-on-error=true
```

迁移后：

```properties
liteflow.agent.runtime.namespace=order-service
liteflow.agent.runtime.default-user-id=anonymous
liteflow.agent.runtime.timeout=2m

liteflow.agent.state-store.type=bean
liteflow.agent.state-store.bean-name=sharedAgentStateStore
liteflow.agent.state-store.failure-policy=fail-fast

liteflow.agent.invocation-guard.mode=bean
liteflow.agent.invocation-guard.bean-name=distributedAgentGuard
liteflow.agent.invocation-guard.coordination-mode=distributed-guard
liteflow.agent.invocation-guard.strict-distributed=true
```

### 5.3 被删除或不再驱动 runtime 的配置

下列 1.x 配置不再驱动 2.0 runtime，应从业务配置中删除：

- `liteflow.agent.session.idle-timeout`
- `liteflow.agent.session.cleanup-interval`
- `liteflow.agent.session.max-sessions`
- `liteflow.agent.session.memory.mode`
- `liteflow.agent.session.memory.local-file.*`
- `liteflow.agent.session.memory.redis.*`
- `liteflow.agent.session.memory.mysql.*`
- `liteflow.agent.session.memory.load-on-first-use`
- `liteflow.agent.session.memory.save-after-call`
- `liteflow.agent.session.memory.save-on-error`

旧 DTO 暂时保留用于配置绑定诊断；只要显式绑定 `session.memory.*`，执行前就会报出 `session.memory -> state-store` 迁移错误。它们不是兼容运行路径。

`liteflow.agent.workspace.cleanup-on-session-expire` 和 `cleanup-on-jvm-shutdown` 也不注册 2.0 清理器或 JVM shutdown hook。需要归档／删除 workspace 时，由应用显式实现。

`liteflow.agent.skills.enabled/path/strict` 仍可作为应用配置 DTO，但 core 不会自动创建 repository；组件必须把 `path` 显式传给 `FileSystemSkillRepository`。其中 `strict` 当前不控制 repository 的错误降级。

### 5.4 数据迁移与多副本

旧 Session Memory 与新 AgentScope `AgentStateStore` 的数据结构不同，没有自动导入。上线前应选择：放弃旧短期会话；写一次性离线转换器；或灰度期间让旧服务只读旧格式、新服务写新 namespace。不要让两套 runtime 同时写同一个逻辑 key。

`MEMORY` 只适合单进程；`JSON` 适合本地开发／单机持久化。多副本共享 Store 时还需要 sticky routing 或真正跨进程的 `AgentInvocationGuard`。共享数据库本身不能防止同一会话并发覆盖。

## 6. Skills 与工具迁移

迁移前，`SkillBox` 同时承载 skill prompt、code execution 与工具解析。2.0.2 拆为：

- `AgentSkillRepository`：技能内容来源。
- `FileSystemSkillRepository`：文件目录实现。
- `SkillFilter`：按稳定 skill ID 过滤。
- `tools()`／`customizeToolkit()`：显式 Java 工具。
- Harness skills／memory／context files：需要完整上下文工程时使用。

```java
@Override
protected List<AgentSkillRepository> skillRepositories() {
    return List.of(new FileSystemSkillRepository(Path.of("/opt/app/skills"), false));
}

@Override
protected SkillFilter skillFilter() {
    return SkillFilter.all();
}
```

core 明确关闭隐式 skill code execution。`SKILL.md` 不再声明一个 Java 类让框架反射创建；需要依赖注入的工具必须作为 Spring／Solon Bean 注入组件，再由 `tools()` 返回。

## 7. Workspace、Shell 与 Harness sandbox

1.x 的 workspace path namespace 和 Shell 黑名单不是安全沙箱。2.0.2 默认关闭 core 文件工具与 Shell；只有组件开关和配置都显式允许时才注册。

| 选择 | 迁移建议 |
| --- | --- |
| 不需要文件／命令 | 保持组件开关 false，`shell.mode=DISABLED` |
| 可信本地开发 | 使用 guarded local，专用 root，显式 `trusted-local=true`，Shell 使用最小 whitelist |
| 不可信命令 | 使用 Harness `DOCKER` 或经过审计的 `CUSTOM` remote filesystem |

guarded local 只提供路径规范化、real-root／symlink、大小与 namespace 检查，文件仍由宿主 JVM 权限访问。Docker 默认减权配置也不能替代镜像、daemon、host projection 和内核层安全治理。

迁移到 Harness 时，子 Agent 必须使用 `List<SubagentDeclaration>`；compaction、memory、skills、task repository、plan mode 与 tool eviction 都通过 `HarnessAgentComponent` 的强类型 hook 配置。guarded local 会禁用本地子 Agent；需要子 Agent 时选择适当的隔离后端。

## 8. HITL 行为变化

2.0.2 的 ASK 不是一个回调后自动继续的布尔开关。一次 LiteFlow `process()` 内会至少执行两次完整 Agent 调用：第一次产生 ASK reply，handler 返回与所有 pending tool 精确对应的 `ConfirmResult`，第二次使用 metadata-only `UserMessage` 恢复。整个过程持有同一 state／workspace lease。

迁移检查：

- 为 ASK 工具配置 `AgentConfirmationHandler`，不要默认自动批准。
- handler 的结果必须无缺失、无重复、无未知 ID，并保留 tool name／input。
- 分别设置 `runtime.timeout` 与 `hitl.confirmation-timeout`。
- 决定 `hitl.fail-on-denied-tool` 是让 Agent 继续回答还是让调用以 `PERMISSION` 失败。
- 把外部取消映射到 Reactor cancellation；不要另开 fire-and-forget subscription。

## 9. A2A 迁移边界

A2A client 的 upstream `A2aAgent` 2.0.2 含 current request 等可变字段，不能继续采用一个实例服务全部远端请求。LiteFlow 的 A2A 组件 runtime 只缓存 resolver/config binding，每次 subscription 新建一个 client Agent，并在 timeout／cancel 时只 interrupt 对应实例。

A2A server 每个 active `taskId` 也拥有一个独立 runtime。`LiteFlowA2aServerFactory` 只创建协议对象，不绑定 Web endpoint、不监听端口、不调用 `postEndpointReady()`；迁移旧 server bootstrap 时，必须把 endpoint、认证、TLS、限流和 ready 回调放回业务框架。

AgentScope 2.0.2 的 server `AgentRunner` 仍要求 coarse `Event`，因此 `A2aProtocolEventAdapter` 是唯一允许使用旧 `Event`／`EventType` 的生产边界。其它代码必须使用 `streamEvents(..., RuntimeContext)` 与类型化 `AgentEvent`。A2A server 尚无远端确认协议，`RequireUserConfirmEvent` 会失败而不是自动批准。

## 10. 生命周期与 ownership

1.x 每会话 Agent 缓存和 session eviction 不再负责资源关闭。2.0.2 每个组件实例拥有一个 lazy runtime：

- Spring 销毁和 Solon bean stop 通过 `Closeable.close()` 等待 in-flight 调用并幂等关闭。
- 非容器应用必须显式 `close()`。
- `ownsMcpClient`、`ownsSkillRepository`、`ownsTaskRepository` 等 hook 明确 borrowed／owned。
- 构建失败逆序回滚 owned 资源，主错误保留，清理错误作为 suppressed。
- 关闭后组件不能重建；不要使用静态 component registry 或自行添加 JVM shutdown hook。

## 11. 推荐迁移顺序

1. 固定 JDK 17 与 AgentScope BOM 2.0.2，移除 aggregate／旧 model 依赖。
2. 迁移一个最小组件的显式 context 签名，让离线 deterministic model 测试通过。
3. 把 Hook 改为 Middleware，并验证 error、cancel、timeout 的 terminal cleanup。
4. 配置 `runtime.namespace` 与新 StateStore；用两个 conversation、两个 agentKey 验证 namespace。
5. 迁移结构化输出、usage、skills、MCP、retry／fallback 与 HITL。
6. 关闭旧本地工具，再按风险选择 guarded local、Docker 或 remote backend。
7. 最后迁移 Harness 与 A2A，并保持默认测试不访问网络、Provider、Docker 或真实 endpoint。

## 12. 验收清单

- JDK 17 下 `mvn test`／`package` 通过，且没有测试 skip／assumption 造绿。
- 依赖树中所有 AgentScope 工件为 2.0.2，无 `io.agentscope:agentscope` aggregate。
- 生产源码除 `A2aProtocolEventAdapter` 外没有旧 coarse event 类型。
- 组件 runtime 构建一次，每调用 `LiteFlowAgentContext`／`RuntimeContext` 都是新对象。
- 相同 identity 的 state 串行，不同 agentKey 状态隔离；workspace 隔离维度符合预期。
- StateStore load failure 在模型前按策略处理，terminal 后无残留 failure／Slot attachment／lease。
- HITL 真实走首轮与 metadata-only continuation，批准、拒绝、超时和取消均被测试。
- guarded local 被文档和配置明确标为宿主机路径防护，不宣传为安全沙箱。
- Docker、live Provider 与真实 A2A transport 只在显式 profile／授权环境运行；未执行时如实记录。
