# LiteFlow ReAct Agent 使用指南

本文介绍如何在 LiteFlow 中使用 `liteflow-react-agent`，把 agentscope-java 的 `ReActAgent` 当作普通 LiteFlow 节点编排到 EL 链路中。

读完本文后，你应该能够：

- 引入对应模型平台模块，并完成 `liteflow.agent.*` 配置；
- 编写一个继承 `ReActAgentComponent` 的 Agent 组件；
- 在 EL 中组合 Agent、普通节点、条件路由和并行节点；
- 正确理解 Session、memory、workspace、内置文件工具和 Shell 工具的边界。

> 当前仓库根版本：`2.15.3.2`。
>
> 当前源码中 `liteflow-react-agent` 子模块的 `maven.compiler.source` / `target` 为 `17`，根 `compile-17+` profile 会在 JDK 17 及以上激活该模块。实际运行时还需要满足 agentscope-java 及具体模型 SDK 的运行要求。

---

## 1. 模块说明

`liteflow-react-agent` 是一个聚合模块，核心能力在 `liteflow-react-agent-core`，不同模型供应商由独立子模块提供便捷入口。

| 模块 | 作用 |
| --- | --- |
| `liteflow-react-agent-core` | `ReActAgentComponent`、`ModelSpec` 基础设施、Session 管理、memory 持久化、workspace 文件工具、受管 Shell 工具 |
| `liteflow-react-agent-openai` | OpenAI 官方 API + OpenAI 兼容协议，内置 DeepSeek、Kimi、GLM、MiniMax 便捷入口 |
| `liteflow-react-agent-anthropic` | Anthropic Claude 模型入口 |
| `liteflow-react-agent-gemini` | Google Gemini 模型入口 |
| `liteflow-react-agent-dashscope` | 阿里云 DashScope / Qwen 模型入口 |

业务项目通常只需要引入一个平台模块。平台模块会传递依赖 `liteflow-react-agent-core`。

---

## 2. 快速开始

### 2.1 引入依赖

以 DeepSeek 这类 OpenAI 兼容平台为例：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-openai</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

如果一条应用里要同时使用多个模型平台，可以同时引入多个平台模块。

### 2.2 配置 LiteFlow 与 Agent

最小配置需要包含规则文件、workspace 根目录和模型凭据。生产环境建议先关闭 Shell 工具，再按需开启。

```properties
liteflow.rule-source=agent/flow.el.xml

liteflow.agent.workspace.root=/var/lib/liteflow/agent-workspaces
liteflow.agent.shell.mode=disabled

liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
liteflow.agent.openai-compatible.deepseek.base-url=https://api.deepseek.com/v1
```

`liteflow.agent.workspace.root` 是必填项。没有配置时，首次执行 Agent 组件会抛出：

```text
AgentConfigException: liteflow.agent.workspace.root is required
```

### 2.3 编写 Agent 组件

Agent 组件继承 `ReActAgentComponent`，至少实现三个方法：

- `model(ctx)`：返回一个 `ModelSpec<?>`，声明使用哪个平台、哪个模型及可选高级参数；
- `systemPrompt(ctx)`：创建 Agent 时使用的系统提示词；
- `userPrompt(ctx)`：每次调用时发送给 Agent 的用户消息。

```java
package demo.agent;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import org.springframework.stereotype.Component;

@Component("deepseekAgent")
public class DeepSeekAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DeepSeek.of("deepseek-chat");
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是一名简洁的中文助理，回答严格控制在两句话以内。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override
    protected boolean enableShellTool() { return false; }

    @Override
    protected boolean enableWorkspaceFileTools() { return false; }
}
```

### 2.4 在 EL 中编排

Agent 节点和普通 `NodeComponent` 一样使用：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE flow PUBLIC "liteflow" "liteflow.dtd">
<flow>
    <chain name="deepseekChain">
        THEN(prepare, deepseekAgent, recordReply);
    </chain>
</flow>
```

调用方式也和普通 LiteFlow 链路一致：

```java
LiteflowResponse response = flowExecutor.execute2Resp("deepseekChain", "用一句话介绍 LiteFlow");
if (response.isSuccess()) {
    Object reply = response.getSlot().getResponseData();
}
```

默认情况下，Agent 回复会通过 `reply.getTextContent()` 写入 `slot.responseData`。如果后续节点希望从指定位置读取回复，可以覆写 `handleReply(reply, ctx)`，或者像测试用例中的 `RecordReplyCmp` 一样把 `responseData` 转存到节点输出。

### 2.3 下游节点如何拿到 Agent 的执行结果

ReAct Agent 节点执行完后，结果传递给下一个节点有两种方式：

**方式 1：默认走 `slot.responseData`（最简单）**

`ReActAgentComponent#handleReply()` 默认实现：

```java
protected void handleReply(Msg reply, ReActAgentContext ctx) {
    ctx.getSlot().setResponseData(reply == null ? null : reply.getTextContent());
}
```

下一个普通 `NodeComponent` 直接读取即可：

```java
@LiteflowComponent("recordReply")
public class RecordReplyCmp extends NodeComponent {
    @Override
    public void process() {
        String reply = (String) this.getSlot().getResponseData();
        // 进行后续处理 ...
    }
}
```

链路结束后，外部调用方也可以通过 `response.getSlot().getResponseData()` 拿到。

**方式 2：覆写 `handleReply` 写入自定义位置**

需要做结构化处理、写到 ContextBean、或者一个链路里有**多个 Agent 节点**时，必须覆写 `handleReply`，否则后一个 Agent 的 `responseData` 会覆盖前一个：

```java
@Override
protected void handleReply(Msg reply, ReActAgentContext ctx) {
    String text = reply == null ? null : reply.getTextContent();
    // 选择 1：写入自定义 ContextBean
    ctx.getSlot().getContextBean(MyAgentCtx.class).setReply(getNodeId(), text);
    // 选择 2：以 nodeId 为 key 存到 slot 输出，避免相互覆盖
    ctx.getSlot().setOutput(getNodeId(), text);
}
```

下游节点对应使用 `slot.getContextBean(MyAgentCtx.class)` 或 `slot.getOutput(nodeId)` 读取。

> **多 Agent 节点共存的注意事项**：默认 `responseData` 是 slot 级别的单一字段，后写覆盖先写。链路中存在多个 ReAct Agent 时，请务必覆写 `handleReply` 用 `setOutput(nodeId, ...)` 或自定义 ContextBean 区分各 Agent 的输出。

---

## 3. ReActAgentComponent 扩展点

`ReActAgentComponent#process()` 是 `final`。框架在其中统一完成配置读取、Session 获取、加锁、Agent 懒构建、调用、回复处理和 memory 保存。业务侧通过覆写受保护方法定制行为。

| 方法 | 是否必须 | 默认行为 | 说明 |
| --- | --- | --- | --- |
| `model(ctx)` | 是 | 无 | 返回 `ModelSpec<?>`，由框架从 `AgentConfig` 解析凭据并构造 agentscope `Model` |
| `systemPrompt(ctx)` | 是 | 无 | 返回系统提示词，同一 Session 首次构建 Agent 时调用 |
| `userPrompt(ctx)` | 是 | 无 | 返回本轮用户消息，每次 `process()` 都调用 |
| `tools(ctx)` | 否 | 空列表 | 注册自定义 `@Tool` 对象 |
| `resolveSessionId(slot)` | 否 | `NanoIdSessionIdGenerator.generate()`，格式 `YYYYMMDD_NanoId(12)` | 决定本次调用使用哪个 Session；默认每次调用生成新 ID（单轮无状态） |
| `maxIterations()` | 否 | `-1` | 返回正数时覆盖全局 `defaults.max-iterations` |
| `enableShellTool()` | 否 | `true` | 是否注册内置受管 Shell 工具 |
| `enableWorkspaceFileTools()` | 否 | `true` | 是否注册内置 workspace 文件工具 |
| `hooks(ctx)` | 否 | 空列表 | 注册 agentscope `Hook` |
| `enableReActLogging()` | 否 | 读 `liteflow.agent.logging.react-enabled`（默认 `true`） | 是否注册内置 `ReActLoggingHook`，将 reason / act / error 事件写到日志 |
| `handleReply(reply, ctx)` | 否 | 写入 `slot.responseData` | 自定义回复处理逻辑 |
| `buildModel(ctx)` | 否 | 委派 `model(ctx).resolve(agentConfig())` | 逃生舱：完全自行构造 agentscope `Model` |

`ReActAgentContext` 提供三项执行上下文：

| 方法 | 说明 |
| --- | --- |
| `getSlot()` | 当前 LiteFlow `Slot` |
| `getSessionId()` | 安全化后的 Session ID |
| `getWorkspaceDir()` | 当前 Session 对应的 workspace 目录 |

注意：`systemPrompt(ctx)` 只在同一 `sessionId` 下首次构建 Agent 时调用；后续调用会复用同一个 Agent 实例和 memory。动态输入应放在 `userPrompt(ctx)` 中。

---

## 4. ModelSpec 与模型入口

### 4.1 核心设计

`ModelSpec<SELF>` 是所有平台模型描述符的基类。子类按"哪个平台 + 哪个模型 + 可选高级参数"三段式给出，框架负责从 `AgentConfig` 解析凭据并构造 agentscope `Model`。

基类提供的共性参数（所有平台共享）：

| 方法 | 类型 | 说明 |
| --- | --- | --- |
| `temperature(double)` | `Double` | 采样温度 |
| `topP(double)` | `Double` | nucleus sampling |
| `topK(int)` | `Integer` | top-k sampling |
| `maxTokens(int)` | `Integer` | 最大输出 token |
| `seed(long)` | `Long` | 随机种子 |
| `stream(boolean)` | `Boolean` | 是否流式 |
| `cacheControl(boolean)` | `Boolean` | 缓存控制 |

所有参数均为可选，未设置时传 `null`，agentscope 使用服务端默认值。

### 4.2 平台入口一览

每个平台模块提供一个不可变入口类，通过静态 `of(modelName)` 方法返回平台对应的 `Spec` 子类。Spec 子类在基类共性参数之上暴露平台个性参数。

| 模块 | 入口类 | Spec 子类 | 个性参数 |
| --- | --- | --- | --- |
| `liteflow-react-agent-openai` | `OpenAI` | `OpenAISpec` | `reasoningEffort`, `frequencyPenalty`, `presencePenalty` |
| `liteflow-react-agent-openai` | `DeepSeek` | `OpenAICompatibleSpec` | 继承 `OpenAISpec` 全部参数 |
| `liteflow-react-agent-openai` | `Kimi` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `GLM` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `Minimax` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `OpenAICompatible` | `OpenAICompatibleSpec` | 自定义 `configKey`，用于任意 OpenAI 兼容厂商 |
| `liteflow-react-agent-anthropic` | `Anthropic` | `AnthropicSpec` | `thinking(budget, enabled)` |
| `liteflow-react-agent-anthropic` | `AnthropicCompatible` | `AnthropicSpec` | 自定义 `configKey`，用于 Anthropic 兼容网关 |
| `liteflow-react-agent-gemini` | `Gemini` | `GeminiSpec` | `thinking(level, budget)` |
| `liteflow-react-agent-dashscope` | `DashScope` | `DashScopeSpec` | `thinking(budget)` |

### 4.3 使用示例

**OpenAI：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return OpenAI.of("gpt-4o")
            .temperature(0.7)
            .maxTokens(1000)
            .stream(true);
}
```

凭据来源：`liteflow.agent.openai.api-key`。

**DeepSeek（OpenAI 兼容）：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return DeepSeek.of("deepseek-chat")
            .temperature(0.5);
}
```

凭据来源：`liteflow.agent.openai-compatible.deepseek.api-key` / `base-url`。

**自定义 OpenAI 兼容厂商：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return OpenAICompatible.custom("myvendor", "my-model")
            .temperature(0.7);
}
```

凭据来源：`liteflow.agent.openai-compatible.myvendor.api-key` / `base-url`。

**Anthropic Claude：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return Anthropic.of("claude-sonnet-4-5")
            .temperature(0.5)
            .thinking(t -> t.budget(2000).enabled(true));
}
```

凭据来源：`liteflow.agent.anthropic.api-key`。

**Anthropic 兼容网关：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return AnthropicCompatible.custom("gateway", "claude-haiku");
}
```

凭据来源：`liteflow.agent.anthropic-compatible.gateway.api-key` / `base-url`。

**Google Gemini：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return Gemini.of("gemini-2.5-flash")
            .thinking(t -> t.level("high").budget(1024));
}
```

凭据来源：`liteflow.agent.gemini.api-key`。

**阿里云 DashScope：**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return DashScope.of("qwen-plus")
            .thinking(t -> t.budget(2048));
}
```

凭据来源：`liteflow.agent.dashscope.api-key`。

### 4.4 凭据配置结构

所有平台凭据都使用 `PlatformCredential`：

| 字段 | 说明 |
| --- | --- |
| `apiKey` | API Key |
| `baseUrl` | 可选，自定义网关或兼容端点 |
| `extra` | 可选，业务自定义键值 |

YAML 示例：

```yaml
liteflow:
  agent:
    workspace:
      root: /var/lib/liteflow/agent-workspaces
    shell:
      mode: disabled
    openai:
      api-key: ${OPENAI_API_KEY}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    gemini:
      api-key: ${GEMINI_API_KEY}
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    openai-compatible:
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
      kimi:
        api-key: ${KIMI_API_KEY}
        base-url: https://api.moonshot.cn/v1
    anthropic-compatible:
      gateway:
        api-key: ${ANTHROPIC_GATEWAY_API_KEY}
        base-url: https://anthropic-gateway.example.com
```

### 4.5 凭据解析规则

框架内建两种凭据解析策略，通过 `CredentialResolver` 实现：

- **头等平台**（`OpenAI` / `Anthropic` / `Gemini` / `DashScope`）：从 `AgentConfig` 的单一 `PlatformCredential` 字段读取（如 `cfg.getOpenai()`）。缺失时抛出 `AgentConfigException`，提示 `liteflow.agent.<platform>.api-key`。
- **兼容平台**（`DeepSeek` / `Kimi` / `GLM` / `Minimax` / `OpenAICompatible.custom`）：从 `AgentConfig` 的 `Map<String, PlatformCredential>` 中按 `configKey` 读取。缺失时抛出提示 `liteflow.agent.<type>.<configKey>.api-key`。

### 4.6 逃生舱：buildModel(ctx)

如果 `ModelSpec` 无法满足需求（例如需要传入 agentscope 原生的高级参数），可以直接覆写 `buildModel(ctx)`，完全自行构造 agentscope `Model`：

```java
@Override
protected Model buildModel(ReActAgentContext ctx) {
    return OpenAIChatModel.builder()
            .apiKey(agentConfig().getOpenai().getApiKey())
            .modelName("gpt-4o")
            .generateOptions(GenerateOptions.builder()
                    .temperature(0.7)
                    .build())
            .stream(true)
            .build();
}
```

覆写 `buildModel(ctx)` 后，`model(ctx)` 不再被 `process()` 调用，但仍建议实现以保持 API 一致性。

---

## 5. Session 与 memory

### 5.1 Session 负责什么

每次执行 Agent 组件时，框架会通过 `AgentSessionManager.acquire(sessionId)` 获取一个 `AgentSession`。同一个 Session 会复用：

- 同一个 `ReActAgent` 实例；
- 同一个 Agent memory；
- 同一个 workspace 子目录；
- 同一把 `ReentrantLock`。

因此，同一个 `sessionId` 下的调用会串行执行，避免多线程同时修改同一份 memory。不同 `sessionId` 可以并行执行。

### 5.2 Session ID 从哪里来

默认实现是：

```java
protected String resolveSessionId(Slot slot) {
    return NanoIdSessionIdGenerator.generate();
}
```

`NanoIdSessionIdGenerator.generate()` 返回 `YYYYMMDD_<12 位 NanoId>` 形式的字符串（例如 `20260430_3F7K9PQRSTUV`），每次调用都会生成一个全新的 ID。这意味着默认情况下"一次调用一个会话"，会话之间互不共享 memory 与 workspace。如果要实现多轮对话，必须覆写为业务会话 ID：

```java
@Override
protected String resolveSessionId(Slot slot) {
    ChatRequest req = slot.getChainReqData(slot.getChainId());
    return "chat-" + req.getUserId() + "-" + req.getConversationId();
}
```

Session ID 只允许直接使用 `[a-zA-Z0-9_-]+`。其他字符会被 URL 编码，并把 `%` 替换为 `_`，从而安全地用作目录名。空值会变成 `_`。

### 5.3 JVM 热 Session 配置

`liteflow.agent.session.*` 控制当前 JVM 中热 Session 的缓存时间和数量：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `idle-timeout` | `30m` | Session 空闲多久后可被清理 |
| `cleanup-interval` | `1m` | 后台清理线程检查间隔，最低 20ms |
| `max-sessions` | `10000` | 当前 JVM 中最多保留多少个热 Session |

清理线程名为 `liteflow-agent-session-cleaner`。它只会清理已经超过 `idle-timeout` 且当前没有被加锁的 Session。

当 Session 数量超过 `max-sessions` 时，会按 `lastActive` 淘汰最旧的 JVM 内缓存。这个淘汰只移除热 Agent 实例，不删除 workspace，也不删除 Redis、MySQL 或文件中的持久化记忆。

### 5.4 memory 持久化模式

`liteflow.agent.session.memory.*` 控制 Agent memory 保存在哪里。它和热 Session 缓存是两件事：热缓存决定当前 JVM 里保留多久，memory 持久化决定重启或重新加载后能否恢复对话历史。

| 模式 | 含义 | 适用场景 |
| --- | --- | --- |
| `JVM` | 默认值，使用 AgentScope 的 JVM 内 Session | 单进程内多轮对话，进程退出后可丢失 |
| `NONE` | 不加载也不保存持久化 Session | 不需要持久化；如需严格无状态，配合唯一 `sessionId` 使用 |
| `WORKSPACE_FILE` | 保存到 `workspace.root/.agent-session/<sessionId>/` | 本地文件持久化、开发测试、小规模部署 |
| `REDIS` | 使用用户提供的 Redis 客户端 Bean | 多实例共享短期会话 |
| `MYSQL` | 使用用户提供的 `DataSource` Bean | 需要数据库持久化和运维治理 |

通用开关：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `load-on-first-use` | `true` | 当前 JVM 首次构建某个 Session 的 Agent 时，尝试恢复历史 |
| `save-after-call` | `true` | 调用成功后保存记忆 |
| `save-on-error` | `true` | 调用抛错时也尝试保存记忆 |

`WORKSPACE_FILE` 示例：

```properties
liteflow.agent.session.memory.mode=WORKSPACE_FILE
liteflow.agent.session.memory.load-on-first-use=true
liteflow.agent.session.memory.save-after-call=true
liteflow.agent.session.memory.save-on-error=true
```

`REDIS` 示例：

```properties
liteflow.agent.session.memory.mode=REDIS
liteflow.agent.session.memory.redis.bean-name=redissonClient
liteflow.agent.session.memory.redis.client-type=REDISSON
liteflow.agent.session.memory.redis.key-prefix=liteflow:agent:session
```

`client-type` 可选 `REDISSON`、`JEDIS`、`LETTUCE`。LiteFlow 不创建 Redis 连接，必须由业务框架提供对应 Bean。

`MYSQL` 示例：

```properties
liteflow.agent.session.memory.mode=MYSQL
liteflow.agent.session.memory.mysql.data-source-bean-name=agentDataSource
liteflow.agent.session.memory.mysql.database-name=agentscope
liteflow.agent.session.memory.mysql.table-name=agentscope_sessions
liteflow.agent.session.memory.mysql.create-if-not-exist=false
```

LiteFlow 不创建 JDBC 连接池，`DataSource` 也需要由业务应用提供。

---

## 6. Workspace 与内置工具

### 6.1 Workspace 目录结构

每个 Session 都会在 `liteflow.agent.workspace.root` 下获得一个独立子目录：

```text
/var/lib/liteflow/agent-workspaces/
├── chat-user-1-conv-1/
├── chat-user-1-conv-2/
└── .agent-session/
```

`<sessionId>/` 是 Agent 工具读写文件的目录；`.agent-session/` 只在 `session.memory.mode=WORKSPACE_FILE` 时创建，用于保存 AgentScope 的记忆文件。

### 6.2 Workspace 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `root` | 无 | 必填，workspace 根目录 |
| `auto-create` | `true` | 根目录不存在时是否自动创建 |
| `cleanup-on-session-expire` | `true` | 空闲 Session 过期清理时是否删除对应 workspace 子目录 |
| `cleanup-on-jvm-shutdown` | `false` | `AgentSessionManager.close()` 时是否删除当前存活 Session 的 workspace |
| `max-file-bytes` | `10485760` | `read_file` 单次最多读取的字节数 |
| `max-list-size` | `1000` | `list_files` 单次最多返回的条目数 |

如果 Agent 生成的文件需要长期保留，例如报告、草稿、审计材料，应把 `cleanup-on-session-expire` 设为 `false`，并由业务侧做归档。

### 6.3 Workspace 文件工具

`enableWorkspaceFileTools()` 默认为 `true`。开启后，框架会注册 `WorkspaceFileTools`：

| 工具名 | 行为 |
| --- | --- |
| `read_file` | 读取当前 Session workspace 内的文本文件，超过 `max-file-bytes` 时截断 |
| `write_file` | 覆盖写入文本文件，并自动创建父目录 |
| `list_files` | 列出目录内容，最多返回 `max-list-size` 条 |
| `delete_file` | 删除文件 |

所有路径都必须是相对路径。绝对路径或 `..` 越界路径会被拒绝。

关闭方式：

```java
@Override
protected boolean enableWorkspaceFileTools() {
    return false;
}
```

### 6.4 受管 Shell 工具

`enableShellTool()` 默认为 `true`。如果配置中的 `liteflow.agent.shell.mode` 不是 `DISABLED`，框架会注册 `ManagedShellCommandTool`，工具名为 `execute_shell_command`。

Shell 工具在当前 Session workspace 下执行命令，使用空白符切分参数，并通过首 token 做白名单或黑名单判断。配置项如下：

| 配置项 | 默认值 |
| --- | --- |
| `mode` | `WHITELIST` |
| `whitelist` | `ls, cat, grep, find, head, tail, wc, sed, awk, python3, node` |
| `blacklist` | `rm, sudo, shutdown, mkfs, dd` |
| `timeout` | `30s` |
| `max-output-bytes` | `1048576` |

生产环境建议默认关闭：

```properties
liteflow.agent.shell.mode=disabled
```

确实需要 Shell 时，优先使用 `WHITELIST`，并把白名单收窄到业务需要的命令。

---

## 6.5 ReAct 事件日志

框架内置 `ReActLoggingHook`，把 agent 的 `reason` / `act` / `error` 事件写入 SLF4J，便于在终端直接观察 ReAct 推理过程：

| 事件 | 日志格式 |
| --- | --- |
| `PreReasoningEvent` | `[agent:reason][sid] >>> model=... messages=N` |
| `PostReasoningEvent` | `[agent:reason][sid] <<< text=... toolCalls=[...]` |
| `PreActingEvent` | `[agent:act][sid] >>> tool=... input=...` |
| `PostActingEvent` | `[agent:act][sid] <<< tool=... result=...` |
| `ErrorEvent` | `[agent:error][sid] ...` |

- 全局开关：`liteflow.agent.logging.react-enabled`（默认 `true`）。
- 单组件开关：覆写 `enableReActLogging()` 强制返回 `true` / `false`。
- 输出 logger 名：`com.yomahub.liteflow.agent.hook.ReActLoggingHook`（可在 logback / log4j2 中独立调级）。
- 文本字段超过 500 字会被截断为 `...(truncated)`。

如果业务侧已经通过自定义 `Hook` 处理事件流，建议关掉内置日志钩子以避免重复输出。

---

## 7. 常见编排方式

### 7.1 注册自定义工具

自定义工具是普通对象，方法上使用 agentscope 的 `@Tool` 和 `@ToolParam`：

```java
package demo.agent.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class OrderTool {

    @Tool(name = "query_order_status", description = "Query order status by order number")
    public String query(@ToolParam(name = "orderNo") String orderNo) {
        return "订单 " + orderNo + " 正在处理中";
    }
}
```

在 Agent 组件里注册：

```java
@Override
protected List<Object> tools(ReActAgentContext ctx) {
    return List.of(new OrderTool());
}
```

### 7.2 用 IF 做路由

```xml
<chain name="routerChain">
    THEN(
        prepare,
        IF(isMath, mathAgent, deepseekAgent),
        recordReply
    );
</chain>
```

`isMath` 可以是普通的 `NodeBooleanComponent`，`mathAgent` 和 `deepseekAgent` 都是 `ReActAgentComponent` 子类。

### 7.3 用 WHEN 并行调用多个模型

```xml
<chain name="parallelChain">
    THEN(
        prepare,
        WHEN(deepseekAgent, dashscopeAgent).maxWaitSeconds(60),
        recordReply
    );
</chain>
```

如果两个 Agent 组件返回同一个 `sessionId`，它们会因为同一把 Session 锁而串行执行。要真正并行，确保不同组件使用不同 Session ID，例如在 `resolveSessionId` 中拼入节点 ID：

```java
@Override
protected String resolveSessionId(Slot slot) {
    return getNodeId() + "-" + slot.getRequestId();
}
```

### 7.4 多轮对话

多轮对话的关键是让同一业务会话返回同一个 `sessionId`：

```java
@Override
protected String resolveSessionId(Slot slot) {
    ChatRequest req = slot.getChainReqData(slot.getChainId());
    return "chat-" + req.getUserId() + "-" + req.getConversationId();
}
```

只要多次执行传入相同的业务会话 ID，Agent 就会在同一个 Session 下复用 memory。是否能跨 JVM 重启恢复，取决于 `liteflow.agent.session.memory.mode`。

---

## 8. 完整配置速查

```properties
# LiteFlow 规则
liteflow.rule-source=agent/flow.el.xml

# workspace
liteflow.agent.workspace.root=/var/lib/liteflow/agent-workspaces
liteflow.agent.workspace.auto-create=true
liteflow.agent.workspace.cleanup-on-session-expire=true
liteflow.agent.workspace.cleanup-on-jvm-shutdown=false
liteflow.agent.workspace.max-file-bytes=10485760
liteflow.agent.workspace.max-list-size=1000

# 当前 JVM 中的热 Session
liteflow.agent.session.idle-timeout=30m
liteflow.agent.session.cleanup-interval=1m
liteflow.agent.session.max-sessions=10000

# Agent memory 持久化
liteflow.agent.session.memory.mode=JVM
liteflow.agent.session.memory.load-on-first-use=true
liteflow.agent.session.memory.save-after-call=true
liteflow.agent.session.memory.save-on-error=true

# Redis memory，仅 mode=REDIS 时需要
liteflow.agent.session.memory.redis.bean-name=redissonClient
liteflow.agent.session.memory.redis.client-type=REDISSON
liteflow.agent.session.memory.redis.key-prefix=liteflow:agent:session

# MySQL memory，仅 mode=MYSQL 时需要
liteflow.agent.session.memory.mysql.data-source-bean-name=agentDataSource
liteflow.agent.session.memory.mysql.database-name=agentscope
liteflow.agent.session.memory.mysql.table-name=agentscope_sessions
liteflow.agent.session.memory.mysql.create-if-not-exist=false

# Shell 工具
liteflow.agent.shell.mode=disabled
liteflow.agent.shell.timeout=30s
liteflow.agent.shell.max-output-bytes=1048576

# ReAct 内部事件日志（reason / act / error）
liteflow.agent.logging.react-enabled=true

# ReAct 默认最大迭代次数
liteflow.agent.defaults.max-iterations=15

# 平台凭据
liteflow.agent.openai.api-key=${OPENAI_API_KEY}
liteflow.agent.anthropic.api-key=${ANTHROPIC_API_KEY}
liteflow.agent.gemini.api-key=${GEMINI_API_KEY}
liteflow.agent.dashscope.api-key=${DASHSCOPE_API_KEY}
liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
liteflow.agent.openai-compatible.deepseek.base-url=https://api.deepseek.com/v1
liteflow.agent.anthropic-compatible.gateway.api-key=${ANTHROPIC_GATEWAY_API_KEY}
liteflow.agent.anthropic-compatible.gateway.base-url=https://anthropic-gateway.example.com
```

---

## 9. 安全建议

1. 生产环境默认关闭 Shell：`liteflow.agent.shell.mode=disabled`。
2. `workspace.root` 使用专门目录，不要和业务源码、日志、密钥目录混放。
3. 不要直接把用户输入原样作为 Session ID。建议使用业务 ID 拼接、哈希或映射。
4. API Key 使用环境变量、配置中心或密钥管理系统，不要写入代码仓库。
5. 根据业务体量设置 `max-file-bytes`、`max-output-bytes`、`timeout` 和 `max-sessions`，避免 Agent 调用消耗不可控。
6. 选择 `REDIS` 或 `MYSQL` 记忆模式时，连接 Bean 由业务应用提供，权限和网络访问也应由业务应用控制。

---

## 10. 故障排查

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| `liteflow.agent.workspace.root is required` | 未配置 workspace 根目录 | 配置 `liteflow.agent.workspace.root` |
| `cannot create workspace root` | 根目录不可写，或父目录权限不足 | 换到应用可写目录，或提前创建并授权 |
| `workspace root does not exist` | `auto-create=false` 且目录不存在 | 提前创建目录，或开启 `auto-create` |
| `Missing API key: please configure liteflow.agent.openai.api-key` | 对应平台凭据未配置 | 配置对应平台的 `api-key` |
| 多轮对话没有记忆 | 默认 Session ID 由 `NanoIdSessionIdGenerator` 每次随机生成 | 覆写 `resolveSessionId`，返回稳定的业务会话 ID |
| 重启后没有历史记忆 | `session.memory.mode=JVM` 或 `NONE` | 改为 `WORKSPACE_FILE`、`REDIS` 或 `MYSQL` |
| Redis 模式启动失败 | 未配置 Bean 名，或 Bean 类型与 `client-type` 不匹配 | 检查 `bean-name`、`client-type` 和 classpath 依赖 |
| MySQL 模式启动失败 | 未配置 `DataSource` Bean，或 Bean 类型错误 | 配置正确的 `data-source-bean-name` |
| Shell 返回 `command 'xxx' not allowed by whitelist` | 白名单模式下命令未放行 | 加入白名单，或继续保持禁用 |
| `path escapes workspace` | 文件工具收到绝对路径或越界路径 | 使用相对路径，并限制在当前 workspace 内 |
| `WHEN` 中多个 Agent 看起来没有并行 | 多个组件共用了同一个 Session ID | 让不同 Agent 返回不同 `sessionId` |

---

## 11. 参考位置

- Core 模块：`liteflow-react-agent/liteflow-react-agent-core/`
- OpenAI 模块：`liteflow-react-agent/liteflow-react-agent-openai/`
- Anthropic 模块：`liteflow-react-agent/liteflow-react-agent-anthropic/`
- Gemini 模块：`liteflow-react-agent/liteflow-react-agent-gemini/`
- DashScope 模块：`liteflow-react-agent/liteflow-react-agent-dashscope/`
- 示例测试：`liteflow-testcase-el/liteflow-testcase-el-react-agent/`
- 规则示例：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/flow.el.xml`
