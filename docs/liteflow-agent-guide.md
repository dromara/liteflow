# LiteFlow Agent 使用指南

本文档面向 LiteFlow Agent 的使用者，介绍如何接入模型、添加工具、保存会话，以及编排多个 Agent。建议第一次使用时先完成第 2 章，再按需要阅读后续章节。

文中的 Java 示例省略了 `import`。标为“组件内覆写”的代码需要放进对应的 Agent 组件类中；其余示例均按当前代码库的公开 API 编写。含 `...`、业务自定义类型或占位变量的高级示例是示意片段，需要替换后使用。

## 1. 简介

`liteflow-agent` 是 LiteFlow 的 Agent 扩展模块，基于 AgentScope Java（2.0.2）构建。它把一个大模型 Agent 封装成一个普通的 LiteFlow 组件，从而可以：

- 用 LiteFlow EL（`THEN` / `WHEN` / `IF` / `SWITCH` 等）自由编排一个或多个 Agent，以及普通业务组件。
- 通过统一的 `ModelSpec` 描述符接入各家大模型平台，凭据走配置，参数走代码。
- 获得开箱即用的多轮对话记忆、工具调用、流式事件、结构化输出、人工确认（HITL）等能力。

**运行要求**：JDK 17+。本文对应 LiteFlow `2.16.2` 和 AgentScope `2.0.2`。

**模块清单**（Maven groupId 均为 `com.yomahub`）：

| 模块 | 作用 |
| --- | --- |
| `liteflow-agent-core` | Agent 组件基类、模型抽象、工具、会话状态、事件、HITL 等核心能力 |
| `liteflow-agent-openai` | OpenAI 及 OpenAI 兼容平台（DeepSeek、Kimi、GLM、MiniMax、任意兼容端点） |
| `liteflow-agent-anthropic` | Anthropic 及 Anthropic 兼容网关 |
| `liteflow-agent-dashscope` | 阿里云百炼（DashScope / 通义千问） |
| `liteflow-agent-gemini` | Google Gemini |
| `liteflow-agent-harness` | 可选增强：上下文压缩、记忆、技能、子代理、计划模式、沙箱文件系统 |
| `liteflow-agent-a2a` | 可选：A2A（Agent-to-Agent）协议客户端 |
| `liteflow-agent-redis` | 可选：Redis 会话状态存储（基于 agentscope-extensions-redis） |
| `liteflow-agent-mysql` | 可选：MySQL 会话状态存储（基于 agentscope-extensions-mysql） |

使用时引入至少一个平台模块即可，平台模块会自动带入 `liteflow-agent-core`。

### 1.1 先认清几个概念

| 概念 | 可以简单理解为 |
| --- | --- |
| Agent 组件 | 一个能调用大模型和工具的 LiteFlow 节点，例如 `chatAgent` |
| Chain | 用 EL 编排出的流程，可以混合 Agent 组件和普通业务组件 |
| `conversationId` | 一段多轮对话的 ID；后续请求复用它即可继续上次对话 |
| `agentKey` | Agent 的稳定身份，默认是节点 ID；用于隔离不同 Agent 的记忆 |
| Toolkit | 当前 Agent 可以调用的工具集合，包括 Java、内置和 MCP 工具 |
| StateStore | 保存对话历史和 Agent 状态的存储，可选 JSON、Redis 或 MySQL |
| Harness | 在普通 Agent 之上增加记忆压缩、子代理、计划和沙箱等能力 |
| MCP / A2A | MCP 把远端能力接成“工具”；A2A 把远端 Agent 接成“节点” |

### 1.2 按目标阅读

| 你想做什么 | 直接阅读 |
| --- | --- |
| 先跑通一个 Agent | [第 2 章：快速开始](#2-快速开始) |
| 更换模型或调整模型参数 | [第 3 章：接入模型平台](#3-接入模型平台) |
| 添加 Java、文件、Shell 或 MCP 工具 | [第 4 章：为 Agent 添加工具](#4-为-agent-添加工具) |
| 实现多轮对话和持久化 | [第 5 章：多轮对话与状态持久化](#5-多轮对话与状态持久化) |
| 把增量内容推送给前端 | [第 6 章：流式输出与事件监听](#6-流式输出与事件监听) |
| 串行、并行或按条件编排多个 Agent | [第 9 章：多 Agent 编排](#9-多-agent-编排) |
| 给高风险工具加人工审批 | [第 10 章：人工确认](#10-人工确认hitl) |
| 使用 Skills、子代理、计划模式或沙箱 | [第 11 章](#11-skills技能)和[第 12 章](#12-harness-模块上下文工程与沙箱) |
| 调用远程 A2A Agent | [第 13 章：A2A](#13-a2a调用远程-agent) |
| 上生产前检查超时、并发和资源释放 | [第 14 章：可靠性与错误处理](#14-可靠性与错误处理) |

### 1.3 完整目录

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

下面用 Spring Boot 接入 DeepSeek，共 5 步。完成后，请求 `chatChain` 就能得到模型的文本答复。

### 2.1 引入依赖

```xml
<properties>
    <liteflow.version>2.16.2</liteflow.version>
</properties>

<!-- LiteFlow Spring Boot 集成（agent 模块本身不含自动装配，需要它） -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-spring-boot-starter</artifactId>
    <version>${liteflow.version}</version>
</dependency>

<!-- 平台模块：按需选择一个或多个 -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-openai</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

通常不需要自行引入 AgentScope 依赖。LiteFlow Agent 会传递引入所需工件，代码中可直接使用相关类型。

确需自行添加 AgentScope 工件时，不要引入 `io.agentscope:agentscope` 聚合包，并确保版本与 LiteFlow 传递引入的版本一致，避免类路径冲突。

Spring Boot 4 使用 `liteflow-spring-boot4-starter`；Solon 使用 `liteflow-solon-plugin`。Agent 的使用方式相同。LiteFlow 规则也可以写成 XML、JSON 或 YML；本文只用 XML 展示，其他格式及完整 EL 语法请参考 [LiteFlow 官方文档](https://liteflow.cc/)。

### 2.2 最小配置

```properties
# 对应 src/main/resources/agent/flow.el.xml
liteflow.rule-source=agent/flow.el.xml

# 必填：agent 运行时命名空间，用于隔离会话状态
liteflow.agent.runtime.namespace=my-service

# 平台凭据（建议从环境变量注入）
liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
```

`runtime.namespace` 用来隔离不同应用的会话状态，必须填写且应保持稳定。缺失时，首次执行就会失败。

### 2.3 编写 Agent 组件

继承 `AgentComponent`，实现模型、系统提示词和用户提示词三个方法：

```java
@Component("chatAgent")
public class ChatAgentCmp extends AgentComponent {

    @Override
    protected ModelSpec<?> model() {
        return DeepSeek.of("deepseek-chat").temperature(0.1);
    }

    @Override
    protected String systemPrompt() {
        return "你是一个简洁的助手，用中文回答。";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}
```

### 2.4 在 EL 中编排

在 `src/main/resources/agent/flow.el.xml` 中按 Spring Bean 名引用组件：

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
    if (!response.isSuccess()) {
        throw new IllegalStateException("Agent 执行失败", response.getCause());
    }
    // 默认输出位置
    System.out.println(response.getSlot().getResponseData());
}
```

执行成功时，控制台会打印模型答复；失败时可从 `response.getCause()` 查看原因。至此最小链路已经跑通。

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

每个凭据配置段支持 `api-key` 和 `base-url`。配置对象还保留了 `extra.*` 字段，但当前内置 Provider 不读取它；平台特有参数请使用下文的 Spec API。

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

不同平台的特有入口如下。只有确实需要时再使用，普通调用只设置模型名和通用参数即可。

| 平台 | 特有入口 |
| --- | --- |
| OpenAI | `reasoningEffort`、`frequencyPenalty`、`presencePenalty`、`endpointPath`、`formatter`、原生结构化输出、代理、HTTP Transport、`customizeBuilder` |
| DeepSeek / Kimi / GLM / MiniMax 等兼容平台 | `endpointPath`、`enableThinking`、代理、HTTP Transport、`customizeContext` |
| Anthropic | `thinking`、`formatter`、`customizeBuilder` |
| DashScope | `thinking`、`formatter`、原生结构化输出、代理、HTTP Transport、`customizeBuilder` |
| Gemini | `thinking`、`formatter`、`customizeBuilder` |

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

使用 `.ownedHttpTransport(...)` 时，HTTP Transport 随 Agent Runtime 关闭；使用 `.borrowedHttpTransport(...)` 时，由调用方负责关闭。`customizeBuilder(...)` 和 `customizeContext(...)` 是平台 SDK 的最终定制入口。

### 3.4 逃生舱：buildModel()

如果 `ModelSpec` 不能满足需求，例如要接入自定义 SDK 或测试模型，可以覆写 `buildModel()`，直接返回 AgentScope 的 `Model` 实例。注意：`model()` 仍是抽象方法，必须保留一个占位实现，否则代码无法编译。

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
public class OrderAgentCmp extends AgentComponent {

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

工具方法参数和返回值没有特殊限制，框架会把方法签名转换成模型可理解的 Schema。

### 4.2 工具中需要依赖注入

把工具类声明为 Spring Bean，注入组件后在 `tools()` 中返回：

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
public class OrderAgentCmp extends AgentComponent {
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
# 文件工具与 Shell 工具共用以下工作区：root 是内置工具的 baseDir，
# 所有路径都被限制在该目录内；即使只开 Shell，也必须配置这两项。
# 启用时目录会自动创建；root 在同一组件的所有会话间共享，
# 需要按会话隔离工作区时请使用 harness 模块的 workspace / sandbox 体系。
liteflow.agent.workspace.root=/data/agent-workspace
liteflow.agent.workspace.trusted-local=true

# Shell 工具：仅白名单模式；默认关闭，开启时必须使用 WHITELIST
liteflow.agent.shell.mode=WHITELIST
# 按业务缩小白名单；这里只示例只读命令
liteflow.agent.shell.whitelist=ls,cat,grep,wc
```

命令过滤由 AgentScope 内置 `ShellCommandTool` 的 `UnixCommandValidator` 执行：只放行白名单中的首段命令，并拒绝包含 `&`、`|`、`;` 或换行的链式命令；单条命令的超时由模型按调用传入（默认 300 秒）。不满足约束时（例如未配置白名单就开启了 Shell 工具），构建期会抛出 `AgentConfigException`。

### 4.4 接入 MCP 工具

下面以 Streamable HTTP MCP 服务为例。客户端交给 Spring 管理，Agent 只借用它：

```java
@Configuration
public class McpConfig {

    @Bean(destroyMethod = "close")
    public McpClientWrapper weatherMcpClient() {
        return McpClientBuilder.create("weather")
                .streamableHttpTransport("https://mcp.example.com/mcp")
                .header("Authorization", "Bearer " + System.getenv("MCP_TOKEN"))
                .buildSync();
    }
}

@Component("weatherAgent")
public class WeatherAgentCmp extends AgentComponent {

    @Resource
    private McpClientWrapper weatherMcpClient;

    @Override
    protected List<McpClientWrapper> mcpClients() {
        return List.of(weatherMcpClient);
    }

    // model() / systemPrompt() / userPrompt() 略
}
```

首次构建 Agent Runtime 时，LiteFlow 会初始化 MCP 客户端、发现工具并注册到 Toolkit。`McpClientBuilder` 还支持 `stdioTransport(...)` 和 `sseTransport(...)`。

客户端的关闭责任必须明确：

- 默认 `ownsMcpClient(...) == false`，表示客户端由 Spring 或调用方关闭，适合共享 bean。
- 如果客户端只属于当前组件，可让 `ownsMcpClient(client)` 返回 `true`；LiteFlow 会在 Runtime 关闭或构建失败时关闭它，不能再由其他组件共享。

覆写 `customizeToolkit(Toolkit)` 可以调整已装配的 Java 工具和内置工具；LiteFlow 会在这个回调之后注册 MCP 工具，因此回调中看不到 MCP 工具。多个工具默认串行执行，配置 `liteflow.agent.toolkit.parallel=true` 可并行。

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

四种方式都不用时，Agent 会在首次执行时生成 `conversationId` 并写回 Slot，可通过 `LiteflowResponse#getConversationId()` 取得。后续调用必须复用这个 ID 才能延续对话；否则会生成新 ID，开启新会话。

同一 `conversationId` 的多次调用会自动带上历史消息，无需额外代码。同一身份的并发调用会被串行化（见 14.4 并发守卫）。

### 5.3 其他执行选项与异步调用

`ExecuteOption` 可以同时传入链路 ID、会话 ID、上下文和事件监听器：

```java
ExecuteOption option = ExecuteOption.of()
        .requestId("req-20260831-001")
        .conversationId("user-1024-task-abc")
        .contextClass(OrderContext.class)
        .eventListener(this::onAgentEvent);

LiteflowResponse response = flowExecutor.execute2Resp("chatChain", request, option);
```

- `requestId` 用于一次执行的链路追踪；不传时由框架生成。它不是会话 ID，也不要把它拼入 `agentKey`。
- `contextClass(...)` 传入上下文类型，由 LiteFlow 创建实例；已有实例时使用 `contextBean(...)`。组件中照常通过 `getContextBean(...)` 获取。
- `conversationId` 决定是否延续 Agent 记忆，`eventListener` 接收第 6 章介绍的流式事件。

不希望阻塞当前线程时，使用同一组选项调用异步 API：

```java
Future<LiteflowResponse> future =
        flowExecutor.execute2Future("chatChain", request, option);
LiteflowResponse response = future.get();
```

`execute2Future(...)` 只改变调用方式，不改变会话、上下文、事件或错误处理语义。

### 5.4 StateStore：会话状态存在哪

内置后端都是持久化存储。会话记忆需要跨重启续接，因此没有内置的内存后端。

```properties
# JSON（默认，本地文件） / REDIS / MYSQL
liteflow.agent.state-store.type=JSON

# type=JSON 时的存储根目录，默认 ./data/agent-state
liteflow.agent.state-store.json-root=/data/agent-state

# 会话状态加载失败策略：FAIL_FAST（默认） / LOG_AND_CONTINUE
liteflow.agent.state-store.failure-policy=FAIL_FAST
```

`failure-policy` 目前只处理会话状态的加载失败：`LOG_AND_CONTINUE` 会记录错误并在没有历史状态的情况下继续。Store 构建失败和状态保存失败仍会直接向外抛出。

#### Redis：引入 liteflow-agent-redis 模块

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-redis</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

```properties
liteflow.agent.state-store.type=REDIS

# 方式一：直连 URI（框架创建并持有 Lettuce 客户端）
liteflow.agent.state-store.redis.uri=redis://localhost:6379

# 方式二：复用容器中已有的客户端 bean（Jedis UnifiedJedis / Lettuce RedisClient
# 或 RedisClusterClient / Redisson RedissonClient /
# io.agentscope.extensions.redis.state.RedisClientAdapter），二选一
#liteflow.agent.state-store.redis.client-bean-name=redissonClient

# 可选：Redis 内的 key 前缀，默认沿用 agentscope 扩展的 agentscope:session:
#liteflow.agent.state-store.redis.key-prefix=liteflow:agent:state:
```

#### MySQL：引入 liteflow-agent-mysql 模块

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-mysql</artifactId>
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

通过 `redis.client-bean-name` 或 `mysql.data-source-bean-name` 传入的客户端属于应用，LiteFlow 不会关闭；使用 `redis.uri` 或 `mysql.jdbc-url` 时，连接资源由 LiteFlow 创建并随 Runtime 关闭。

#### 自定义存储

需要其他后端（如 MongoDB）时，覆写组件的 `stateStoreResolver()` 返回自定义 `AgentStateStore` 装配：

```java
@Override
protected AgentStateStoreResolver stateStoreResolver() {
    return config -> new ResolvedAgentStateStore(new MongoAgentStateStore(...), true);
}
```

`ResolvedAgentStateStore` 的第二个参数表示资源所有权：`true` 表示由 Runtime 关闭，适合当前组件独占的 Store；`false` 表示仅借用，关闭责任在调用方，适合共享 Store。

#### 存储内容与清理

AgentScope 2.0.2 的主要持久化入口是 `agent_state`，其中 `context` 保存当前模型上下文，另外还包含摘要、权限、任务、计划和工具状态。`memory_messages` / `toolkit_activeGroups` 是旧版兼容读取键，不应作为新会话消息查询的主要入口。

Core Agent 的物理会话 ID 为 `lf-<agent 哈希>.lf-<会话哈希>`；Harness 使用带 `.h1.` 版本标记的独立编码，并另外保存沙箱恢复状态。读取时使用下节的 `AgentConversationService.agentState(...)`，无需自己拼接目录、Redis key 或 SQL 的 `session_id`。

启用会话历史后，框架还会在同一个 StateStore 后端中保存独立的会话元数据和逐条消息。它们不会被模型上下文压缩修改。JSON、MySQL、Redis 使用相同的服务 API。

框架不自动按时间过期。删除托管会话请使用 `AgentConversationService.delete(...)`，不要直接删除底层文件或键。工作区文件、沙箱快照、子代理的独立会话及长期记忆仍按应用的保留策略管理，不随此操作删除。

### 5.5 Agent Runtime 生命周期

Agent Runtime 在组件第一次执行时创建，之后由该组件复用。模型、工具、MCP 客户端、中间件和技能仓库等构建期配置也随 Runtime 复用，因此不要根据每次请求动态改变这些配置。不同身份的调用可以并发执行，所以组件字段、自定义工具和中间件不要保存请求级可变态；确需共享可变状态时，必须保证线程安全。

`agentKey` 代表组件的稳定身份。组件初始化后再返回另一个 `agentKey` 会直接报错；按请求变化的内容应放在 `userPrompt(...)`、`transformSystemPrompt(...)`、`customizeRuntimeContext(...)` 或业务上下文中。

Spring 和 Solon 管理的组件会随 bean 生命周期调用 `close()`。手工创建组件、手工注册单例或使用自定义容器时，应确认容器会执行 `close()`；否则要在应用退出前显式调用。关闭会等待正在执行的调用结束，并释放框架拥有的模型、MCP 客户端、技能仓库等资源。`buildModel()`、`fallbackModel()` 和 `routingModels()` 返回的模型都视为 Runtime 独占，会随 Runtime 关闭；不要把同一个可关闭模型实例交给多个组件共享。

### 5.6 会话列表、历史消息与当前上下文查询

引入 `liteflow-agent-core` 后即可使用 `AgentConversationService`。查询只创建状态存储连接，不需要模型 API Key，也不会启动 Agent 或 Docker 沙箱。

Spring / Spring Boot 通过显式导入注册 Bean，由容器负责关闭服务：

```java
@Configuration
@Import(AgentConversationConfiguration.class)
public class ConversationConfiguration {
}
```

然后注入 `AgentConversationService`。纯 Java 或 Solon 应用也可以自行管理服务生命周期：

```java
try (AgentConversationService conversations =
        AgentConversationService.open(liteflowConfig.getAgent())) {
    var conversation = conversations.create("alice", "订单咨询");
    var page = conversations.list("alice", 0, 20);
    var messages = conversations.messages("alice", conversation.id(), 0, 50);
}
```

`open(...)` 复用 `liteflow.agent.state-store.*` 配置并管理新建 Store 的所有权。使用自定义 Store 时调用 `new AgentConversationService(agentConfig, sharedStore)`；此构造方式不会关闭借用的 Store。查询服务应与 Agent 使用相同的 namespace、userId、后端和并发守卫。

要让 `AgentComponent` / `HarnessAgentComponent` 自动登记业务会话并记录每次调用的输入、最终答复或失败信息，开启：

```properties
liteflow.agent.conversation-history-enabled=true
```

该选项默认关闭，以保留现有应用的存储行为。开启后，普通 `execute2Resp(...)` 调用也会自动创建历史，不要求先调用 `create(...)`。每条自动记录带有 `agentKey` 和 `requestId`，用于区分同一业务会话中的不同 Agent 和执行。A2A 及非 Agent 链路可以使用 `append(...)` 显式记录。

服务使用业务会话 ID，不需要调用方计算哈希：

```java
var session = conversations.create("alice", "会话标题");
String conversationId = session.id();
// Agent 的 resolveUserId(...) 必须也返回 alice。
flowExecutor.execute2Resp("chatChain", "你好",
        ExecuteOption.of().conversationId(conversationId));

var metadata = conversations.get("alice", conversationId); // Optional<AgentConversation>
var firstPage = conversations.messages("alice", conversationId, 0, 50);
if (firstPage.hasMore()) {
    var nextPage = conversations.messages("alice", conversationId, firstPage.nextCursor(), 50);
}
conversations.update("alice", conversationId, "新标题", Map.of("category", "订单"));
conversations.delete("alice", conversationId);
```

- `list(userId, cursor, limit)`：按更新时间降序读取元数据。cursor 是列表偏移，初始为 0。底层 Store 没有索引分页接口，因此当前实现扫描该用户的会话元数据，但不加载历史消息；并发新增或更新时间变化时应刷新列表。
- `messages(userId, conversationId, cursor, limit)`：按顺序分页读取消息。cursor 是上一页最后一条消息的 sequence，初始为 0；每次只读取当前页的消息。
- 两种分页的 limit 都为 1～200。`get(...)` 对不存在或已删除的会话返回空；对不存在会话追加消息或读取消息页会抛出 `IllegalArgumentException`。
- `update(...)` 的 title 为 null 时保留标题；attributes 替换已有属性 Map。

**当前模型上下文与聊天记录是两个不同的读取入口。** 已有旧会话尚未开启历史记录时，可以直接读取持久化状态：

```java
Optional<AgentState> state = conversations.agentState("alice", conversationId, "chatAgent");
List<Msg> context = state.map(AgentState::getContext).orElseGet(List::of);
```

该方法通过 Core / Harness 的寻址规则读取快照，返回的数据不会修改运行中的状态。若同一个 agentKey 同时存在 Core 与 Harness 两份状态，会报告歧义，避免任意选择。它返回的是最近持久化的工作上下文，可能已经被摘要替代；不能保证恢复压缩前的历史。旧哈希会话没有原始 conversationId 的索引，不能自动反查并生成左侧会话列表。

**Web 或多 Agent 编排的对外消息。** 如果用户看到的输入、输出与某个 Agent 的 prompt、reply 不同，应由应用明确记录对外消息，避免把中间 Agent 输出当成最终答复：

```java
// false 关闭这个会话的自动消息追加；启用历史配置后仍自动登记参与的 Agent。
conversations.create("alice", conversationId, "会话标题", false);
conversations.append("alice", conversationId, "user", "input", userText);
// 执行 LiteFlow 链，取得对外答复；可选记录工具轨迹或其他展示信息。
conversations.append("alice", conversationId, "assistant", "result", finalText);
```

`append(...)` 还支持传入 agentKey 和 requestId。它保存独立的展示内容，不向模型上下文注入消息。自动记录默认只包含输入、结果和错误；工具轨迹、UI 状态等可以通过自定义 stage 显式追加。通用属性保存在 `AgentConversation.attributes`，框架不规定前端样式。

删除会先持久化删除标记，然后等待已登记 Agent 的状态锁，清理会话消息与关联的 Agent 状态。迟到的结果不会恢复已删除的数据；删除失败可以重试，删除后的 ID 不能复用。会保留一个不含消息正文的小型删除标记。旧会话或自定义执行器的状态可先调用 `attachAgent(userId, conversationId, agentKey)` 关联；调用 `agentState(...)` 本身不会建立这种关联。

默认守卫只在单进程内生效。多实例运行时，查询服务与执行组件需要配置同一个分布式 `AgentInvocationGuard`，并实现新增的 `CONVERSATION` scope；该 scope 的 key 不包含 agentKey，只保护短暂的会话元数据操作。服务不会持有会话锁等待 Agent 执行结束。

升级已有 UI 存储时可以使用 `importIfAbsent(...)` 导入完整消息列表，保留原消息 ID 和时间。它在消息写入后才发布元数据，允许失败重试，且不会重新导入已经删除的 ID。`liteflow-agent-web-example` 已使用此接口迁移原来的 `chat_session` 记录，并由框架服务完成后续存取。

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
                case "agent.tool.result.delta": // 工具返回内容增量
                    if (event.getText() != null) {
                        System.out.print(event.getText());
                    }
                    break;
                case "agent.result":         // 最终结果（event.isLast() == true）
                    System.out.println("\n[done] " + event.getText());
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
| `agent.reasoning` | 面向兼容消费端的推理文本事件；当前文本增量也会映射到此事件 |
| `agent.tool_result` | 面向兼容消费端的工具结果事件 |
| `agent.summary` | Harness 摘要或提示信息 |
| `agent.result` | 最终答复（`last=true`） |
| `agent.error` | 执行错误 |

`agent.tool.call.end` 表示工具参数已经接收完毕，其 `text` 固定为空；工具正文应读取 `agent.tool.result.delta`。事件对象 `FlowEvent` 上还有 `chainId`、`nodeId`、`requestId`、`conversationId`、`timestamp` 等字段。`data` 的实际类型是 `AgentFlowEventData`，包含原始 AgentScope 事件及 `userId`、`agentKey`、`traceId`、`taskId`、`replyId` 等关联信息。

监听器抛异常时的行为由 `liteflow.agent.event.listener-failure-mode` 控制：`FAIL_FAST`（默认，中断执行）或 `LOG_AND_CONTINUE`。

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

- `ChatUsage` 提供输入、输出、总 token、缓存命中 token 和耗时：`getInputTokens()`、`getOutputTokens()`、`getTotalTokens()`、`getCachedTokens()`、`getTime()`。
- 一次 Agent 调用里发生多轮 Agent 迭代（多次模型请求）时，用量是累计值。
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

除 `onAgent` 外，`MiddlewareBase` 还有 `onReasoning`、`onActing`、`onModelCall`、`onSystemPrompt` 等切点，按需覆写。框架内置的日志、状态持久化、事件桥接等中间件会自动安装。

LiteFlow 会把所有用户中间件放在统一的 `order=1000` 层级，原对象的 `order()` 不参与最终排序；多个用户中间件的先后由 `middlewares()` 返回列表的顺序决定。

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

普通 Agent 默认以节点 ID 作为 `agentKey`，因此 `agentA` 和 `agentB` 身份不同，可以并行执行。不要把 `requestId` 拼进 `agentKey`：Runtime 会复用，组件初始化后改变身份会导致后续请求失败。

Harness Agent 还会对同一会话的 workspace 加锁。即使节点不同，同一 `conversationId` 下也会串行访问 workspace；这是为了避免并发写坏文件。需要真正并行的 Harness 任务时，应让任务使用互相隔离的会话和 workspace，而不是改变 `agentKey`。

### 9.4 下游节点获取 Agent 结果

- Agent 的最终答复默认只写入 `slot.getResponseData()`。
- 默认不会写入 `slot.output[nodeId]`。流水线中需要按节点读取结果时，上游 Agent 必须覆写 `handleReply(...)` 并显式调用 `setOutput(...)`。

例如，让 `searchAgent` 把文本结果交给 `summaryAgent`：

```java
// searchAgent 中
@Override
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    String text = reply == null ? null : reply.getTextContent();
    getSlot().setOutput(getNodeId(), text);
    // 不调用 super，避免把中间结果写入最终 responseData
}

// summaryAgent 中
@Override
protected String userPrompt(LiteFlowAgentContext context) {
    Object searchResult = getSlot().getOutput("searchAgent");
    return "请总结以下检索结果：\n" + String.valueOf(searchResult);
}
```

最终节点仍使用默认 `handleReply(...)`，链执行完成后从 `response.getSlot().getResponseData()` 取得最终结果。也可以在 `handleReply(...)` 中写入自己的上下文对象。

### 9.5 一次执行里的多个 Agent 如何共享会话

同一次执行中的多个 Agent 共享 `conversationId`，但 `agentKey` 各自独立、记忆互不干扰。文件工具的 workspace root 是组件级共享目录（见 4.3 节），需要按会话或按 Agent 隔离文件时请使用 harness 模块。

### 9.6 其他 EL 能力

Agent 是普通 LiteFlow 节点，因此也能用于 `SWITCH`、`FOR`、`WHILE`、`ITERATOR`、`CATCH`、链级 `RETRY`、`TIMEOUT`、`PRE`、`FINALLY`、`AND`、`OR`、`NOT` 以及节点标签和数据等语法。完整写法请参考 [LiteFlow EL 规则官方文档](https://liteflow.cc/)。

注意区分两类重试：组件的 `maxRetries()` 只重试模型调用；EL 的 `RETRY(...)` 会重新执行整个节点或条件，可能再次调用工具并产生业务副作用。

## 10. 人工确认（HITL）

对于下单、删除、发消息等高风险工具，可以要求模型在调用前等待人工确认。本节适用于 `AgentComponent`，也适用于显式启用 HITL 的 `HarnessAgentComponent`。Harness 未提供权限策略时默认使用 `PermissionMode.BYPASS`，不会触发确认；详见 [§12.1](#121-默认权限策略)。

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

处理器必须为每个待确认工具恰好返回一条结果，并保留原工具调用的 ID 和名称；可以在返回前修改工具输入参数。缺项、重复项或身份不一致都会被拒绝。

确认处理器有两种注册方式：

- 只给某个组件使用：覆写上面的 `confirmationHandler()`。
- 全应用共用：把一个 `AgentConfirmationHandler` 实现注册为 Spring 或 Solon Bean，无需每个组件覆写。容器里必须恰好只有一个该类型的 Bean；显式覆写的处理器优先。

相关配置：

```properties
# 等待人工确认的超时时间，默认 2m
liteflow.agent.hitl.confirmation-timeout=5m
# 工具被拒绝时是否让链失败，默认 false（拒绝结果会回传给模型继续推理）
liteflow.agent.hitl.fail-on-denied-tool=true
```

`stopOnReject()` 与这个配置不是同一个开关：前者决定权限拒绝后是否停止 Agent 的推理循环，关闭时模型可以收到拒绝结果并继续推理；`hitl.fail-on-denied-tool=true` 则会在人工拒绝后立即让当前组件以 `PERMISSION` 错误失败。

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

`dynamicSkillsEnabled()` 默认返回 `true`，用于自动安装动态技能中间件。返回 `false` 会关闭从 Repository 动态加载技能的默认路径；只有自行接管技能装配时才应关闭。配置项 `skills.strict` 当前仅为兼容保留，不改变解析或错误策略。

## 12. Harness 模块：上下文工程与沙箱

`liteflow-agent-harness` 在 Agent 之上提供更多 AgentScope Harness 能力：上下文压缩、记忆、子代理、任务与计划、工具结果淘汰、沙箱文件系统等。用法与 `AgentComponent` 一致，改为继承 `HarnessAgentComponent`：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-harness</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

```java
@Component("harnessAgent")
public class MyHarnessAgentCmp extends HarnessAgentComponent {
    // model() / systemPrompt() / userPrompt() 与 AgentComponent 完全相同，
    // 工具、中间件、HITL API 等能力也一致；默认权限策略与 Harness 特有覆写点见下文。
}
```

### 12.1 默认权限策略

`HarnessAgentComponent` 未覆写 `permissionContext()`，或返回空的权限上下文时，LiteFlow 默认使用 `PermissionMode.BYPASS`。模型调用 Harness 工具时会直接执行，不触发人工确认，因此使用方不需要添加任何权限相关代码，也不需要实现 `confirmationHandler()`。

`BYPASS` 只跳过权限确认，不会关闭 Harness 的文件系统或 Docker 沙箱边界。路径限制、workspace 隔离、Docker 网络、CPU 与内存限制仍按配置生效。

如果状态存储中已有旧会话，且组件仍使用默认权限策略，LiteFlow 会在调用开始前把该会话中持久化的旧权限上下文同步为 `BYPASS`，避免升级后继续触发 `ASK`。

需要人工确认时，显式覆写 `permissionContext()` 并添加 `ASK` 规则，再按 [§10](#10-人工确认hitl) 实现 `confirmationHandler()`。显式提供的 `ALLOW`、`ASK`、`DENY` 等非空权限策略优先于 Harness 默认值，不会被改写成 `BYPASS`。

### 12.2 文件系统后端

Harness 无论选择哪种文件系统后端，都必须配置宿主机上的 `workspace.root`：

```properties
# 三种后端都必填
liteflow.agent.workspace.root=/data/agent-workspace

# 方案一：GUARDED_LOCAL（默认）
liteflow.agent.harness.filesystem-backend=GUARDED_LOCAL
liteflow.agent.harness.trusted-local=true

# 方案二：DOCKER；使用时替换上面的 filesystem-backend
# liteflow.agent.harness.filesystem-backend=DOCKER
# liteflow.agent.harness.docker.image=ubuntu:22.04
# liteflow.agent.harness.docker.workspace-root=/workspace
# liteflow.agent.harness.docker.memory-size-bytes=536870912
# liteflow.agent.harness.docker.cpu-count=1
# liteflow.agent.harness.docker.network=none
# liteflow.agent.harness.docker.snapshot-root=/data/agent-snapshots
# liteflow.agent.harness.docker.workspace-projection-enabled=true
# liteflow.agent.harness.docker.workspace-projection-roots=AGENTS.md,skills,subagents,knowledge,.skills-cache
```

三种后端的边界不同：

| 后端 | 适用场景 | 必要条件与限制 |
| --- | --- | --- |
| `GUARDED_LOCAL` | 可信机器上的受控本地读写 | 必须设置 `harness.trusted-local=true`；默认关闭子代理；不提供宿主机 Shell |
| `DOCKER` | 需要进程、文件和资源隔离 | 本机 Docker 可用；支持子代理；默认无网络、512 MB 内存、1 个 CPU |
| `CUSTOM` | 接入远程文件系统或自有沙箱 | 必须覆写 `filesystemConfigurer()`；安全边界和资源生命周期由实现方负责；支持子代理 |

Docker 的 `snapshot-root` 用于把 sandbox 快照持久化到本地；也可以覆写 `sandboxSnapshotProvider()` 接入远程快照，但二者不能同时使用。工作区投影默认开启，只允许把 `workspace-projection-roots` 中列出的相对路径投影到 sandbox，路径逃逸和符号链接会被拒绝。

选择 `CUSTOM` 时，至少要提供一个 configurer：

```java
@Override
protected HarnessFilesystemConfigurer filesystemConfigurer() {
    return new MyFilesystemConfigurer();
}

final class MyFilesystemConfigurer implements HarnessFilesystemConfigurer {
    @Override
    public void configure(HarnessAgent.Builder builder,
                          HarnessFilesystemContext context) {
        // 使用 context.workspaceRoot()、maxFileBytes()、commandTimeout()
        // 在 builder 上安装自己的 filesystem、message bus 等能力。
    }
}
```

上面的 `CUSTOM` 代码展示扩展契约，具体安装方式取决于所接入的文件系统。不要通过 `customizeHarness(...)` 绕过 `filesystemConfigurer()` 替换文件系统。

### 12.3 上下文压缩（Compaction）

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

### 12.4 长期记忆（Memory）

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

### 12.5 工具结果淘汰（Tool Result Eviction）

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

### 12.6 子代理、任务与计划模式

子代理是主 Agent 可以派生的下级 Agent。只有 `DOCKER` 和 `CUSTOM` 后端支持子代理；默认 `GUARDED_LOCAL` 会关闭这项能力。

最简单的本地子代理继承父 Agent 的模型，并使用独立 workspace：

```java
@Override
protected List<SubagentDeclaration> subagents() {
    return List.of(SubagentDeclaration.builder()
            .name("researcher")
            .description("负责检索资料并汇总要点")
            .inlineAgentsBody("你是资料检索专员，只输出要点清单。")
            .workspaceMode(WorkspaceMode.ISOLATED)
            .steps(10)
            .tools(List.of("query_order"))
            .skills(List.of("research"))
            .persistSession(true)
            .build());
}

@Override
protected boolean enablePlanMode() { return true; }   // 计划模式：先产出计划再逐步执行
```

子代理声明的常用选项：

| 选项 | 作用 |
| --- | --- |
| `inlineAgentsBody(...)` / `workspace(...)` | 直接给提示词，或从包含 `AGENTS.md` 的定义目录加载；二选一 |
| `workspaceMode(ISOLATED / SHARED)` | 使用独立 workspace，或与父 Agent 共用 workspace |
| `model(...)` | 覆盖模型；省略时继承父模型 |
| `steps` / `temperature` / `topP` / `variant` | 控制子代理迭代次数和模型参数 |
| `tools(...)` / `skills(...)` | 工具和技能白名单；空列表表示继承全部 |
| `persistSession(true)` | 用稳定 spawn key 保存子代理会话，支持跨父调用和重启恢复 |
| `inheritParentPermissions(true)` | 继承父 Agent 的 `DENY` 规则，默认开启 |
| `mode` / `hidden` / `exposeToUser` | 控制可派生范围、是否对模型可见及是否暴露为用户线程 |

`model(...)` 通过 AgentScope `ModelRegistry` 解析。比如 DeepSeek 的完整 ID 是 `deepseek:deepseek-chat`，并读取 `DEEPSEEK_API_KEY`；它不会读取 `liteflow.agent.openai-compatible.deepseek.*`。不需要单独凭据时，省略该项继承父模型最稳妥。

远程子代理通过 AgentScope task HTTP 服务调用，与第 13 章的 A2A 协议不是同一种接口：

```java
SubagentDeclaration.builder()
        .name("remote-reviewer")
        .description("调用远程服务审查代码")
        .url("https://agent.example.com")
        .headers(Map.of("Authorization", "Bearer " + token))
        .remoteStreaming(true)
        .remoteAskPolicy(RemoteAskPolicy.DENY)
        .build();
```

远程声明还支持事件明细级别和静态上下文属性。远程 HITL 默认自动拒绝，避免任务无限等待；需要其他行为时显式设置 `remoteAskPolicy(...)`。

任务清单默认存放在 workspace；`taskRepository()` 可替换为数据库等自定义实现，并用 `ownsTaskRepository(...)` 声明关闭责任。`additionalContextFiles()` 可把 workspace 内的额外文件注入每次调用。`customizeHarness(...)` 是最终 builder 定制入口，但不能替换 LiteFlow 托管的模型、Toolkit、文件系统、权限和核心能力。

## 13. A2A：调用远程 Agent

`liteflow-agent-a2a` 提供 A2A（Agent-to-Agent）协议客户端，用于在链中调用远程 Agent：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-a2a</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

下面的组件从远端标准 well-known 地址发现 Agent Card；示例中的 Agent Card 地址需要 Bearer Token：

```java
@Component("a2aAgent")
public class MyA2aAgentCmp extends A2aAgentComponent {

    private final AgentCardResolver resolver;

    public MyA2aAgentCmp(
            @Value("${remote.agent.base-url}") String baseUrl,
            @Value("${remote.agent.token}") String token) {
        this.resolver = WellKnownAgentCardResolver.builder()
                .baseUrl(baseUrl)
                .authHeaders(Map.of("Authorization", "Bearer " + token))
                .build();
    }

    @Override
    protected String remoteAgentName() {
        return "remote-assistant";
    }

    @Override
    protected AgentCardResolver agentCardResolver() {
        return resolver;
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return getSlot().getChainReqData(getSlot().getChainId()).toString();
    }
}
```

```properties
remote.agent.base-url=https://agent.example.com
remote.agent.token=${REMOTE_AGENT_TOKEN}
```

这里的 `authHeaders(...)` 只用于获取 Agent Card，不会自动带到后续 JSON-RPC 请求。若远端调用端点也需要鉴权，还要在 `a2aAgentConfig()` 中配置 `JSONRPCTransport`：Agent Card 已声明安全方案时可添加 `AuthInterceptor` 提供凭据，否则可注入自定义 `A2AHttpClient` 统一添加请求头。

远端不提供 well-known 地址时，可以构造 `AgentCard`，再用 `FixedAgentCardResolver` 返回固定卡片。

`a2aAgentConfig()` 可配置 A2A `ClientConfig` 和 Transport；`a2aClientRuntimeFactory()` 是替换客户端运行时的底层扩展点，通常只在接入自定义 Transport 或离线测试时覆写。默认实现会为每次调用创建独立的远程 Agent 实例。

当前 A2A 组件只支持文本输出和一条用户消息。请求会自动携带 `userId`、`conversationId`、`agentKey`、`traceId` 元数据，并受第 14 章的总超时和并发守卫控制。

本模块只提供客户端。要把本地 Agent 暴露为 A2A 服务，可使用 AgentScope 的 `agentscope-extensions-a2a-server`。

## 14. 可靠性与错误处理

### 14.1 迭代上限、重试与回退模型

一次 Agent 调用最多执行多少轮“推理 → 工具 → 再推理”，由 `maxIterations()` 控制；未覆写时使用 `liteflow.agent.defaults.max-iterations`，默认 50。达到上限会停止继续迭代，避免模型或工具陷入循环。

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

`liteflow.agent.runtime.timeout` 是一次 Agent 调用的总截止时间，默认 2 分钟，超时抛出 `AgentInvocationException(TIMEOUT)`。还可以用 `modelExecutionConfig()` 和 `toolExecutionConfig()` 分别限制单次模型请求和工具调用；HITL、Harness Shell 另有各自的等待上限。内层超时不应大于总超时。

### 14.4 并发守卫

同一身份（namespace + userId + conversationId + agentKey）的并发调用会被串行化，防止会话状态错乱。默认进程内实现，可替换为自定义实现（如基于 Redis 的分布式锁）：

```properties
liteflow.agent.invocation-guard.mode=BEAN
liteflow.agent.invocation-guard.bean-name=myAgentInvocationGuard   # AgentInvocationGuard 实现 bean
liteflow.agent.invocation-guard.acquire-timeout=2m                 # 等待锁的超时
```

`invocation-guard.lease-duration` 当前是保留配置，内置运行路径不读取它。自定义守卫只能依赖传入的 `acquire-timeout`，如果需要租约续期，应在守卫实现内部完成。

### 14.5 异常类型

- `AgentConfigException`：配置/构建期错误（缺 namespace、非法参数、开启了未启用的工具等），fail-fast。
- `AgentInvocationException`：调用期错误，携带类型 `TIMEOUT` / `INTERRUPTED` / `ACQUISITION_FAILED` / `PERMISSION` / `STRUCTURED_OUTPUT`。
- 在链上体现为 `response.isSuccess() == false`，异常通过 `response.getCause()` 获取。

### 14.6 本次调用上下文

组件的大多数动态扩展点都会收到 `LiteFlowAgentContext`。常用信息包括：

| 信息 | API |
| --- | --- |
| 身份 | `getNamespace()`、`getUserId()`、`getConversationId()`、`getAgentKey()` |
| 链路 | `getChainId()`、`getNodeId()`、`getRequestId()`、`getTraceId()` |
| 时间与输出 | `getDeadline()`、`getOutputSpec()`、`isCancelled()` |
| 本次统计 | `getChatUsage()`、`getUsedSkills()`、`getConfirmationEvents()` |
| LiteFlow 数据 | `getSlot()` |

需要把业务对象传给 AgentScope 的模型、工具或中间件时，覆写 `customizeRuntimeContext(RuntimeContext.Builder, LiteFlowAgentContext)`，把值放入 builder。该方法每次调用都会执行，适合租户、鉴权和请求级元数据；不要在这里替换 LiteFlow 已放入的 `Slot` 和 `LiteFlowAgentContext`。

## 15. 离线测试自己的 Agent 链

不需要真实大模型也能测试编排链路：覆写 `buildModel()` 返回一个进程内的假 `io.agentscope.core.model.Model` 实现即可，Spring 上下文与 EL 链照常执行：

```java
@Override
protected ModelSpec<?> model() {
    throw new UnsupportedOperationException("offline test uses buildModel");
}

@Override
protected Model buildModel() {
    return new FakeModel(); // 自己实现的 Model，按脚本返回固定答复/工具调用
}
```

参考实现见测试模块 `liteflow-testcase-el/liteflow-testcase-el-agent` 中的 `ScriptedChatModel`（支持按队列编排「文本答复 → 工具调用 → 异常」脚本，并能回放每次模型调用的入参做断言）。该模块的 `feature/*` 目录覆盖了本文档大部分特性的完整可运行示例，`platform/*` 目录是各平台的连通性示例，均可直接参考。

## 16. 配置速查

所有配置前缀为 `liteflow.agent.*`。Spring Boot 3、Spring Boot 4 和 Solon 的集成模块都会绑定这些配置。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `runtime.namespace` | 无（必填） | 运行时命名空间，隔离会话状态 |
| `runtime.default-user-id` | `anonymous` | 默认用户 ID |
| `runtime.timeout` | `2m` | 单次 Agent 调用总超时 |
| `defaults.max-iterations` | `50` | 全局最大 Agent 迭代次数 |
| `conversation-history-enabled` | `false` | 自动登记会话并保存独立的输入、答复及失败记录；见 §5.6 |
| `state-store.type` | `JSON` | 会话状态存储：`JSON` / `REDIS` / `MYSQL`（均持久化） |
| `state-store.json-root` | `./data/agent-state` | JSON 存储根目录 |
| `state-store.failure-policy` | `FAIL_FAST` | 会话状态加载失败策略：`FAIL_FAST` / `LOG_AND_CONTINUE`；不影响构建和保存失败 |
| `state-store.redis.uri` | 无 | Redis 直连 URI（与 `redis.client-bean-name` 二选一，需 redis 模块） |
| `state-store.redis.client-bean-name` | 无 | 已有 Jedis、Lettuce、Redisson 或 `RedisClientAdapter` bean 名 |
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
| `invocation-guard.lease-duration` | `2m` | 保留项，当前内置运行路径不读取 |
| `hitl.confirmation-timeout` | `2m` | 人工确认等待超时 |
| `hitl.fail-on-denied-tool` | `false` | 工具被拒绝时是否让链失败 |
| `workspace.backend` | `GUARDED_LOCAL` | core 内置文件与 Shell 工具的后端边界 |
| `workspace.root` | 无 | 工作区根目录；Harness 始终必填，core 开启文件或 Shell 工具时必填 |
| `workspace.trusted-local` | `false` | 确认信任本地文件系统（core 开启文件或 Shell 工具时需 `true`） |
| `workspace.auto-create` | `true` | 自动创建工作区目录（harness 受控文件系统消费） |
| `workspace.max-file-bytes` | `10485760` | 单文件大小上限（10MB，harness 受控文件系统消费） |
| `shell.mode` | `DISABLED` | Shell 工具模式：`WHITELIST` / `DISABLED` |
| `shell.whitelist` | 内置清单 | 命令白名单，仅匹配首段命令；底层校验器把空白名单视为全部放行，因此 LiteFlow 在开启 Shell 工具时拒绝空白名单 |
| `shell.timeout` | `30s` | 单条命令超时（Harness Shell 工具消费） |
| `logging.enabled` | `true` | Agent 推理/行动/错误过程日志 |
| `skills.enabled` | `false` | 是否启用技能 |
| `skills.path` | `./skills` | 技能仓库目录；`classpath:` 前缀表示 classpath 资源，其他值表示文件系统路径 |
| `skills.strict` | `true` | 兼容保留项，当前不改变解析或错误策略 |
| `openai.api-key` / `openai.base-url` | 无 | OpenAI 凭据 |
| `anthropic.api-key` / `anthropic.base-url` | 无 | Anthropic 凭据 |
| `gemini.api-key` / `gemini.base-url` | 无 | Gemini 凭据 |
| `dashscope.api-key` / `dashscope.base-url` | 无 | DashScope 凭据 |
| `openai-compatible.<key>.api-key` / `.base-url` | 无 | OpenAI 兼容平台凭据（含 deepseek/kimi/glm/minimax 预设） |
| `anthropic-compatible.<key>.api-key` / `.base-url` | 无 | Anthropic 兼容网关凭据（均必填） |
| 各平台的 `extra.*` | 无 | 保留字段，当前内置 Provider 不读取 |
| `harness.filesystem-backend` | `GUARDED_LOCAL` | Harness 文件系统后端：`GUARDED_LOCAL` / `DOCKER` / `CUSTOM` |
| `harness.trusted-local` | `false` | `GUARDED_LOCAL` 需设为 `true` |
| `harness.docker.image` | `ubuntu:22.04` | 沙箱镜像 |
| `harness.docker.workspace-root` | `/workspace` | 沙箱内工作目录 |
| `harness.docker.memory-size-bytes` | `536870912` | 沙箱内存上限（512MB） |
| `harness.docker.cpu-count` | `1` | 沙箱 CPU 数 |
| `harness.docker.network` | `none` | 沙箱网络模式 |
| `harness.docker.snapshot-root` | 无 | 本地 sandbox 快照目录；与 `sandboxSnapshotProvider()` 互斥 |
| `harness.docker.workspace-projection-enabled` | `true` | 是否把宿主 workspace 的允许内容投影到 sandbox |
| `harness.docker.workspace-projection-roots` | `AGENTS.md, skills, subagents, knowledge, .skills-cache` | 允许投影的相对路径列表 |

## 17. 组件扩展点速查

`AgentComponent` 必须实现的抽象方法：

| 方法 | 说明 |
| --- | --- |
| `model()` | 返回 `ModelSpec`，决定用哪个平台哪个模型；即使覆写 `buildModel()` 也必须实现 |
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
| `mcpClients()` / `ownsMcpClient(...)` | 接入 MCP 客户端，并声明是否由 Runtime 关闭 |
| `middlewares()` | 注册中间件 |
| `enableWorkspaceFileTools()` / `enableShellTool()` | 开启内置文件 / Shell 工具 |
| `structuredOutputType()` / `structuredOutputSchema()` | 结构化输出（二者互斥） |
| `handleReply(Msg, LiteFlowAgentContext)` | 自定义最终答复的去向（也是读取 `getChatUsage()` 的时机，见 6.2） |
| `resolveUserId(Slot)` / `resolveConversationId(Slot)` / `agentKey()` | 会话身份解析 |
| `stateStoreResolver()` | 自定义状态存储解析 |
| `permissionContext()` / `confirmationHandler()` / `stopOnReject()` | HITL 权限与确认；Harness 默认 `BYPASS`，仅权限策略产生 `ASK` 时需要确认处理器 |
| `skillRepositoryRegistrations()` / `skillFilter()` / `dynamicSkillsEnabled()` | 技能仓库注册、ownership、过滤和动态技能开关 |
| `transformSystemPrompt(...)` | 动态改写系统提示词 |
| `customizeRuntimeContext(...)` | 写入请求级 AgentScope 上下文 |
| `modelExecutionConfig()` / `toolExecutionConfig()` | 模型 / 工具执行配置 |
| `customizeAgent(ReActAgent.Builder)` | 底层定制（不可替换框架托管的模型、工具、存储与中间件） |

`HarnessAgentComponent` 额外提供：

| 方法 | 说明 |
| --- | --- |
| `compactionConfig()` / `memoryConfig()` | 上下文压缩和长期记忆 |
| `additionalContextFiles()` | 注入 workspace 内的上下文文件 |
| `subagents()` / `enablePlanMode()` | 子代理和计划模式 |
| `filesystemConfigurer()` | `CUSTOM` 文件系统后端，选择该后端时必填 |
| `sandboxSnapshotProvider()` / `dockerSandboxClient()` | 自定义 Docker 快照与客户端 |
| `taskRepository()` / `ownsTaskRepository(...)` | 自定义任务存储及关闭责任 |
| `toolResultEvictionConfig()` | 大型工具结果落盘和预览 |
| `ownedHarnessResources()` | 注册随 Runtime 关闭的额外资源 |
| `customizeHarness(...)` | 最终 builder 定制，不能替换 LiteFlow 托管能力 |

`A2aAgentComponent` 必须实现 `remoteAgentName()`、`agentCardResolver()` 和 `userPrompt(...)`；可通过 `a2aAgentConfig()` 调整客户端，通过 `a2aClientRuntimeFactory()` 替换底层运行时。

## 18. 常见报错与排查

绝大多数配置问题都是构建期 fail-fast 的 `AgentConfigException`，报错信息可直接对照下表定位（按报错原文搜索即可）：

| 报错信息（片段） | 原因 | 处理 |
| --- | --- | --- |
| `liteflow.agent.runtime.namespace is required before execution` | 未配置运行时命名空间 | 补上 `liteflow.agent.runtime.namespace`，见 [§2.2](#22-最小配置) |
| `state-store type REDIS requires the liteflow-agent-redis module on the classpath`（MYSQL 同理） | 选了 REDIS/MYSQL 后端但没引入对应模块 | 引入 `liteflow-agent-redis` / `liteflow-agent-mysql`，见 [§5.4](#54-statestore会话状态存在哪) |
| `state-store type REDIS requires either state-store.redis.uri or state-store.redis.client-bean-name` | 连接源缺失 | 配置 uri 或 client-bean-name 其一（两者都配会报 mutually exclusive） |
| `Redis state store could not be created` | URI 模式创建 Lettuce 客户端失败 | 检查地址和网络；bean 模式还需确认客户端类型受支持，具体连通性检查取决于 Jedis、Lettuce、Redisson 或 `RedisClientAdapter` 实现 |
| `MySQL state store could not be created` | DataSource 创建失败、连不上库，或库表准备失败 | 检查 JDBC 连接；或设 `mysql.create-if-not-exist=true`，或预先建库建表 |
| `enableShellTool requires liteflow.agent.shell.mode != DISABLED` | 开了 shell 工具但 shell 模式未启用 | 设 `liteflow.agent.shell.mode=WHITELIST`，见 [§4.3](#43-内置工具agentscope-文件工具与-shell-工具) |
| `enableShellTool requires a non-empty / non-blank liteflow.agent.shell.whitelist` | 白名单为空或含空白条目；底层校验器会将空白名单视为全部放行，因此 LiteFlow 主动拒绝 | 提供非空且无空白项的白名单 |
| `built-in workspace tools require workspace.trustedLocal=true`（或 `...a valid workspace.root`、`...GUARDED_LOCAL`） | 文件工具或 Shell 工具的工作区前置条件不满足 | 使用 `GUARDED_LOCAL`，配置 `workspace.root` 并显式设置 `trusted-local=true`，见 [§4.3](#43-内置工具agentscope-文件工具与-shell-工具) |
| `structuredOutputType and structuredOutputSchema are mutually exclusive` | 两种结构化输出覆写同时存在 | 只保留其一，见 [§7](#7-结构化输出) |
| `Harness workspace.root must not be blank` / `Harness shell.timeout must be positive` | Harness 文件系统配置缺失/非法 | 补 `workspace.root`、检查 `shell.timeout`，见 [§12](#12-harness-模块上下文工程与沙箱) |
| `Harness CUSTOM filesystem requires a non-null filesystemConfigurer` | 选择了 `CUSTOM`，但组件没有提供文件系统 | 覆写 `filesystemConfigurer()`，见 [§12.2](#122-文件系统后端) |
| `Agent identity changed after this component runtime was initialized` | 组件运行后改变了 `agentKey`，常见原因是拼入 `requestId` | 让 `agentKey` 保持稳定；请求级数据放入 prompt 或上下文，见 [§5.5](#55-agent-runtime-生命周期) |
| 子代理工具不可用或声明未生效 | Harness 使用默认 `GUARDED_LOCAL` 后端 | 改用具备隔离边界的 `DOCKER` 或自行实现 `CUSTOM` 后端，见 [§12.6](#126-子代理任务与计划模式) |
| 子代理 `.model("deepseek-chat")` 未切换模型 | ID 未匹配 AgentScope `ModelRegistry`，因此继承了父模型 | 使用 `deepseek:deepseek-chat` 并配置 `DEEPSEEK_API_KEY`，或省略 `model(...)` |
| `No AgentConfirmationHandler is configured` / `Multiple AgentConfirmationHandler beans found` | `ASK` 没有处理器，或容器中存在多个全局处理器 | 覆写 `confirmationHandler()`，或只保留一个全局 bean，见 [§10](#10-人工确认hitl) |
| `A2A client requires exactly one UserMessage` / `A2A client supports TEXT output only` | A2A 客户端输入/输出形态不符合协议约束 | 输入改为单条用户消息；结构化需求在远端完成后以文本返回，见 [§13](#13-a2a调用远程-agent) |

调用期的 `AgentInvocationException`（TIMEOUT / INTERRUPTED / ACQUISITION_FAILED / PERMISSION / STRUCTURED_OUTPUT）见 [§14.5](#145-异常类型)。
