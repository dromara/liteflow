# LiteFlow ReAct Agent 使用指南

本文介绍如何在 LiteFlow 中使用 `liteflow-react-agent`，把 agentscope-java 的 `ReActAgent` 当作普通 LiteFlow 节点编排到 EL 链路中。

读完本文后，你应该能够：

- 引入对应模型平台模块，并完成 `liteflow.agent.*` 配置；
- 编写一个继承 `ReActAgentComponent` 的 Agent 组件；
- 在 EL 中组合 Agent、普通节点、条件路由和并行节点；
- 通过 `ExecuteOption.eventListener(...)` 接收 Agent 执行中的流式事件；
- 正确理解 runtime identity、StateStore、workspace、内置文件工具、Shell 工具和 Skills 的边界。

> 当前仓库根版本：`2.16.1.1`。
>
> 当前源码中 `liteflow-react-agent` 聚合模块的 `maven.compiler.source` / `target` 为 `17`，根 `compile-17+` profile 会在 JDK 17 及以上激活该模块。实际运行时还需要满足 agentscope-java 及具体模型 SDK 的运行要求。

---

## 1. 模块说明

`liteflow-react-agent` 是一个聚合模块，核心能力在 `liteflow-react-agent-core`，不同模型供应商由独立子模块提供便捷入口。

| 模块 | 作用 |
| --- | --- |
| `liteflow-react-agent-core` | `ReActAgentComponent`、`ModelSpec`、runtime identity/guard、StateStore、Middleware、流式事件桥接、workspace 文件工具与受管 Shell 工具 |
| `liteflow-react-agent-openai` | OpenAI 官方 API + OpenAI 兼容协议，内置 DeepSeek、Kimi、GLM、Minimax 便捷入口 |
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

业务应用仍需引入 LiteFlow 对应的运行集成，例如 `liteflow-spring-boot-starter`、`liteflow-spring-boot4-starter` 或其他已有 LiteFlow starter；模型平台模块只提供 ReAct Agent 能力和平台 SDK 入口。如果一个应用里要同时使用多个模型平台，可以同时引入多个平台模块。

### 2.2 配置 LiteFlow 与 Agent

最小配置需要包含规则文件、非空 runtime namespace 和模型凭据。内置本地工具默认关闭。

```properties
liteflow.rule-source=agent/flow.el.xml

liteflow.agent.runtime.namespace=my-service
liteflow.agent.shell.mode=disabled

liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
# DeepSeek.of(...) 已内置默认 baseUrl；如需覆盖再配置下一行。
liteflow.agent.openai-compatible.deepseek.base-url=https://api.deepseek.com/v1
```

`liteflow.agent.runtime.namespace` 是必填项。没有配置时，首次执行 Agent 组件会抛出：

```text
AgentConfigException: liteflow.agent.runtime.namespace is required before execution
```

只有组件显式开启 workspace/Shell 工具时才需要 `workspace.root`，并且必须同时配置 `workspace.trusted-local=true`。相对路径基于 JVM `user.dir`；生产环境建议使用专用绝对路径。

### 2.3 编写 Agent 组件

Agent 组件继承 `ReActAgentComponent`，至少实现三个方法：

- `model()`：返回一个 `ModelSpec<?>`，声明使用哪个平台、哪个模型及可选高级参数；
- `systemPrompt()`：创建 Agent 时使用的系统提示词（框架会在它前面自动拼接一段统一系统提示词，详见 [§ 3](#3-reactagentcomponent-扩展点)）；
- `userPrompt(LiteFlowAgentContext)`：每次调用时发送给 Agent 的用户消息，并接收本轮不可跨调用保存的上下文。

AgentScope 2 执行上下文通过 `LiteFlowAgentContext` 参数与 AgentScope `RuntimeContext` 传递，不使用静态或线程本地 holder。上下文只能在当前 `process()` 生命周期内使用。

```java
package demo.agent;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import org.springframework.stereotype.Component;

@Component("deepseekAgent")
public class DeepSeekAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        return DeepSeek.of("deepseek-chat");
    }

    @Override
    protected String systemPrompt() {
        return "你是一名简洁的中文助理，回答严格控制在两句话以内。";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        Object req = context.getSlot().getChainReqData(context.getChainId());
        return req == null ? "" : req.toString();
    }

    @Override
    protected boolean enableShellTool() { return false; }

    @Override
    protected boolean enableWorkspaceFileTools() { return false; }
}
```

如果自定义工具、Middleware 或 Model 会被缓存并跨多次 invocation 复用，不要在这些对象中保存某次调用的 `LiteFlowAgentContext` 引用。工具应从当次 AgentScope `RuntimeContext` 读取上下文，Middleware 应使用回调参数。

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

默认情况下，Agent 回复会写入 `slot.responseData`。如果后续节点希望从指定位置读取回复，可以覆写 `handleReply(reply, context)`，或者像测试用例中的 `RecordReplyCmp` 一样把 `responseData` 转存到节点输出。

### 2.5 下游节点如何拿到 Agent 的执行结果

ReAct Agent 节点执行完后，结果传递给下一个节点有两种方式。

**方式 1：默认走 `slot.responseData`（最简单）**

`ReActAgentComponent#handleReply()` 默认实现：

```java
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    AgentReplyHandler.handle(reply, context.getOutputSpec(), context.getSlot());
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

需要做结构化处理、写到 ContextBean，或者一条链路里有多个 Agent 节点时，建议覆写 `handleReply`，否则后一个 Agent 的 `responseData` 会覆盖前一个：

```java
@Override
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    String text = reply == null ? null : reply.getTextContent();
    // 选择 1：写入自定义 ContextBean
    context.getSlot().getContextBean(MyAgentCtx.class).setReply(getNodeId(), text);
    // 选择 2：以 nodeId 为 key 存到 slot 输出，避免相互覆盖
    context.getSlot().setOutput(getNodeId(), text);
    // 选择 3：把本轮累计 token 用量一起落盘，供下游节点或调用方读取
    ChatUsage usage = context.getChatUsage();
    if (usage != null) {
        context.getSlot().setOutput(getNodeId() + ".usage", Map.of(
                "inputTokens", usage.getInputTokens(),
                "outputTokens", usage.getOutputTokens(),
                "totalTokens", usage.getTotalTokens(),
                "timeSeconds", usage.getTime()
        ));
    }
}
```

下游节点对应使用 `slot.getContextBean(MyAgentCtx.class)` 或 `slot.getOutput(nodeId)` 读取。`ChatUsage` 来自 `io.agentscope.core.model.ChatUsage`；`context.getChatUsage()` 的语义与边界见 [§ 3](#3-reactagentcomponent-扩展点)。

> **多 Agent 节点共存的注意事项**：默认 `responseData` 是 slot 级别的单一字段，后写覆盖先写。链路中存在多个 ReAct Agent 时，请覆写 `handleReply` 用 `setOutput(nodeId, ...)` 或自定义 ContextBean 区分各 Agent 的输出。

### 2.6 流式输出

LiteFlow 的 `execute2Resp(...)` 仍然保持原有语义：整条 chain 执行完成后返回 `LiteflowResponse`。如果希望在 Agent 执行过程中实时拿到模型输出、工具结果或最终结果，可以通过 `ExecuteOption.eventListener(...)` 注册事件监听器。

```java
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.LiteflowResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

List<FlowEvent> events = new CopyOnWriteArrayList<>();

LiteflowResponse response = flowExecutor.execute2Resp("deepseekChain", "用一句话介绍 LiteFlow",
        ExecuteOption.of()
                .conversationId("chat-user-1-conv-1")
                .eventListener(event -> {
                    if ("agent.reasoning".equals(event.getType()) && event.getText() != null) {
                        // 可在这里转发到 SSE、WebSocket 或命令行输出。
                        System.out.print(event.getText());
                    }
                    events.add(event);
                }));

if (response.isSuccess()) {
    Object finalReply = response.getSlot().getResponseData();
}
```

当前 ReAct Agent 会把 AgentScope 的流事件转换成 LiteFlow 通用 `FlowEvent`：

| `FlowEvent#getType()` | 含义 | 典型内容 |
| --- | --- | --- |
| `agent.reasoning` | 模型推理 / 回复过程 | 增量文本、最终 assistant 消息、工具调用请求 |
| `agent.tool_result` | 工具执行结果 | 工具输出或工具执行中的增量片段 |
| `agent.summary` | 达到最大迭代次数后的总结 | summary 生成过程或最终 summary |
| `agent.result` | 本轮 Agent 最终结果 | 与最终 `handleReply(reply, context)` 使用的消息一致 |

`FlowEvent` 会携带 `chainId`、`nodeId`、`requestId`、`conversationId`、`text`、`last`、`timestamp` 和原始 `data`。其中 `nodeId` 对多 Agent 链路很重要：`WHEN(agentA, agentB)` 并发执行时，多个 Agent 的流式事件可能交错到达，调用方应按 `nodeId`、`conversationId` 或业务自定义字段分组展示。

无论是否注册 `eventListener`，`ReActCallExecutor` 始终通过 `agent.call(...)`（结构化输出时使用对应重载）执行。AgentScope 的类型化事件由 `FlowEventBridgeMiddleware` 观察；当前 Slot 注册了 listener 时，middleware 才把事件映射为 `FlowEvent` 并投递。调用结束后组件执行 `handleReply(reply, context)`；AgentScope 与所配置 `AgentStateStore` 的状态读写路径不因 listener 是否存在而改变。

`eventListener` 在事件投递所在的执行线程中同步回调。生产环境转发到 SSE、WebSocket 或消息队列时，建议只做轻量入队或缓冲，不要执行耗时 I/O。`FlowEventBridgeMiddleware` 按 `liteflow.agent.event.listener-failure-mode` 处理 listener 异常：默认 `FAIL_FAST` 会让本次调用失败，`LOG_AND_CONTINUE` 记录警告并继续处理模型事件。

---

## 3. ReActAgentComponent 扩展点

`ReActAgentComponent#process()` 是 `final`。框架在其中统一完成配置读取、identity 解析、invocation guard、组件 runtime 懒构建、StateStore 恢复/保存、调用与回复处理。业务侧通过覆写受保护方法定制行为。

| 方法 | 是否必须 | 默认行为 | 说明 |
| --- | --- | --- | --- |
| `model()` | 是 | 无 | 返回 `ModelSpec<?>`，由框架从 `AgentConfig` 解析凭据并构造 agentscope `Model` |
| `systemPrompt()` | 是 | 无 | 返回组件共享 runtime 的基础系统提示词；每个组件实例仅在懒构建 runtime 时调用一次 |
| `userPrompt(LiteFlowAgentContext)` | 是 | 无 | 返回本轮用户消息，每次 `process()` 都调用 |
| `transformSystemPrompt(prompt, LiteFlowAgentContext)` | 否 | 原样返回基础提示词 | 按 invocation 动态转换系统提示词；在 AgentScope 2 Middleware 调用链中执行 |
| `tools()` | 否 | 空列表 | 注册自定义 `@Tool` 对象 |
| `skillRepositories()` | 否 | 空列表 | 返回 AgentScope 2 `AgentSkillRepository`；需要文件技能时显式创建 `FileSystemSkillRepository` |
| `skillFilter()` | 否 | `SkillFilter.all()` | 以 skill ID 过滤 repository 中可见的技能 |
| `middlewares()` | 否 | 空列表 | 注册 AgentScope 2 `MiddlewareBase`；组件 runtime 构建时固定 |
| `resolveConversationId(Slot)` | 否 | 先复用 `slot.conversationId`，再读 `chainReqData` Map 中的 `conversationId`，最后生成 ID | 决定本次调用所属业务会话；同一条 chain 内首个 Agent 写回 slot 后，后续 Agent 默认复用 |
| `agentKey()` | 否 | 当前 `nodeId`，为空时为 `default` | 在同一 conversation 中区分不同 Agent 实例和记忆；默认不同节点互相隔离 |
| `maxIterations()` | 否 | `-1` | 返回正数时覆盖全局 `defaults.max-iterations` |
| `enableShellTool()` | 否 | `false` | 是否注册内置受管 Shell 工具；启用时还要求 `shell.mode != DISABLED` 与 `workspace.trusted-local=true` |
| `enableWorkspaceFileTools()` | 否 | `false` | 是否注册内置 workspace 文件工具；启用时要求 guarded-local trusted workspace |
| `handleReply(reply, context)` | 否 | 按 output spec 写入 Slot | 自定义回复处理逻辑 |
| `buildModel()` | 否 | 委派 `model().resolve(agentConfig())` | 逃生舱：完全自行构造 agentscope `Model` |

`LiteFlowAgentContext` 作为 invocation 参数提供以下执行上下文：

| 方法 | 说明 |
| --- | --- |
| `getSlot()` | 当前 LiteFlow `Slot` |
| `getConversationId()` | 解析后的原始业务 conversation ID；安全 workspace ID 由 identity resolver 另行哈希生成 |
| `getAgentKey()` | 组件的原始稳定 Agent key，默认来自 `nodeId`；安全 state namespace 另行哈希生成 |
| `getRuntimeSessionId()` | 传给 AgentScope runtime 的安全 session ID |
| `getAgentNamespace()` | 隔离组件 runtime StateStore 的安全 namespace |
| `getUsedSkills()` | 本轮通过 load-skill 工具成功加载的 skill ID 列表 |
| `getChatUsage()` | 本次 `process()` 截至当前已累计的 token 用量（agentscope `ChatUsage`，含 `getInputTokens()` / `getOutputTokens()` / `getTotalTokens()` / `getTime()`（秒））；模型未上报或本轮尚未发生过 reasoning step 时返回 `null` |

注意：每个组件实例只懒建一个共享 runtime，`systemPrompt()` 只在该 runtime 构建时调用一次，不能读取构建期 `Slot` 来表达 invocation 动态信息。动态系统提示词应放在 `transformSystemPrompt(prompt, LiteFlowAgentContext)`，也可以由 per-call Middleware 根据回调中的 `RuntimeContext` 处理；动态用户输入与模型路由分别放在 `userPrompt(context)` 和 `routeModel(..., context)`。

**框架统一系统提示词**：你在 `systemPrompt()` 中返回的内容不是最终系统提示词。框架在 `effectiveSystemPrompt()` 中会**始终在你的提示词前面拼接**一段内置的 `DEFAULT_SYSTEM_PROMPT`，最终下发给底层 ReActAgent 的是 `DEFAULT_SYSTEM_PROMPT + "\n\n" + 你的 systemPrompt()`。这段默认提示词的内容大致是：

```text
请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
```

由此带来的几个行为，做提示词工程时需要心里有数：

- 模型默认会**用用户提问所用的语言**回答；如需固定输出语言，应在自己的 `systemPrompt()` 里显式覆盖；
- 模型在每次调用工具前会**先输出一两句推理摘要**——该内容会进入 `ReActLoggingMiddleware` 日志和流式 `agent.reasoning` 事件，属于预期行为；
- 当 `systemPrompt()` 返回空字符串或空白时，框架会**单独使用**这段默认提示词。

`getChatUsage()` 的累计口径：底层 agentscope 每次 model call 的 usage 由 `ChatUsageMiddleware` 写入当前 `LiteFlowAgentContext`，相同 event ID 会去重。因此 `context.getChatUsage()` 给出整次 `process()` 的累计值，而 `Msg#getChatUsage()` 只代表消息自身携带的 usage。

`tools()`、`middlewares()`、`skillRepositories()`、`skillFilter()` 与 `buildModel()` 都属于组件 runtime 构建期声明。不要让这些方法依赖单次请求数据；测试不同声明时应使用新的组件实例或 fresh application context。

**组件方法与 application 配置的优先级**

`ReActAgentComponent` 的部分受保护方法与 `liteflow.agent.*` 配置项控制的是同一件事。它们冲突时的合并规则并不统一，分三种：

| 重叠项 | 组件方法 | 对应配置 | 合并规则 |
| --- | --- | --- | --- |
| 最大迭代 | `maxIterations()` | `liteflow.agent.defaults.max-iterations` | 方法**返回正数时覆盖**配置；返回默认 `-1`（或非正数）时用配置 |
| Shell 工具 | `enableShellTool()` | `liteflow.agent.shell.mode` | 组件必须返回 `true`，配置必须不是 `DISABLED`，workspace 还必须显式 trusted-local |
| workspace 文件工具 | `enableWorkspaceFileTools()` | `liteflow.agent.workspace.*` | 组件必须返回 `true`，backend 必须为 `GUARDED_LOCAL` 且 `trusted-local=true` |

其余 `liteflow.agent.*` 配置（如 `runtime.*`、`state-store.*`、`workspace.*`）没有对应的简单开关；而 `model()` / `buildModel()` 与凭据配置是分工而非重叠。

---

## 4. ModelSpec 与模型入口

### 4.1 核心设计

`ModelSpec<SELF>` 是所有平台模型描述符的基类。子类按“哪个平台 + 哪个模型 + 可选高级参数”三段式给出，框架负责从 `AgentConfig` 解析凭据并构造 agentscope `Model`。

基类提供的共性参数（所有平台共享）：

| 方法 | 类型 | 说明 |
| --- | --- | --- |
| `temperature(double)` | `Double` | 采样温度 |
| `topP(double)` | `Double` | nucleus sampling |
| `topK(int)` | `Integer` | top-k sampling |
| `maxTokens(int)` | `Integer` | 最大输出 token |
| `seed(long)` | `Long` | 随机种子 |
| `stream(boolean)` | `Boolean` | 是否让底层模型请求使用流式模式 |
| `cacheControl(boolean)` | `Boolean` | 缓存控制；当前 OpenAI 与 DashScope 内置解析会下发该参数 |

所有参数均为可选，未设置时不写入 `GenerateOptions` 或模型 Builder，agentscope 使用服务端或 SDK 默认值。

注意：`ModelSpec.stream(true)` 只控制底层模型请求是否使用流式传输；调用方是否能在 LiteFlow 执行期间收到事件，取决于本次调用是否通过 `ExecuteOption.eventListener(...)` 注册了监听器。没有 listener 时，最终仍然只通过 `LiteflowResponse` 读取结果。

### 4.2 平台入口一览

每个平台模块提供一个不可变入口类，通过静态 `of(modelName)` 或 `custom(configKey, modelName)` 方法返回平台对应的 `Spec` 子类。Spec 子类在基类共性参数之上暴露平台个性参数。

| 模块 | 入口类 | Spec 子类 | 个性参数 |
| --- | --- | --- | --- |
| `liteflow-react-agent-openai` | `OpenAI` | `OpenAISpec` | `reasoningEffort`, `frequencyPenalty`, `presencePenalty` |
| `liteflow-react-agent-openai` | `DeepSeek` | `OpenAICompatibleSpec` | 继承 `OpenAISpec` 全部参数，内置默认 `baseUrl` |
| `liteflow-react-agent-openai` | `Kimi` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `GLM` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `Minimax` | `OpenAICompatibleSpec` | 同上 |
| `liteflow-react-agent-openai` | `OpenAICompatible` | `OpenAICompatibleSpec` | 自定义 `configKey`，用于任意 OpenAI 兼容厂商；无默认 `baseUrl` |
| `liteflow-react-agent-anthropic` | `Anthropic` | `AnthropicSpec` | `thinking(t -> t.budget(...).enabled(...))`；当前内置解析下发 `budget` |
| `liteflow-react-agent-anthropic` | `AnthropicCompatible` | `AnthropicSpec` | 自定义 `configKey`，用于 Anthropic 兼容网关 |
| `liteflow-react-agent-gemini` | `Gemini` | `GeminiSpec` | `thinking(t -> t.level(...).budget(...))` |
| `liteflow-react-agent-dashscope` | `DashScope` | `DashScopeSpec` | `thinking(t -> t.budget(...))`，设置 budget 时会启用 thinking |

### 4.3 使用示例

**OpenAI：**

```java
@Override
protected ModelSpec<?> model() {
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
protected ModelSpec<?> model() {
    return DeepSeek.of("deepseek-chat")
            .temperature(0.5);
}
```

凭据来源：`liteflow.agent.openai-compatible.deepseek.api-key`，`base-url` 可选；未配置时使用 `DeepSeek` 入口内置的默认地址。

**自定义 OpenAI 兼容厂商：**

```java
@Override
protected ModelSpec<?> model() {
    return OpenAICompatible.custom("myvendor", "my-model")
            .temperature(0.7);
}
```

凭据来源：`liteflow.agent.openai-compatible.myvendor.api-key` / `base-url`。自定义厂商没有内置默认地址，通常需要配置 `base-url`。

**Anthropic Claude：**

```java
@Override
protected ModelSpec<?> model() {
    return Anthropic.of("claude-sonnet-4-5")
            .temperature(0.5)
            .thinking(t -> t.budget(2000).enabled(true));
}
```

凭据来源：`liteflow.agent.anthropic.api-key`。

**Anthropic 兼容网关：**

```java
@Override
protected ModelSpec<?> model() {
    return AnthropicCompatible.custom("gateway", "claude-haiku");
}
```

凭据来源：`liteflow.agent.anthropic-compatible.gateway.api-key` / `base-url`。

**Google Gemini：**

```java
@Override
protected ModelSpec<?> model() {
    return Gemini.of("gemini-2.5-flash")
            .thinking(t -> t.level("high").budget(1024));
}
```

凭据来源：`liteflow.agent.gemini.api-key`。

**阿里云 DashScope：**

```java
@Override
protected ModelSpec<?> model() {
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
| `extra` | 可选，业务自定义键值；当前内置 ProviderSpec 尚未读取 |

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
- **兼容平台**（`DeepSeek` / `Kimi` / `GLM` / `Minimax` / `OpenAICompatible.custom` / `AnthropicCompatible.custom`）：从 `AgentConfig` 的 `Map<String, PlatformCredential>` 中按 `configKey` 读取。缺失时抛出提示 `liteflow.agent.<type>.<configKey>.api-key`。

### 4.6 逃生舱：buildModel()

如果 `ModelSpec` 无法满足需求（例如需要传入 agentscope 原生的高级参数），可以直接覆写 `buildModel()`，完全自行构造 agentscope `Model`：

```java
@Override
protected Model buildModel() {
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

覆写 `buildModel()` 后，默认实现中的 `model().resolve(agentConfig())` 不会被调用；但因为 `model()` 仍是抽象方法，子类仍需实现它。

---

## 5. Conversation、agentKey 与 memory

### 5.1 identity 各维度分别负责什么

当前源码把 workspace/runtime session 维度与 Agent state 维度分开：

- `namespace + userId + conversationId`：决定安全 `runtimeSessionId`、workspace 目录与 workspace lease；
- `namespace + agentKey`：决定组件 runtime 的 `agentNamespace`；
- `namespace + userId + conversationId + agentKey`：决定 Agent state 的 guard key 与最终 store session key。

每个 Agent 组件实例在首次执行时懒构建一个 `ReActAgentRuntime`。同一组件实例后续调用会复用：

- 同一个 `ReActAgent` 实例；
- 同一组 Model、Middleware、Toolkit 与 skill repositories；
- 同一个 namespaced `AgentStateStore`；
- 由 invocation guard 分别协调 workspace 与 Agent state 的调用租约。

`runtimeSessionId` 由 `namespace`、`userId` 与 `conversationId` 哈希生成，不含 `agentKey`。workspace 目录使用该 ID，workspace lease key 同样不含 `agentKey`，所以相同三元组下的不同 Agent 共享同一 workspace 和同一 workspace lease。`agentNamespace` 由 `namespace` 与 `agentKey` 生成；Agent state guard 还包含 `userId` 与 `conversationId`，因此不同 `agentKey` 的状态彼此隔离。

因此，同一个 `(namespace, userId, conversationId, agentKey)` 的 state 调用会串行执行。不同 `agentKey` 且不使用本地工具的组件可以并行；当组件需要 workspace lease 时，相同 `(namespace, userId, conversationId)` 的调用还会由共享 workspace lease 串行化。`agentKey` 不能提供文件隔离，文件隔离必须改变 `namespace`、`userId` 或 `conversationId`。

### 5.2 conversationId 从哪里来

默认实现是：

```java
protected String resolveConversationId(Slot slot) {
    String existing = slot.getConversationId();
    if (existing != null && !existing.isEmpty()) {
        return existing;
    }
    Object req = slot.getChainReqData(slot.getChainId());
    if (req instanceof Map<?, ?> map) {
        Object v = map.get(CONVERSATION_ID_REQUEST_KEY);
        if (v != null) {
            String s = v.toString();
            if (!s.isEmpty()) {
                return s;
            }
        }
    }
    return ConversationIdGenerator.generate();
}
```

`ConversationIdGenerator.generate()` 返回 `YYYYMMDD_<12 位 NanoId>` 形式的字符串（例如 `20260430_3F7K9PQRSTUV`）。首个 Agent 解析出 conversationId 后会写回 `slot.setConversationId(cid)`，同一条 chain 中后续 Agent 会默认复用它。

也可以在调用链路时显式传入 conversationId：

```java
LiteflowResponse response = flowExecutor.execute2Resp(
        "deepseekChain",
        request,
        ExecuteOption.of().conversationId("chat-user-1-conv-1"));
```

或者让框架先生成一个 conversationId，再从响应中取回供下一轮复用：

```java
LiteflowResponse first = flowExecutor.execute2Resp(
        "deepseekChain",
        request,
        ExecuteOption.of().autoConversationId());
String cid = first.getConversationId();
```

如果要按业务请求对象实现多轮对话，也可以覆写：

```java
@Override
protected String resolveConversationId(Slot slot) {
    ChatRequest req = slot.getChainReqData(slot.getChainId());
    return "chat-" + req.getUserId() + "-" + req.getConversationId();
}
```

`conversationId` 与 `agentKey` 只允许直接使用 `[a-zA-Z0-9_-]+`。其他字符会被 URL 编码，并把 `%` 替换为 `_`，从而安全地用作目录名或缓存 key。空值会变成 `_`。

### 5.3 agentKey 从哪里来

默认实现是当前节点的 `nodeId`：

```java
protected String agentKey() {
    String nodeId = getNodeId();
    return (nodeId == null || nodeId.isEmpty()) ? "default" : nodeId;
}
```

这意味着同一段 conversation 中，`mathAgent` 和 `summaryAgent` 默认有各自独立的 Agent 记忆；但它们共享 `workspace.root/<conversationId>/` 目录。

如果确实希望多个节点共享同一个 Agent 记忆，可以让它们返回同一个 `agentKey()`；如果希望同一个节点的不同执行完全隔离，可以把 `requestId` 或业务标识拼入 `agentKey()`。

### 5.4 组件 runtime 生命周期

组件首次执行时构建 runtime，后续 invocation 复用；容器停止时通过 `Closeable.close()` 等待正在执行的调用结束，再按 Agent、MCP/skill repository/model、StateStore wrapper 的顺序关闭。运行中修改模型、工具、Middleware 或 skill filter 不会重建现有 runtime；需要不同构建期声明时应使用独立组件实例，测试中则使用 fresh application context。

### 5.5 StateStore 持久化

`liteflow.agent.state-store.*` 控制 AgentScope 2 state 的保存位置与失败策略：

| `type` | 含义 | 适用场景 |
| --- | --- | --- |
| `MEMORY` | 默认值，组件关闭或进程退出后丢失 | 单进程内多轮对话与测试 |
| `JSON` | 保存到 `json-root` 下的 JSON 文件 | 本地持久化、小规模部署 |
| `BEAN` | 从容器按 `bean-name` 获取业务提供的 `AgentStateStore` | Redis、数据库或其它共享后端 |

```properties
liteflow.agent.state-store.type=JSON
liteflow.agent.state-store.json-root=./data/agent-state
liteflow.agent.state-store.failure-policy=FAIL_FAST
```

`failure-policy` 可选 `FAIL_FAST` 与 `WARN_AND_CONTINUE`。StateStore 会被 `GuardedNamespacedAgentStateStore` 包装，所有 runtime session key 都绑定到当前 Agent namespace；`BEAN` 类型的 store 由应用拥有，LiteFlow 不关闭它。

### 5.6 自定义 StateStore

简单后端可直接把 `AgentStateStore` 注册为 Spring/Solon Bean，并选择 `type=BEAN`。需要自定义解析、ownership 或按配置创建后端时，覆写组件的 `stateStoreResolver()`：

```java
@Override
protected AgentStateStoreResolver stateStoreResolver() {
    return config -> new ResolvedAgentStateStore(myStore, false);
}
```

resolver 返回的 `ResolvedAgentStateStore` 明确声明底层 store 是否由组件拥有；构建失败和正常关闭都遵守该 ownership。

---

## 6. Workspace 与内置工具

### 6.1 Workspace 目录结构

启用本地工具后，每个安全 runtime session ID 都会在 `liteflow.agent.workspace.root` 下获得一个哈希子目录：

```text
/var/lib/liteflow/agent-workspaces/
├── session-829355f33e5d.../
└── session-d5258b18d1a4.../
```

目录名不直接包含 conversationId、agentKey 或用户输入。StateStore 的 `json-root` 与工具 workspace 独立配置，工具无法通过正常相对路径读取 state 文件。

### 6.2 Workspace 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `backend` | `GUARDED_LOCAL` | 内置本地工具要求该 backend |
| `trusted-local` | `false` | 必须显式设为 `true` 才允许注册本地文件或 Shell 工具 |
| `root` | 无 | 启用本地工具时必填，workspace 根目录 |
| `auto-create` | `true` | 根目录不存在时是否自动创建 |
| `max-file-bytes` | `10485760` | `read_file` 单次最多读取的字节数；超出时截断读取 |
| `max-list-size` | `1000` | `list_files` 单次最多返回的条目数 |

`cleanup-on-session-expire` 与 `cleanup-on-jvm-shutdown` 仅保留给 1.x 配置迁移；2.0 core 不注册静态清理器或 JVM shutdown hook。需要归档/清理时由业务侧显式处理。

### 6.3 Workspace 文件工具

`enableWorkspaceFileTools()` 默认为 `false`。组件返回 `true` 且 workspace 明确设置 `trusted-local=true` 后，框架才注册 `WorkspaceFileTools`：

| 工具名 | 行为 |
| --- | --- |
| `read_file` | 读取当前 conversation workspace 内的文本文件，超过 `max-file-bytes` 时截断 |
| `write_file` | 覆盖写入文本文件，并自动创建父目录 |
| `list_files` | 列出目录内容，最多返回 `max-list-size` 条 |
| `delete_file` | 删除文件 |

所有路径都必须是相对路径。绝对路径或 `..` 越界路径会被拒绝。

当前文件工具面向文本内容：`read_file` 按 `max-file-bytes` 截断读取，`write_file` 是覆盖写入并自动创建父目录。业务如果允许 Agent 写入大文件或二进制文件，建议关闭内置文件工具，改用自定义工具做大小、类型和审计控制。

关闭方式：

```java
@Override
protected boolean enableWorkspaceFileTools() {
    return false;
}
```

### 6.4 受管 Shell 工具

`enableShellTool()` 默认为 `false`。组件返回 `true`、`shell.mode` 不是 `DISABLED`，并且 workspace 明确设置 `trusted-local=true` 后，框架才注册 `ManagedShellCommandTool`，工具名为 `execute_shell_command`。

Shell 工具在当前 conversation workspace 下执行命令。当前实现会按空白符切分命令字符串，用 `ProcessBuilder` 直接执行 token 列表，并通过首 token 做白名单或黑名单判断；不会通过系统 shell 解释管道、重定向、变量展开等语法。配置项如下：

| 配置项 | 默认值 |
| --- | --- |
| `mode` | `DISABLED` |
| `whitelist` | `ls, find, tree, stat, file, basename, dirname, pwd, which, cat, head, tail, grep, sed, awk, wc, sort, uniq, cut, tr, diff, echo, printf, expr, date, whoami, hostname, uname, env, df, du, ps, md5sum, sha256sum, jq, curl, wget, python3, node` |
| `blacklist` | `rm, sudo, shutdown, mkfs, dd` |
| `timeout` | `30s` |
| `max-output-bytes` | `1048576` |

生产环境建议默认关闭：

```properties
liteflow.agent.shell.mode=disabled
```

确实需要 Shell 时，优先使用 `WHITELIST`，并把白名单收窄到业务需要的命令。

默认白名单包含 `curl`、`wget`、`python3` 和 `node` 等能力较强的命令。即使当前实现不经过系统 shell、不支持管道和重定向，这些命令仍可能带来网络访问或脚本执行风险；生产环境不要直接沿用默认白名单。

### 6.5 ReAct 事件日志

框架内置 `ReActLoggingMiddleware`，在 AgentScope 2 Middleware 链中观察 reasoning、acting 与 error 事件并写入 SLF4J。是否输出由 `liteflow.agent.logging.react-enabled` 控制。

| 事件 | 日志格式 |
| --- | --- |
| `PreReasoningEvent` | `[agent:reason][conversationId:agentKey] >>> model=... messages=N` |
| `PostReasoningEvent` | `[agent:reason][conversationId:agentKey] <<< text=... toolCalls=[...]` |
| `PostReasoningEvent`（usage 附加行） | `[agent:reason][conversationId:agentKey] <<< usage input=N output=N total=N time=Ns`（仅当本步消息上报了 `ChatUsage` 时输出） |
| `PreActingEvent` | `[agent:act][conversationId:agentKey] >>> tool=... input=...` |
| `PostActingEvent` | `[agent:act][conversationId:agentKey] <<< tool=... result=...` |
| `ErrorEvent` | `[agent:error][conversationId:agentKey] ...` |

单次 model call 的 usage 同时由 `ChatUsageMiddleware` 累加到当前 `LiteFlowAgentContext`；在 `handleReply(reply, context)` 中读取 `context.getChatUsage()` 可得到本次 invocation 的累计值。

- 全局开关：`liteflow.agent.logging.react-enabled`（默认 `true`）。
- 输出 logger 名：`com.yomahub.liteflow.agent.middleware.ReActLoggingMiddleware`（可在 logback / log4j2 中独立调级）。
- 文本字段超过 500 字会被截断为 `...(truncated)`。

业务侧需要额外观察或改写执行时，可通过 `middlewares()` 注册 `MiddlewareBase`；不要保存某次 invocation 的 context。

---

## 7. Skills 支持

`liteflow-react-agent` 通过 AgentScope 2 的 `AgentSkillRepository` 与 `SkillFilter` 接入技能。Skill 适合承载“什么时候使用”“如何执行”的长指令；Java 工具仍通过组件 `tools()` 或 `customizeToolkit()` 显式注册。

### 7.1 开启 Skills

组件需要显式返回 repository。下面示例复用 `liteflow.agent.skills.*` 作为应用配置：

```properties
liteflow.agent.skills.enabled=true
liteflow.agent.skills.path=./skills
```

| `SkillsConfig` 字段 | 默认值 | 当前行为 |
| --- | --- | --- |
| `enabled` | `false` | core 不会自动读取；上例的组件覆写自行读取它，决定是否返回 repository |
| `path` | `./skills` | core 不会自动读取；上例把它传给 `FileSystemSkillRepository` 构造器 |
| `strict` | `true` | 仅保留配置绑定兼容；`strict` 字段当前不会被 `ReActAgentComponent`、`FileSystemSkillRepository` 或 `SkillFilter` 读取，设为 `false` 不会启用 warn-and-continue 模式 |

```java
@Override
protected List<AgentSkillRepository> skillRepositories() {
    if (!agentConfig().getSkills().isEnabled()) {
        return List.of();
    }
    return List.of(new FileSystemSkillRepository(
            Path.of(agentConfig().getSkills().getPath()), false));
}

@Override
protected boolean ownsSkillRepository(AgentSkillRepository repository) {
    return true;
}
```

`skills.path` 的相对路径基于 JVM `user.dir`；生产环境建议使用只读绝对路径。`FileSystemSkillRepository` 构造时若根路径不存在或不是目录，会立即抛出 `IllegalArgumentException`；枚举时会跳过没有 `SKILL.md` 的子目录，并对无法解析的单个 skill 记录 warning 后忽略。repository 非空且 `dynamicSkillsEnabled()` 为 `true` 时，LiteFlow 管理的动态技能 middleware 会注册 `load_skill_through_path`。

### 7.2 目录结构与 SKILL.md

推荐目录结构：

```text
skills/
├── demo/
│   └── SKILL.md
└── tool-skill/
    └── SKILL.md
```

最小 `SKILL.md` 示例：

```markdown
---
name: demo
description: Demo skill for LiteFlow ReAct agent
---

# Demo Skill

Use this skill when the request is about a simple demonstration.
```

`name` 是可读名称；`SkillFilter` 使用 `AgentSkill#getSkillId()` 返回的稳定 ID，不应直接假设 ID 等于目录名。

### 7.3 组件级技能过滤

如果某个 Agent 只能在指定技能内选择，覆写 `skillFilter()`：

```java
@Override
protected SkillFilter skillFilter() {
    AgentSkill demo = repository.getAllSkills().stream()
            .filter(skill -> skill.getName().equals("demo"))
            .findFirst()
            .orElseThrow();
    return SkillFilter.only(demo.getSkillId());
}
```

语义如下：

- `SkillFilter.all()`：允许 repository 中全部技能；
- `SkillFilter.none()`：不暴露任何技能；
- `SkillFilter.only(ids...)` / `except(ids...)`：按 skill ID 选择；
- invocation 可在 AgentScope `RuntimeContext` 中放入 overlay `SkillFilter`，进一步收窄本轮范围。

`SkillFilter` 只按 skill ID 做允许/拒绝判断，不负责验证 ID 是否存在，也不会触发 class 或 tool 反射加载。`only("missing-id")` 的结果是没有 repository skill 可见，而不是读取 `SkillsConfig.strict` 决定降级策略。

要彻底关闭动态技能，可不返回 repository，或覆写：

```java
@Override
protected boolean dynamicSkillsEnabled() {
    return false;
}
```

repository 与 filter 都是组件 runtime 构建期声明。修改 filter 后现有 runtime 不会自动重建；需要不同 filter 的组件或测试应使用独立组件实例/fresh context。

### 7.4 Java 工具与技能

LiteFlow 2.0 core 不反射实例化 `SKILL.md` 中声明的 Java 类。需要依赖注入的工具应注册为 Spring/Solon Bean，再由 Agent 组件注入并通过 `tools()` 返回；需要更细的动态选择时，可在自定义 Middleware 或 `customizeToolkit()` 中实现显式策略。这样工具 ownership、权限与审计边界都留在应用代码中。

### 7.5 记录本轮使用的技能

`LiteFlowAgentContext#getUsedSkills()` 返回当前 invocation 中已经成功加载过的 skill ID 列表。典型用法是在 `handleReply(reply, context)` 中把回复和技能使用情况一起写到下游可读的位置：

```java
@Override
protected void handleReply(Msg reply, LiteFlowAgentContext context) {
    context.getSlot().setOutput(getNodeId(), Map.of(
            "reply", reply == null ? "" : reply.getTextContent(),
            "skillsUsed", context.getUsedSkills()
    ));
}
```

使用边界：

- 每次 `process()` 开始前会清空上一轮记录；
- 只有 `load_skill_through_path` 成功加载的技能会被记录；
- 可在 `process()` 触发的调用链内读取，例如 `userPrompt(context)`、Middleware 与 `handleReply(reply, context)`；
- `process()` 最终清理后，后续 LiteFlow 生命周期回调不应再保存或读取该 context。

### 7.6 当前边界

当前实现使用 LiteFlow 管理的 `DynamicSkillMiddleware`，并关闭 Agent builder 自带的第二套 dynamic-skill middleware；向 middleware 传入的代码执行开关以及 builder 的 `skillCodeExecutionEnabled` 均为 `false`，技能加载不会隐式开启代码执行。Shell 与 workspace 工具仍只能通过各自组件扩展点显式启用，并受 trusted-local 与 shell policy 约束。repository 路径仍应只读、受版本管理，避免提示词供应链被篡改。

---

## 8. 常见编排方式

### 8.1 注册自定义工具

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
protected List<Object> tools() {
    return List.of(new OrderTool());
}
```

#### Spring / Solon 环境下获取带依赖注入的工具实例

上面的示例用 `new OrderTool()` 直接构造，适用于工具无外部依赖的简单场景。当工具类需要 Spring bean 注入（如数据库客户端、远程服务）时，应将工具类注册为容器 bean，再在组件中注入使用：

```java
// 1. 工具类标 @Component，依赖由容器注入
@Component
public class OrderTool {

    @Resource
    private OrderService orderService;  // ← Spring DI

    @Tool(name = "query_order_status", description = "Query order status by order number")
    public String query(@ToolParam(name = "orderNo") String orderNo) {
        return orderService.queryStatus(orderNo);
    }
}

// 2. Agent 组件通过 @Resource 注入工具 bean
@Component("orderAgent")
public class OrderAgentCmp extends ReActAgentComponent {

    @Resource
    private OrderTool orderTool;  // ← 容器注入，DI 已生效

    @Override
    protected List<Object> tools() {
        return List.of(orderTool);  // 返回容器管理的实例
    }

    // ... model(), systemPrompt(), userPrompt() 等省略
}
```

**原理**：`ReActAgentComponent` 本身就是 Spring bean，因此它的字段天然由容器管理。通过 `@Resource` / `@Autowired` 注入的 bean 所有依赖都已就绪，直接放进 `tools()` 即可。不要在组件里 `new` 一个需要 DI 的工具类——那样注入不会生效。

Skill 文档不会触发 Java 类反射。Java 工具统一由组件 `tools()` 返回容器已管理的实例，或通过 `customizeToolkit()` 显式注册。

如果工具需要访问当前 `Slot`、workspace 或 conversation 信息，应从工具调用收到的 AgentScope `RuntimeContext` 中读取 `LiteFlowAgentContext`，不要在构造工具时捕获某次 invocation。

### 8.2 用 IF 做路由

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

### 8.3 用 WHEN 并行调用多个模型

```xml
<chain name="parallelChain">
    THEN(
        prepare,
        WHEN(deepseekAgent, dashscopeAgent).maxWaitSeconds(60),
        recordReply
    );
</chain>
```

默认情况下，同一条 chain 内的多个 Agent 共享 `conversationId`；在相同 namespace 与 user 下，它们也共享同一个 `runtimeSessionId`、workspace 与 workspace lease。默认 `agentKey()` 是各自的 `nodeId`，所以 `deepseekAgent` 与 `dashscopeAgent` 的 `agentNamespace`、StateStore key 与 state guard key 不同。两者都不需要 workspace lease 时可以并行；启用本地 workspace/Shell 工具后，共享 workspace lease 会串行化相同会话的相关调用，文件不会因 `agentKey` 不同而隔离。

如果多个组件覆写为相同的稳定 `agentKey()`，它们会竞争同一个 state guard key。每个组件实例只拥有一个 runtime，`agentKey()` 在首次 runtime 构建后必须保持稳定；把 request ID 等单次值拼入 `agentKey` 会在后续调用触发 identity-change 校验失败。文件是否隔离只由 `namespace + userId + conversationId` 决定。

```java
@Override
protected String agentKey() {
    return "stable-deepseek-agent";
}
```

### 8.4 多轮对话

多轮对话的关键是让多次调用使用同一个 `conversationId`，同时需要让同一个 Agent 节点保持同一个 `agentKey`（默认 nodeId 已满足）：

```java
LiteflowResponse first = flowExecutor.execute2Resp(
        "deepseekChain",
        firstRequest,
        ExecuteOption.of().conversationId("chat-user-1-conv-1"));

LiteflowResponse second = flowExecutor.execute2Resp(
        "deepseekChain",
        secondRequest,
        ExecuteOption.of().conversationId("chat-user-1-conv-1"));
```

或者在组件中按业务对象覆写：

```java
@Override
protected String resolveConversationId(Slot slot) {
    ChatRequest req = slot.getChainReqData(slot.getChainId());
    return "chat-" + req.getUserId() + "-" + req.getConversationId();
}
```

只要多次执行解析出的 runtime identity 相同，AgentScope 就会从同一 StateStore session 恢复对话历史。是否能跨 JVM 重启恢复，取决于 `liteflow.agent.state-store.type` 与后端实现。

---

## 9. 完整配置速查

```properties
# LiteFlow 规则
liteflow.rule-source=agent/flow.el.xml

# workspace
liteflow.agent.workspace.backend=guarded-local
liteflow.agent.workspace.trusted-local=false
liteflow.agent.workspace.root=/var/lib/liteflow/agent-workspaces
liteflow.agent.workspace.auto-create=true
liteflow.agent.workspace.max-file-bytes=10485760
liteflow.agent.workspace.max-list-size=1000

# AgentScope 2 runtime 与 StateStore
liteflow.agent.runtime.namespace=my-service
liteflow.agent.runtime.default-user-id=default
liteflow.agent.runtime.timeout=2m
liteflow.agent.state-store.type=memory
liteflow.agent.state-store.json-root=./data/agent-state
liteflow.agent.state-store.failure-policy=fail-fast

# Shell 工具
liteflow.agent.shell.mode=disabled
liteflow.agent.shell.timeout=30s
liteflow.agent.shell.max-output-bytes=1048576

# ReAct 内部事件日志（reason / act / error）
liteflow.agent.logging.react-enabled=true

# ReAct 默认最大迭代次数
liteflow.agent.defaults.max-iterations=50

# Skills
liteflow.agent.skills.enabled=false
liteflow.agent.skills.path=./skills

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

## 10. 安全建议

1. 生产环境默认关闭 Shell：`liteflow.agent.shell.mode=disabled`。
2. `workspace.root` 使用专门目录，不要和业务源码、日志、密钥目录混放。
3. 不要直接把用户输入原样作为 `conversationId` 或 `agentKey`。建议使用业务 ID 拼接、哈希或映射。
4. API Key 使用环境变量、配置中心或密钥管理系统，不要写入代码仓库。
5. 根据业务体量设置 `max-file-bytes`、`max-output-bytes` 与 runtime/tool timeout，避免 Agent 调用消耗不可控。
6. 使用 `state-store.type=BEAN` 时，后端 Bean 由业务应用提供，权限、网络访问和生命周期也应由业务应用控制。
7. 本地文件与 Shell 工具只有在受信部署中才设置 `workspace.trusted-local=true`；该路径保护不是容器沙箱。
8. 开启 Skills 时，`skills.path` 建议使用只读、受版本管理或受发布流程控制的目录；不要让普通用户直接写入 `SKILL.md` 或其中声明的 Java 工具类。
9. Core 明确关闭 AgentScope skill code execution；不要在自定义 Agent builder 中重新启用。Skill repository 仍应使用只读、可信内容源。

---

## 11. 故障排查

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| `liteflow.agent.workspace.root is required` | 未配置 workspace 根目录 | 配置 `liteflow.agent.workspace.root` |
| `cannot create workspace root` | 根目录不可写，或父目录权限不足 | 换到应用可写目录，或提前创建并授权 |
| `workspace root does not exist` | `auto-create=false` 且目录不存在 | 提前创建目录，或开启 `auto-create` |
| `Missing API key: please configure liteflow.agent.openai.api-key` | 对应平台凭据未配置 | 配置对应平台的 `api-key` |
| 多轮对话没有记忆 | 每次调用使用了不同 `conversationId`，或 `agentKey()` 被拼入了一次性值 | 传入稳定的 `ExecuteOption.conversationId(...)`，或覆写 `resolveConversationId()` 返回稳定业务会话 ID；同时保持同一 Agent 的 `agentKey` 稳定 |
| 同一条 chain 中多个 Agent 没有共享文件 | 它们解析到了不同 `conversationId` | 让首个 Agent 写回的 `slot.conversationId` 被后续 Agent 复用，或显式传入同一个 `ExecuteOption.conversationId(...)` |
| 重启后没有历史记忆 | `state-store.type=MEMORY` | 改为 `JSON`，或提供持久化 `AgentStateStore` Bean 并选择 `BEAN` |
| BEAN StateStore 启动失败 | 未配置 Bean 名，或 Bean 未实现 `AgentStateStore` | 检查 `state-store.bean-name` 与容器中的 Bean 类型 |
| Shell 返回 `command 'xxx' not allowed by whitelist` | 白名单模式下命令未放行 | 加入白名单，或继续保持禁用 |
| `path escapes workspace` | 文件工具收到绝对路径或越界路径 | 使用相对路径，并限制在当前 workspace 内 |
| `Base directory does not exist` | 组件把不存在的 `skills.path` 传给 `FileSystemSkillRepository` | 创建并保护 skills 根目录，或让组件不返回该 repository；构造器会直接失败，没有配置驱动的降级开关 |
| 预期 skill 没有出现在 system prompt | repository 未返回该 skill，或 filter 使用了 name 而不是 skill ID | 检查 `AgentSkillRepository#getAllSkills()`，并把 `AgentSkill#getSkillId()` 传给 `SkillFilter` |
| `context.getUsedSkills()` 为空 | 本轮 Agent 没有成功调用 `load_skill_through_path`，或读取时已离开 `process()` 生命周期 | 在 `handleReply(reply, context)` 或 Middleware 回调中读取；确认模型确实加载了对应 skill |
| 没有收到流式事件 | 本次调用没有使用 `ExecuteOption.eventListener(...)`，或链路中没有 ReAct Agent 节点 | 注册 listener；确认事件类型是否为 `agent.reasoning`、`agent.tool_result`、`agent.summary` 或 `agent.result` |
| invocation context 已失效或缺失 | 在构造器、Bean 初始化、异步线程或 `process()` 结束后保存/读取上下文 | 只使用 `userPrompt(context)`、工具 `RuntimeContext`、Middleware 回调与 `handleReply(reply, context)` 的当次参数 |
| 注册 listener 后链路失败 | listener 抛异常且 `listener-failure-mode=FAIL_FAST`，或执行阻塞 I/O 导致上游超时 | listener 内只做轻量处理；需要容忍投递失败时评估并配置 `LOG_AND_CONTINUE` |
| `WHEN` 中多个 Agent 看起来没有并行 | state guard key 相同、多个工具型 Agent 共享 workspace lease，或下游等待最慢分支 | 保持各组件 `agentKey` 稳定且不同；需要文件隔离时使用不同 `namespace`、`userId` 或 `conversationId`，仅改变 `agentKey` 无效 |
| `context.getChatUsage()` 返回 `null` | 本轮还没发生 model call，或模型/网关未上报 `ChatUsage` | 改到 `handleReply(reply, context)` 再读；确认模型响应携带 usage |
| `context.getChatUsage()` 的累计 token 比 SDK 单次响应大 | 同一次 `process()` 内 ReAct 触发了多次 model call，`ChatUsageMiddleware` 会逐次累加 | 这是预期行为；单步 usage 可在自定义 Middleware 的 model-call 事件中读取 |

---

## 12. 参考位置

- Core 模块：`liteflow-react-agent/liteflow-react-agent-core/`
- OpenAI 模块：`liteflow-react-agent/liteflow-react-agent-openai/`
- Anthropic 模块：`liteflow-react-agent/liteflow-react-agent-anthropic/`
- Gemini 模块：`liteflow-react-agent/liteflow-react-agent-gemini/`
- DashScope 模块：`liteflow-react-agent/liteflow-react-agent-dashscope/`
- Skill 入口：`liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/`
- Skill 配置：`liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/SkillsConfig.java`
- 示例测试：`liteflow-testcase-el/liteflow-testcase-el-react-agent/`
- 规则示例：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/flow.el.xml`
- Skill 示例：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/`
