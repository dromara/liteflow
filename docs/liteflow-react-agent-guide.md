# LiteFlow ReAct Agent 使用指南

本文档面向使用者，介绍如何在 LiteFlow 中使用 ReAct Agent 能力：接入大模型、声明工具、编排多 Agent、管理多轮会话。文中不讲内部实现原理，所有示例均来自当前代码库的真实用法。

## 1. 简介

`liteflow-react-agent` 是 LiteFlow 的 Agent 扩展模块，基于 AgentScope Java（2.0.2）构建。它把一个大模型 ReAct Agent 封装成一个普通的 LiteFlow 组件，从而可以：

- 用 LiteFlow EL（`THEN` / `WHEN` / `IF` / `SWITCH` 等）自由编排一个或多个 Agent，以及普通业务组件。
- 通过统一的 `ModelSpec` 描述符接入各家大模型平台，凭据走配置，参数走代码。
- 获得开箱即用的多轮对话记忆、工具调用、流式事件、结构化输出、人工确认（HITL）等能力。

**运行要求**：JDK 17+。当前版本组合为 LiteFlow `2.16.1.1` + AgentScope `2.0.2`。

**模块清单**（Maven groupId 均为 `com.yomahub`）：

| 模块 | 作用 |
| --- | --- |
| `liteflow-react-agent-core` | Agent 组件基类、模型抽象、工具、会话状态、事件、HITL 等核心能力 |
| `liteflow-react-agent-openai` | OpenAI 及 OpenAI 兼容平台（DeepSeek、Kimi、GLM、MiniMax、任意兼容端点） |
| `liteflow-react-agent-anthropic` | Anthropic 及 Anthropic 兼容网关 |
| `liteflow-react-agent-dashscope` | 阿里云百炼（DashScope / 通义千问） |
| `liteflow-react-agent-gemini` | Google Gemini |
| `liteflow-react-agent-harness` | 可选增强：上下文压缩、记忆、技能、子代理、计划模式、沙箱文件系统 |
| `liteflow-react-agent-a2a` | 可选：A2A（Agent-to-Agent）协议客户端 |
| `liteflow-react-agent-redis` | 可选：Redis 会话状态存储（基于 agentscope-extensions-redis） |
| `liteflow-react-agent-mysql` | 可选：MySQL 会话状态存储（基于 agentscope-extensions-mysql） |

使用时按「core + 至少一个平台模块」组合引入，平台模块会自动带入 core。

**目录**

- [1. 简介](#1-简介)
- [2. 快速开始](#2-快速开始)
- [3. 接入模型平台](#3-接入模型平台)
- [4. 为 Agent 添加工具](#4-为-agent-添加工具)
- [5. 多轮对话与状态持久化](#5-多轮对话与状态持久化)
- [6. 流式输出与事件监听](#6-流式输出与事件监听)
- [7. 结构化输出](#7-结构化输出)
- [8. 中间件](#8-中间件)
- [9. 多 Agent 编排](#9-多-agent-编排)
- [10. 人工确认（HITL）](#10-人工确认hitl)
- [11. Skills（技能）](#11-skills技能)
- [12. Harness 模块：上下文工程与沙箱](#12-harness-模块上下文工程与沙箱)
- [13. A2A：调用远程 Agent](#13-a2a调用远程-agent)
- [14. 可靠性与错误处理](#14-可靠性与错误处理)
- [15. 离线测试自己的 Agent 链](#15-离线测试自己的-agent-链)
- [16. 配置速查](#16-配置速查)
- [17. 组件扩展点速查](#17-组件扩展点速查)
- [18. 常见报错与排查](#18-常见报错与排查)

## 2. 快速开始

以一个 Spring Boot 应用接入 DeepSeek 为例，共 5 步。

### 2.1 引入依赖

```xml
<!-- LiteFlow Spring Boot 集成（agent 模块本身不含自动装配，需要它） -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-spring-boot-starter</artifactId>
    <version>${liteflow.version}</version>
</dependency>

<!-- 平台模块：按需选择一个或多个 -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-openai</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

通常**不需要**自己引入任何 AgentScope 依赖——liteflow-react-agent 的模块会把用到的 AgentScope 工件（core、各平台扩展等）传递进来，代码里直接 import 即可编译。

若因特殊需要自行添加 AgentScope 工件，注意两点：不要引入 `io.agentscope:agentscope` 聚合包；版本需与 LiteFlow 传递引入的版本一致（当前为 2.0.2），避免类路径上混用多个版本。

### 2.2 最小配置

```properties
# LiteFlow 规则文件
liteflow.rule-source=agent/flow.el.xml

# 必填：agent 运行时命名空间，用于隔离会话状态
liteflow.agent.runtime.namespace=my-service

# 平台凭据（建议从环境变量注入）
liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
```

`liteflow.agent.runtime.namespace` 是必填项，缺失时首次执行会抛出 `liteflow.agent.runtime.namespace is required before execution`。

### 2.3 编写 Agent 组件

继承 `ReActAgentComponent`，实现 3 个抽象方法：

```java
@Component("chatAgent")
public class ChatAgentCmp extends ReActAgentComponent {

    // 用哪个模型：返回平台入口类构造的 ModelSpec
    @Override
    protected ModelSpec<?> model() {
        return DeepSeek.of("deepseek-chat").temperature(0.1);
    }

    // 系统提示词（会追加在框架内置约定之后）
    @Override
    protected String systemPrompt() {
        return "你是一个简洁的助手，用中文回答。";
    }

    // 用户提示词：通常取链的请求参数
    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}
```

### 2.4 在 EL 中编排

Agent 组件就是普通节点，按 bean 名引用：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE flow PUBLIC "liteflow" "liteflow.dtd">
<flow>
    <chain name="chatChain">
        THEN(chatAgent);
    </chain>
</flow>
```

### 2.5 执行并获取结果

```java
@Resource
private FlowExecutor flowExecutor;

public void chat() {
    LiteflowResponse response = flowExecutor.execute2Resp("chatChain", "介绍一下 LiteFlow");
    if (response.isSuccess()) {
        // Agent 的最终答复默认写入 slot 的 responseData
        Object reply = response.getSlot().getResponseData();
        System.out.println(reply);
    }
}
```

至此一个最小 Agent 链路已经可以运行。下面的章节按需阅读。

## 3. 接入模型平台

### 3.1 平台一览

所有平台都通过「入口类的静态工厂 + 链式参数」构造 `ModelSpec`，凭据从 `liteflow.agent.*` 配置读取：

| 平台 | 入口（组件 `model()` 中调用） | 所属模块 | 配置前缀 |
| --- | --- | --- | --- |
| OpenAI | `OpenAI.of("gpt-4o-mini")` | openai | `liteflow.agent.openai.*` |
| Anthropic | `Anthropic.of("claude-3-5-haiku-latest")` | anthropic | `liteflow.agent.anthropic.*` |
| Gemini | `Gemini.of("gemini-2.5-flash")` | gemini | `liteflow.agent.gemini.*` |
| DashScope | `DashScope.of("qwen-plus")` | dashscope | `liteflow.agent.dashscope.*` |
| DeepSeek | `DeepSeek.of("deepseek-chat")` | openai | `liteflow.agent.openai-compatible.deepseek.*` |
| Kimi | `Kimi.of("moonshot-v1-8k")` | openai | `liteflow.agent.openai-compatible.kimi.*` |
| GLM | `GLM.of("glm-4")` | openai | `liteflow.agent.openai-compatible.glm.*` |
| MiniMax | `Minimax.of("MiniMax-Text-01")` | openai | `liteflow.agent.openai-compatible.minimax.*` |
| 任意 OpenAI 兼容端点 | `OpenAICompatible.custom("my-platform", "model-x")` | openai | `liteflow.agent.openai-compatible.my-platform.*` |
| 任意 Anthropic 兼容网关 | `AnthropicCompatible.custom("gateway", "model-x")` | anthropic | `liteflow.agent.anthropic-compatible.gateway.*` |

每个凭据配置段支持 `api-key`、`base-url`、`extra.*`（自定义键值）三项。

凭据解析规则：

- 头等平台（openai / anthropic / gemini / dashscope）：只需 `api-key`，`base-url` 缺省用官方端点。
- 内置预设（deepseek / kimi / glm / minimax）：内置默认 `base-url`，通常只配 `api-key`；需要覆盖时再加 `base-url`。
- 自定义兼容端点（`OpenAICompatible.custom` / `AnthropicCompatible.custom`）：`api-key` 和 `base-url` 都必填，缺失会在构建期抛出 `AgentConfigException`。
- 代码里调用 `.apiKey(...)` / `.baseUrl(...)` 显式设置的值优先于配置文件。

### 3.2 通用模型参数

所有 `ModelSpec` 共有的链式参数：

```java
OpenAI.of("gpt-4o-mini")
        .temperature(0.1)        // 温度
        .topP(0.9)               // topP
        .topK(50)                // topK
        .maxTokens(1024)         // 最大输出 token
        .seed(42L)               // 随机种子
        .stream(true)            // 模型层流式
        .parallelToolCalls(true) // 并行工具调用
        .additionalHeader("k", "v")      // 附加请求头
        .additionalBodyParam("k", "v");  // 附加请求体参数
```

完整列表：`apiKey`、`baseUrl`、`temperature`、`topP`、`topK`、`maxTokens`、`maxCompletionTokens`、`seed`、`stream`、`cacheControl`、`parallelToolCalls`、`executionConfig(...)`、`additionalHeader(s)`、`additionalBodyParam(s)`、`additionalQueryParam(s)`、`generateOptions(...)`。

单次调用超时不在 Spec 上：用 `executionConfig(ExecutionConfig)` 设置，或使用全局配置 `liteflow.agent.runtime.timeout`（默认 2 分钟）。

### 3.3 平台特有参数

```java
// OpenAI：推理强度、惩罚项、代理、原生结构化输出等
OpenAI.of("o4-mini").reasoningEffort("medium");

// Anthropic：原生 thinking（budget 为 token 预算，开启时必填）
Anthropic.of("claude-sonnet-4-5")
        .thinking(t -> t.enabled(true).budget(2048));

// DashScope：通义千问 thinking（enable_thinking + thinking_budget）
DashScope.of("qwen-plus")
        .thinking(t -> t.budget(512).enabled(true));

// Gemini：2.5 系列用 thinking_level（low/medium/high），老接口用 thinking_budget
Gemini.of("gemini-2.5-flash")
        .thinking(t -> t.level("medium").budget(321));
```

### 3.4 逃生舱：buildModel()

如果 `ModelSpec` 不能满足需求（自定义 SDK 封装、Mock 模型等），可以不覆写 `model()`，改为直接覆写 `buildModel()` 返回任意 AgentScope `io.agentscope.core.model.Model` 实例：

```java
@Override
protected ModelSpec<?> model() {
    throw new UnsupportedOperationException("use buildModel");
}

@Override
protected Model buildModel() {
    return myCustomModel; // 任何 Model 实现
}
```

离线测试（不调用真实大模型）正是基于这个机制，见第 15 章。

## 4. 为 Agent 添加工具

### 4.1 声明自定义工具

工具是普通的 Java 对象，方法上标注 AgentScope 的 `@Tool` / `@ToolParam` 注解（`io.agentscope.core.tool` 包），然后在组件中覆写 `tools()` 返回：

```java
@Component("orderAgent")
public class OrderAgentCmp extends ReActAgentComponent {

    @Override
    protected List<Object> tools() {
        return List.of(new OrderTool());
    }

    // model() / systemPrompt() / userPrompt() 略

    public static class OrderTool {
        @Tool(name = "query_order", description = "按订单号查询订单状态")
        public String queryOrder(@ToolParam(name = "orderId", description = "订单号") String orderId) {
            return "订单 " + orderId + " 已发货";
        }
    }
}
```

工具方法参数和返回值没有特殊限制，框架会把方法签名转换成模型可理解的 schema。

### 4.2 工具中需要依赖注入

把工具类声明为 Spring bean，注入组件后在 `tools()` 中返回：

```java
@Component
public class OrderTool {
    @Resource
    private OrderService orderService;

    @Tool(name = "query_order", description = "按订单号查询订单状态")
    public String queryOrder(@ToolParam(name = "orderId", description = "订单号") String orderId) {
        return orderService.query(orderId);
    }
}

@Component("orderAgent")
public class OrderAgentCmp extends ReActAgentComponent {
    @Resource
    private OrderTool orderTool;

    @Override
    protected List<Object> tools() {
        return List.of(orderTool);
    }
}
```

### 4.3 内置工具：AgentScope 文件工具与 Shell 工具

组件可以开启 AgentScope 内置的两类工具，默认关闭：

```java
@Override
protected boolean enableWorkspaceFileTools() { return true; } // 文件读写（view_text_file / list_directory / write_text_file / insert_text_file）

@Override
protected boolean enableShellTool() { return true; }          // execute_shell_command（仅白名单模式）
```

开启后需要对应配置：

```properties
# 文件工具：workspace.root 是内置工具的 baseDir，所有路径都被限制在该目录内，
# 启用时目录会自动创建；root 在同一组件的所有会话间共享，
# 需要按会话隔离工作区时请使用 harness 模块的 workspace / sandbox 体系。
liteflow.agent.workspace.root=/data/agent-workspace
liteflow.agent.workspace.trusted-local=true

# shell 工具：仅白名单模式；默认 disabled，开启 shell 工具时不能为 disabled
liteflow.agent.shell.mode=WHITELIST
```

命令过滤由 AgentScope 内置 `ShellCommandTool` 的 `UnixCommandValidator` 执行：只放行白名单中的首段命令，并拒绝包含 `&`、`|`、`;` 或换行的链式命令；单条命令的超时由模型按调用传入（默认 300 秒）。不满足约束时（例如未配置白名单就开启了 shell 工具），构建期会抛出 `AgentConfigException`。

### 4.4 MCP 工具与 Toolkit 微调

- 覆写 `mcpClients()` 返回 `McpClientWrapper` 列表，可以把 MCP 服务提供的工具接入 Agent。
- 覆写 `customizeToolkit(Toolkit)` 可以在工具装配完成后做后处理。
- 多个工具默认串行执行，配置 `liteflow.agent.toolkit.parallel=true` 可并行。

## 5. 多轮对话与状态持久化

### 5.1 会话身份：namespace、userId、conversationId、agentKey

一段对话由四元组唯一定位：`namespace + userId + conversationId + agentKey`。

- `namespace`：配置项 `liteflow.agent.runtime.namespace`，必填，通常一个应用一个。
- `userId`：默认取 `liteflow.agent.runtime.default-user-id`（默认 `anonymous`），可覆写组件的 `resolveUserId(Slot)` 按业务解析。
- `conversationId`：一次会话的标识，来源见下文。
- `agentKey`：默认取节点 nodeId，可覆写 `agentKey()` 自定义。同一链里多个 Agent 默认各自隔离记忆。

### 5.2 指定 conversationId 的四种方式

```java
// 方式一：显式指定
flowExecutor.execute2Resp("chatChain", "第二轮问题",
        ExecuteOption.of().conversationId("user-1024-task-abc"));

// 方式二：让框架生成，执行后取回复用
LiteflowResponse r = flowExecutor.execute2Resp("chatChain", "第一轮问题",
        ExecuteOption.of().autoConversationId());
String cid = r.getConversationId();

// 方式三：请求参数为 Map 时，放入 "conversationId" 键
flowExecutor.execute2Resp("chatChain", Map.of("conversationId", cid, "text", "你好"));

// 方式四：组件内覆写，按业务规则解析
@Override
protected String resolveConversationId(Slot slot) {
    return "user-" + getUserIdFromSomewhere();
}
```

同一 `conversationId` 的多次调用会自动带上历史消息，无需额外代码。同一身份的并发调用会被串行化（见 14.4 并发守卫）。

### 5.3 StateStore：会话状态存在哪

所有后端都是持久化存储——会话记忆必须能跨重启续接，因此不提供非持久化的内存选项。

```properties
# JSON（默认，本地文件） / REDIS / MYSQL
liteflow.agent.state-store.type=JSON

# type=JSON 时的存储根目录，默认 ./data/agent-state
liteflow.agent.state-store.json-root=/data/agent-state

# 存储失败策略：FAIL_FAST（默认） / LOG_AND_CONTINUE
liteflow.agent.state-store.failure-policy=FAIL_FAST
```

#### REDIS：引入 liteflow-react-agent-redis 模块

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-redis</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

```properties
liteflow.agent.state-store.type=REDIS

# 方式一：直连 URI（框架创建并持有 Lettuce 客户端）
liteflow.agent.state-store.redis.uri=redis://localhost:6379

# 方式二：复用容器中已有的客户端 bean（Jedis UnifiedJedis / Lettuce RedisClient
# 或 RedisClusterClient / Redisson RedissonClient），二选一
#liteflow.agent.state-store.redis.client-bean-name=redissonClient

# 可选：Redis 内的 key 前缀，默认沿用 agentscope 扩展的 agentscope:session:
#liteflow.agent.state-store.redis.key-prefix=liteflow:agent:state:
```

#### MYSQL：引入 liteflow-react-agent-mysql 模块

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-mysql</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

```properties
liteflow.agent.state-store.type=MYSQL

# 方式一：复用应用的连接池 DataSource bean（生产推荐）
#liteflow.agent.state-store.mysql.data-source-bean-name=dataSource

# 方式二：直连 JDBC url（框架创建并持有 DataSource）
liteflow.agent.state-store.mysql.jdbc-url=jdbc:mysql://localhost:3306/liteflow
liteflow.agent.state-store.mysql.username=liteflow
liteflow.agent.state-store.mysql.password=secret

# 可选：库表与自动建表，默认 agentscope / agentscope_sessions / false
liteflow.agent.state-store.mysql.database-name=liteflow
liteflow.agent.state-store.mysql.table-name=agent_state
liteflow.agent.state-store.mysql.create-if-not-exist=true
```

#### 自定义存储

需要其他后端（如 MongoDB）时，覆写组件的 `stateStoreResolver()` 返回自定义 `AgentStateStore` 装配：

```java
@Override
protected AgentStateStoreResolver stateStoreResolver() {
    return config -> new ResolvedAgentStateStore(new MongoAgentStateStore(...), true);
}
```

#### 存储内容与清理

无论哪种后端，每个 (userId, 会话) 对应一个独立的「slot」，内容固定两块：

- `agent_state`：Agent 运行状态（权限规则、计划、任务等）；
- `memory_messages`：对话历史消息——多轮记忆的来源。

LiteFlow 会在会话 ID 前再加一段由 `agentKey` 哈希而来的 agent 命名空间，因此同一会话与不同 Agent 的记忆天然隔离。各后端的实际落盘位置：

```text
JSON    <json-root>/<userId>/lf-<agent哈希>.lf-<会话哈希>/
          ├── agent_state.json
          ├── memory_messages.jsonl
          └── memory_messages.hash
Redis   <key-prefix><userId>/lf-<agent哈希>.lf-<会话哈希>:agent_state
        （消息列表额外有 :list 后缀键与 :_keys 索引键）
MySQL   表 agentscope_sessions，session_id 列 = <userId>:lf-<agent哈希>.lf-<会话哈希>，
        每个状态键一行 JSON
```

（JSON 后端里 userId 默认是 `anonymous`；含特殊字符的用户 ID 目录段会被编码为文件系统安全的形式。）

清理：LiteFlow 目前不提供会话过期与自动清理 API。删除某个会话即删除上述对应的 slot——JSON 删除该目录、Redis 删除该前缀下的键、MySQL 按 `session_id` 删除。由于目录与键名是哈希值、无法反查 conversationId，建议在业务侧保留「用户 → conversationId」的映射并按业务规则清理，或用周期任务按文件的更新时间归档。

## 6. 流式输出与事件监听

### 6.1 流式事件监听

执行 API 本身（`execute2Resp`）是阻塞返回的，流式效果通过事件监听实现。执行时注册一个 `FlowEventListener`：

```java
flowExecutor.execute2Resp("chatChain", "写一首诗",
        ExecuteOption.of().eventListener(event -> {
            switch (event.getType()) {
                case "agent.text.delta":     // 模型文本增量，逐段推送
                    System.out.print(event.getText());
                    break;
                case "agent.tool.call.end":  // 工具调用完成
                    System.out.println("\n[tool] " + event.getText());
                    break;
                case "agent.result":         // 最终结果（event.isLast() == true）
                    System.out.println("\n[done]");
                    break;
            }
        }));
```

常用事件类型：

| 事件 type | 含义 |
| --- | --- |
| `agent.start` / `agent.end` | Agent 调用开始 / 结束 |
| `agent.text.delta` | 模型输出文本增量（流式正文） |
| `agent.thinking.delta` | 思考内容增量 |
| `agent.tool.call.start` / `delta` / `end` | 工具调用入参的开始 / 增量 / 完成 |
| `agent.tool.result.start` / `delta` / `end` | 工具返回的开始 / 增量 / 完成 |
| `agent.confirm.required` / `agent.confirm.result` | HITL 确认请求 / 结果 |
| `agent.result` | 最终答复（`last=true`） |
| `agent.error` | 执行错误 |

事件对象 `FlowEvent` 上还有 `chainId`、`nodeId`、`conversationId`、`timestamp`、`data`（含原始 AgentScope 事件与调用元数据）等字段。监听器抛异常时的行为由 `liteflow.agent.event.listener-failure-mode` 控制：`FAIL_FAST`（默认，中断执行）或 `LOG_AND_CONTINUE`。

### 6.2 Token 用量统计（ChatUsage）

每次模型调用的 token 用量会自动累计到本次调用上下文，在组件的生命周期钩子（如 `handleReply`）中通过 `context.getChatUsage()` 读取：

```java
@Override
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    ChatUsage usage = context.getChatUsage();   // io.agentscope.core.model.ChatUsage
    if (usage != null) {
        metrics.record(getNodeId(), usage.getInputTokens(), usage.getOutputTokens());
    }
    super.handleReply(reply, context);
}
```

- `ChatUsage` 提供 `getInputTokens()` / `getOutputTokens()` / `getTotalTokens()` 三个读数。
- 一次 Agent 调用里发生多轮 ReAct 迭代（多次模型请求）时，用量是**累计值**。
- 用量只覆盖本次 `process()` 生命周期。跨会话、全局维度的成本统计，请在 `handleReply` 里上报到自己的监控系统完成。

## 7. 结构化输出

默认情况下 Agent 答复是纯文本。需要结构化结果时，二选一覆写（同时覆写会抛 `AgentConfigException`）：

```java
// 方式一：指定 Java 类型，responseData 直接是反序列化后的对象
public record StructuredReply(String answer, int score) {}

@Override
protected Class<?> structuredOutputType() {
    return StructuredReply.class;
}

// 方式二：指定 JSON Schema，responseData 为 JsonNode
@Override
protected JsonNode structuredOutputSchema() {
    return schema; // com.fasterxml.jackson.databind.JsonNode
}
```

执行后照常从 `response.getSlot().getResponseData()` 取结果，类型分别是 `StructuredReply` 或 `JsonNode`。模型无法产出合法结构时会抛出 `AgentInvocationException`。

## 8. 中间件

中间件可以在 Agent 调用、模型调用、工具执行等时机插入自定义逻辑（日志、鉴权、监控、改写输入等）。实现 AgentScope 的 `MiddlewareBase` 接口，在组件中注册：

```java
@Override
protected List<MiddlewareBase> middlewares() {
    return List.of(new MiddlewareBase() {
        @Override
        public int order() {
            return 10; // 数值越小越靠内层执行
        }

        @Override
        public Flux<AgentEvent> onAgent(Agent agent, RuntimeContext context,
                                        AgentInput input,
                                        Function<AgentInput, Flux<AgentEvent>> next) {
            // 前置逻辑
            return next.apply(input)
                    .doOnComplete(() -> { /* 后置逻辑 */ });
            // 返回 Flux.error(...) 可阻断本次调用，链执行失败
        }
    });
}
```

除 `onAgent` 外，`MiddlewareBase` 还有 `onReasoning`、`onActing`、`onModelCall`、`onSystemPrompt` 等切点，按需覆写。框架内置的日志、状态持久化、事件桥接等中间件会自动安装，无需关心。

## 9. 多 Agent 编排

Agent 组件是普通 LiteFlow 节点，可以和普通组件一起参与任何 EL 语法。

### 9.1 串行流水线

```xml
<chain name="multiAgentChain">
    THEN(searchAgent, summaryAgent, saveResultCmp);
</chain>
```

### 9.2 IF 路由：按条件选择 Agent

用一个普通 `NodeBooleanComponent` 做条件判断：

```java
@Component("isMathRequest")
public class IsMathRequestCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            return "math".equals(map.get("type"));
        }
        return false;
    }
}
```

```xml
<chain name="routeChain">
    THEN(
        IF(isMathRequest, mathAgent, generalAgent)
    );
</chain>
```

### 9.3 WHEN 并行：同时调用多个 Agent

```xml
<chain name="parallelChain">
    THEN(
        WHEN(agentA, agentB).maxWaitSeconds(120)
    );
</chain>
```

**注意**：默认情况下同一身份的调用会被并发守卫串行化，并行 Agent 需要把 `agentKey()` 区分到请求级别才能真正并行：

```java
@Override
protected String agentKey() {
    return "agentA__" + getSlot().getRequestId();
}
```

### 9.4 下游节点获取 Agent 结果

- Agent 的最终答复默认写入 `slot.getResponseData()`（链执行完毕后也可从 `response.getSlot().getResponseData()` 读取）。
- 下游普通组件可用 `getSlot().getOutput("agentNodeId")` 获取该 Agent 的输出，自行转存。
- 覆写 `handleReply(Msg, LiteFlowAgentContext)` 可以完全自定义答复的去向（例如写入自定义上下文而不是 responseData）：

```java
@Override
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    getMyContext().setAnswer(reply.getTextContent());
    // 不调用 super.handleReply(...) 则不再写入 responseData
}
```

### 9.5 一次执行里的多个 Agent 如何共享会话

同一次执行中的多个 Agent 共享 `conversationId`，但 `agentKey` 各自独立、记忆互不干扰。文件工具的 workspace root 是组件级共享目录（见 4.3 节），需要按会话或按 Agent 隔离文件时请使用 harness 模块。

## 10. 人工确认（HITL）

对于高危工具（如下单、删除、发消息），可以要求模型调用前先经人工确认。两步：

本节适用于 `ReActAgentComponent`，也适用于显式启用 HITL 的 `HarnessAgentComponent`。`HarnessAgentComponent` 未提供权限策略时默认使用 `PermissionMode.BYPASS`，不需要实现 `confirmationHandler()`；详见 [§12.1](#121-默认权限策略)。

第一步，用权限规则把工具标记为「需确认」（`ASK`）：

```java
@Override
protected PermissionContextState permissionContext() {
    return PermissionContextState.builder()
            .addAskRule("refund_order", new PermissionRule(
                    "refund_order", null, PermissionBehavior.ASK, "退款需人工确认"))
            .build();
}
```

第二步，提供确认处理器，在其中对接你的确认通道（IM 通知、审批系统等）：

```java
@Override
protected AgentConfirmationHandler confirmationHandler() {
    return (event, context) -> Mono.just(event.getToolCalls().stream()
            // ConfirmResult 第一个参数：true 允许执行，false 拒绝
            .map(tool -> new ConfirmResult(askHuman(tool), tool))
            .toList());
}
```

相关配置：

```properties
# 等待人工确认的超时时间，默认 2m
liteflow.agent.hitl.confirmation-timeout=5m
# 工具被拒绝时是否让链失败，默认 false（拒绝结果会回传给模型继续推理）
liteflow.agent.hitl.fail-on-denied-tool=true
```

## 11. Skills（技能）

技能是放在目录里的 `SKILL.md` 文件（YAML 头 + Markdown 正文），Agent 可以按需加载来扩展能力：

```markdown
---
name: demo
description: 演示技能，说明何时使用它
---

# Demo Skill

这里写技能的具体指令内容。
```

使用步骤：

```properties
liteflow.agent.skills.enabled=true
# 普通路径表示文件系统目录
liteflow.agent.skills.path=./skills
# 打包在应用资源中的技能可配置为：
# liteflow.agent.skills.path=classpath:agent/skills
```

LiteFlow 会根据路径自动创建 Repository，并在 Agent 运行时关闭或构建失败回滚时自动释放；组件不需要覆写任何 Repository 或 ownership 方法。

只有需要远程、共享或自定义 Repository 时才使用高级扩展点：

```java
@Override
protected List<SkillRepositoryRegistration> skillRepositoryRegistrations() {
    return List.of(
            SkillRepositoryRegistration.owned(customRepository),
            SkillRepositoryRegistration.borrowed(sharedRepository));
}

// 可选：只暴露部分技能
@Override
protected SkillFilter skillFilter() {
    return SkillFilter.only("demo", "another-skill");
}
```

本轮调用实际用到过哪些技能，可在 `handleReply` 中通过 `context.getUsedSkills()` 读取。

## 12. Harness 模块：上下文工程与沙箱

`liteflow-react-agent-harness` 在 ReAct 之上提供更多 AgentScope Harness 能力：上下文压缩、记忆、子代理、任务与计划、工具结果淘汰、沙箱文件系统等。用法与 `ReActAgentComponent` 一致，改为继承 `HarnessAgentComponent`：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-harness</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

```java
@Component("harnessAgent")
public class MyHarnessAgentCmp extends HarnessAgentComponent {
    // model() / systemPrompt() / userPrompt() 与 ReActAgentComponent 完全相同，
    // 工具、中间件、HITL API 等能力也一致；默认权限策略与 Harness 特有覆写点见下文。
}
```

### 12.1 默认权限策略

`HarnessAgentComponent` 未覆写 `permissionContext()`，或返回空的权限上下文时，LiteFlow 默认使用 `PermissionMode.BYPASS`。模型调用 Harness 工具时会直接执行，不触发人工确认，因此使用方不需要添加任何权限相关代码，也不需要实现 `confirmationHandler()`。

`BYPASS` 只跳过权限确认，不会关闭 Harness 的文件系统或 Docker 沙箱边界。路径限制、workspace 隔离、Docker 网络、CPU 与内存限制仍按配置生效。

如果状态存储中已有旧会话，且组件仍使用默认权限策略，LiteFlow 会在调用开始前把该会话中持久化的旧权限上下文同步为 `BYPASS`，避免升级后继续触发 `ASK`。

需要人工确认时，显式覆写 `permissionContext()` 并添加 `ASK` 规则，再按 [§10](#10-人工确认hitl) 实现 `confirmationHandler()`。显式提供的 `ALLOW`、`ASK`、`DENY` 等非空权限策略优先于 Harness 默认值，不会被改写成 `BYPASS`。

文件系统后端通过配置选择：

```properties
# GUARDED_LOCAL（默认，受控本地目录） / DOCKER（Docker 沙箱） / CUSTOM
liteflow.agent.harness.filesystem-backend=DOCKER
# GUARDED_LOCAL 后端必须显式确认信任本地文件系统
liteflow.agent.harness.trusted-local=true

# DOCKER 后端的沙箱参数（均有默认值）
liteflow.agent.harness.docker.image=ubuntu:22.04
liteflow.agent.harness.docker.workspace-root=/workspace
liteflow.agent.harness.docker.memory-size-bytes=536870912
liteflow.agent.harness.docker.cpu-count=1
liteflow.agent.harness.docker.network=none
```

### 12.2 上下文压缩（Compaction）

长对话超过阈值时自动把早期消息摘要成总结、保留最近消息，防止上下文超限：

```java
@Override
protected CompactionConfig compactionConfig() {
    return CompactionConfig.builder()
            .triggerMessages(80)      // 消息条数触发阈值，默认 50
            .triggerTokens(80_000)    // token 触发阈值，默认 0（不按 token 触发）
            .keepMessages(30)         // 压缩后保留最近 N 条消息，默认 20
            .keepTokensMin(2_000)     // 压缩后至少保留的 token 数，默认 2000
            .build();
}
```

摘要默认用主模型生成，可用 `.model(...)` 指定更便宜的模型执行。

### 12.3 长期记忆（Memory）

把对话中的关键信息抽取为长期记忆，供后续会话使用：

```java
@Override
protected MemoryConfig memoryConfig() {
    return MemoryConfig.builder()
            .model(DashScope.of("qwen-turbo").resolve(agentConfig()))  // 抽取记忆用的模型
            .build();
}
```

抽取时机、抽取提示词（flushPrompt）与记忆合并策略可通过 builder 继续调整。

### 12.4 工具结果淘汰（Tool Result Eviction）

过大的工具返回不会一直占用上下文，而是落盘到 workspace、只在上下文保留预览：

```java
@Override
protected ToolResultEvictionConfig toolResultEvictionConfig() {
    return ToolResultEvictionConfig.builder()
            .maxResultChars(80_000)   // 超过该长度的结果被淘汰，默认 80_000
            .previewChars(2_000)      // 上下文中保留的预览长度，默认 2_000
            .build();
}
```

### 12.5 子代理、任务与计划模式

子代理（subagent）是主 Agent 可以派生的下级 Agent，各自有独立的提示词与模型：

```java
@Override
protected List<SubagentDeclaration> subagents() {
    return List.of(SubagentDeclaration.builder()
            .name("researcher")
            .description("负责检索资料并汇总要点")
            .model("deepseek-chat")              // 可省略，默认继承主模型
            .inlineAgentsBody("你是资料检索专员，只输出要点清单。")  // 子代理系统提示词
            .build());
}

@Override
protected boolean enablePlanMode() { return true; }   // 计划模式：先产出计划再逐步执行
```

任务清单默认存放在 workspace（`taskRepository()` 可替换为自定义实现，如落库）；`additionalContextFiles()` 可把额外文件注入每次调用的上下文；`dockerSandboxClient()` 可自定义 Docker 沙箱客户端。更多可覆写点见 `HarnessAgentComponent` 的 javadoc。

## 13. A2A：调用远程 Agent

`liteflow-react-agent-a2a` 提供 A2A（Agent-to-Agent）协议**客户端**集成，用于在链中调用远程 Agent。继承 `A2aAgentComponent`：

```java
@Component("a2aAgent")
public class MyA2aAgentCmp extends A2aAgentComponent {

    @Override
    protected String remoteAgentName() {
        return "remote-assistant";
    }

    @Override
    protected AgentCardResolver agentCardResolver() {
        return myResolver; // AgentScope 的 AgentCardResolver，按名解析远程端点
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return getSlot().getChainReqData(getSlot().getChainId()).toString();
    }
}
```

约束：仅支持文本输出；输入为一条用户消息；每次调用都会创建独立的远程 Agent 实例，并自动携带 `userId`、`conversationId`、`agentKey`、`traceId` 元数据。

（本模块只做客户端。要把本地 Agent 暴露为 A2A 服务，可直接使用 AgentScope 的 `agentscope-extensions-a2a-server` 自行搭建。）

## 14. 可靠性与错误处理

### 14.1 重试与回退模型

```java
@Override
protected int maxRetries() { return 3; }          // 模型调用失败重试次数

@Override
protected Model fallbackModel() {                  // 主模型失败后的回退模型
    return DashScope.of("qwen-plus").resolve(agentConfig());
}
```

### 14.2 多模型路由

```java
@Override
protected List<Model> routingModels() {            // 候选模型池
    return List.of(cheapModel, strongModel);
}

@Override
protected Model routeModel(Model defaultModel, LiteFlowAgentContext context) {
    // 按本次调用选择用哪个模型
    return isHardQuestion(context) ? strongModel : cheapModel;
}
```

### 14.3 超时

单次 Agent 调用的总超时由 `liteflow.agent.runtime.timeout` 控制（默认 2 分钟），超时抛出 `AgentInvocationException(TIMEOUT)`。

### 14.4 并发守卫

同一身份（namespace + userId + conversationId + agentKey）的并发调用会被串行化，防止会话状态错乱。默认进程内实现，可替换为自定义实现（如基于 Redis 的分布式锁）：

```properties
liteflow.agent.invocation-guard.mode=BEAN
liteflow.agent.invocation-guard.bean-name=myAgentInvocationGuard   # AgentInvocationGuard 实现 bean
liteflow.agent.invocation-guard.acquire-timeout=2m                 # 等待锁的超时
```

### 14.5 异常类型

- `AgentConfigException`：配置/构建期错误（缺 namespace、非法参数、开启了未启用的工具等），fail-fast。
- `AgentInvocationException`：调用期错误，携带类型 `TIMEOUT` / `INTERRUPTED` / `ACQUISITION_FAILED` / `PERMISSION` / `STRUCTURED_OUTPUT`。
- 在链上体现为 `response.isSuccess() == false`，异常通过 `response.getCause()` 获取。

## 15. 离线测试自己的 Agent 链

不需要真实大模型也能测试编排链路：覆写 `buildModel()` 返回一个进程内的假 `io.agentscope.core.model.Model` 实现即可，Spring 上下文与 EL 链照常执行：

```java
@Override
protected Model buildModel() {
    return new FakeModel(); // 自己实现的 Model，按脚本返回固定答复/工具调用
}
```

参考实现见测试模块 `liteflow-testcase-el/liteflow-testcase-el-react-agent` 中的 `ScriptedChatModel`（支持按队列编排「文本答复 → 工具调用 → 异常」脚本，并能回放每次模型调用的入参做断言）。该模块的 `feature/*` 目录覆盖了本文档大部分特性的完整可运行示例，`platform/*` 目录是各平台的连通性示例，均可直接参考。

## 16. 配置速查

所有配置前缀为 `liteflow.agent.*`，写在 `application.properties` / `application.yml` 中即可被 `liteflow-spring-boot-starter` 绑定。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `runtime.namespace` | 无（**必填**） | 运行时命名空间，隔离会话状态 |
| `runtime.default-user-id` | `anonymous` | 默认用户 ID |
| `runtime.timeout` | `2m` | 单次 Agent 调用总超时 |
| `defaults.max-iterations` | `50` | 全局最大 ReAct 迭代次数 |
| `state-store.type` | `JSON` | 会话状态存储：`JSON` / `REDIS` / `MYSQL`（均持久化） |
| `state-store.json-root` | `./data/agent-state` | JSON 存储根目录 |
| `state-store.failure-policy` | `FAIL_FAST` | 存储失败策略：`FAIL_FAST` / `LOG_AND_CONTINUE` |
| `state-store.redis.uri` | 无 | Redis 直连 URI（与 `redis.client-bean-name` 二选一，需 redis 模块） |
| `state-store.redis.client-bean-name` | 无 | 已有 Jedis/Lettuce/Redisson 客户端 bean 名 |
| `state-store.redis.key-prefix` | `agentscope:session:` | Redis key 前缀 |
| `state-store.mysql.data-source-bean-name` | 无 | 应用 DataSource bean 名（与 `mysql.jdbc-url` 二选一，需 mysql 模块） |
| `state-store.mysql.jdbc-url` | 无 | JDBC 直连 url |
| `state-store.mysql.username` / `state-store.mysql.password` | 无 | JDBC 直连凭据 |
| `state-store.mysql.database-name` / `state-store.mysql.table-name` | `agentscope` / `agentscope_sessions` | 库表名 |
| `state-store.mysql.create-if-not-exist` | `false` | 是否自动建库建表 |
| `toolkit.parallel` | `false` | 工具是否并行执行 |
| `event.listener-failure-mode` | `FAIL_FAST` | 事件监听器异常策略 |
| `invocation-guard.mode` | `LOCAL` | 并发守卫：`LOCAL` / `BEAN` |
| `invocation-guard.bean-name` | 无 | `mode=BEAN` 时的守卫 bean 名 |
| `invocation-guard.acquire-timeout` | `2m` | 等待调用锁的超时 |
| `invocation-guard.lease-duration` | `2m` | 调用锁租约时长 |
| `hitl.confirmation-timeout` | `2m` | 人工确认等待超时 |
| `hitl.fail-on-denied-tool` | `false` | 工具被拒绝时是否让链失败 |
| `workspace.root` | 无 | 工作区根目录（开文件工具时必填），即内置工具的 baseDir |
| `workspace.trusted-local` | `false` | 确认信任本地文件系统（开文件工具时需 `true`） |
| `workspace.auto-create` | `true` | 自动创建工作区目录（harness 受控文件系统消费） |
| `workspace.max-file-bytes` | `10485760` | 单文件大小上限（10MB，harness 受控文件系统消费） |
| `shell.mode` | `DISABLED` | shell 工具模式：`WHITELIST` / `DISABLED` |
| `shell.whitelist` | 内置清单 | 命令白名单（仅匹配首段命令；留空等价于放行全部，务必保持非空） |
| `shell.timeout` | `30s` | 单条命令超时（harness shell 工具消费） |
| `logging.react-enabled` | `true` | ReAct 推理/行动/错误过程日志 |
| `skills.enabled` | `false` | 是否启用技能 |
| `skills.path` | `./skills` | 技能仓库目录；`classpath:` 前缀表示 classpath 资源，其他值表示文件系统路径 |
| `openai.api-key` / `openai.base-url` | 无 | OpenAI 凭据 |
| `anthropic.api-key` / `anthropic.base-url` | 无 | Anthropic 凭据 |
| `gemini.api-key` / `gemini.base-url` | 无 | Gemini 凭据 |
| `dashscope.api-key` / `dashscope.base-url` | 无 | DashScope 凭据 |
| `openai-compatible.<key>.api-key` / `.base-url` | 无 | OpenAI 兼容平台凭据（含 deepseek/kimi/glm/minimax 预设） |
| `anthropic-compatible.<key>.api-key` / `.base-url` | 无 | Anthropic 兼容网关凭据（均必填） |
| `harness.filesystem-backend` | `GUARDED_LOCAL` | Harness 文件系统后端：`GUARDED_LOCAL` / `DOCKER` / `CUSTOM` |
| `harness.trusted-local` | `false` | `GUARDED_LOCAL` 需设为 `true` |
| `harness.docker.image` | `ubuntu:22.04` | 沙箱镜像 |
| `harness.docker.workspace-root` | `/workspace` | 沙箱内工作目录 |
| `harness.docker.memory-size-bytes` | `536870912` | 沙箱内存上限（512MB） |
| `harness.docker.cpu-count` | `1` | 沙箱 CPU 数 |
| `harness.docker.network` | `none` | 沙箱网络模式 |

## 17. 组件扩展点速查

`ReActAgentComponent` 必须实现的抽象方法：

| 方法 | 说明 |
| --- | --- |
| `model()` | 返回 `ModelSpec`，决定用哪个平台哪个模型 |
| `systemPrompt()` | 系统提示词（追加在内置约定之后） |
| `userPrompt(LiteFlowAgentContext)` | 用户提示词，通常取链请求参数 |

常用可选覆写（均有默认实现）：

| 方法 | 说明 |
| --- | --- |
| `buildModel()` | 逃生舱：直接返回 AgentScope `Model`，绕过 `model()` |
| `maxIterations()` | 最大迭代次数，默认 `-1`（用全局配置） |
| `maxRetries()` / `fallbackModel()` | 模型重试 / 回退模型 |
| `routingModels()` / `routeModel(...)` | 多模型路由 |
| `tools()` | 自定义工具列表（`@Tool` 注解对象） |
| `customizeToolkit(Toolkit)` | 工具装配后处理 |
| `mcpClients()` | 接入 MCP 工具客户端 |
| `middlewares()` | 注册中间件 |
| `enableWorkspaceFileTools()` / `enableShellTool()` | 开启内置文件 / shell 工具 |
| `structuredOutputType()` / `structuredOutputSchema()` | 结构化输出（二者互斥） |
| `handleReply(Msg, LiteFlowAgentContext)` | 自定义最终答复的去向（也是读取 `getChatUsage()` 的时机，见 6.2） |
| `resolveUserId(Slot)` / `resolveConversationId(Slot)` / `agentKey()` | 会话身份解析 |
| `stateStoreResolver()` | 自定义状态存储解析 |
| `permissionContext()` / `confirmationHandler()` / `stopOnReject()` | HITL 权限与确认；Harness 默认 `BYPASS`，仅权限策略产生 `ASK` 时需要确认处理器 |
| `skillRepositoryRegistrations()` / `skillFilter()` | 高级技能仓库注册、ownership 与过滤；普通目录优先使用 `skills.*` 配置 |
| `transformSystemPrompt(...)` | 动态改写系统提示词 |
| `modelExecutionConfig()` / `toolExecutionConfig()` | 模型 / 工具执行配置 |
| `customizeAgent(ReActAgent.Builder)` | 底层定制（不可替换框架托管的模型、工具、存储与中间件） |

## 18. 常见报错与排查

绝大多数配置问题都是构建期 fail-fast 的 `AgentConfigException`，报错信息可直接对照下表定位（按报错原文搜索即可）：

| 报错信息（片段） | 原因 | 处理 |
| --- | --- | --- |
| `liteflow.agent.runtime.namespace is required before execution` | 未配置运行时命名空间 | 补上 `liteflow.agent.runtime.namespace`，见 [§2.2](#22-最小配置) |
| `state-store type REDIS requires the liteflow-react-agent-redis module on the classpath`（MYSQL 同理） | 选了 REDIS/MYSQL 后端但没引入对应模块 | 引入 `liteflow-react-agent-redis` / `liteflow-react-agent-mysql`，见 [§5.3](#53-statestore会话状态存在哪) |
| `state-store type REDIS requires either state-store.redis.uri or state-store.redis.client-bean-name` | 连接源缺失 | 配置 uri 或 client-bean-name 其一（两者都配会报 mutually exclusive） |
| `Redis state store could not be created` | Redis 不可达（构建期即建连，fail-fast） | 检查地址/网络；确认客户端 bean 类型受支持 |
| `MySQL state store could not be created` | 连不上库，或库表不存在且未开自动建表 | 检查 JDBC 连接；或设 `mysql.create-if-not-exist=true`，或预先建库建表 |
| `enableShellTool requires liteflow.agent.shell.mode != DISABLED` | 开了 shell 工具但 shell 模式未启用 | 设 `liteflow.agent.shell.mode=WHITELIST`，见 [§4.3](#43-内置工具agentscope-文件工具与-shell-工具) |
| `enableShellTool requires a non-empty / non-blank liteflow.agent.shell.whitelist` | 白名单为空或含空白条目（留空等价于放行全部，框架拒绝） | 提供非空且无空白项的白名单 |
| `built-in workspace tools require workspace.trustedLocal=true`（或 `...a valid workspace.root`、`...GUARDED_LOCAL`） | 文件工具前置条件不满足 | 配置 `workspace.root` 并显式 `trusted-local=true`，见 [§4.3](#43-内置工具agentscope-文件工具与-shell-工具) |
| `structuredOutputType and structuredOutputSchema are mutually exclusive` | 两种结构化输出覆写同时存在 | 只保留其一，见 [§7](#7-结构化输出) |
| `Harness workspace.root must not be blank` / `Harness shell.timeout must be positive` | Harness 文件系统配置缺失/非法 | 补 `workspace.root`、检查 `shell.timeout`，见 [§12](#12-harness-模块上下文工程与沙箱) |
| `No AgentConfirmationHandler is configured` | 显式配置了 `ASK`，但没有提供确认处理器 | 实现 `confirmationHandler()`；若使用 Harness 默认 `BYPASS`，则无需配置 `ASK`，见 [§10](#10-人工确认hitl) 与 [§12.1](#121-默认权限策略) |
| `A2A client requires exactly one UserMessage` / `A2A client supports TEXT output only` | A2A 客户端输入/输出形态不符合协议约束 | 输入改为单条用户消息；结构化需求在远端完成后以文本返回，见 [§13](#13-a2a调用远程-agent) |

调用期的 `AgentInvocationException`（TIMEOUT / INTERRUPTED / ACQUISITION_FAILED / PERMISSION / STRUCTURED_OUTPUT）见 [§14.5](#145-异常类型)。
