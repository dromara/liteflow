# LiteFlow ReAct Agent 使用指南

本文介绍如何在 LiteFlow 中使用 `liteflow-react-agent`，把 agentscope-java 的 `ReActAgent` 当作普通 LiteFlow 节点编排到 EL 链路中。

读完本文后，你应该能够：

- 引入对应模型平台模块，并完成 `liteflow.agent.*` 配置；
- 编写一个继承 `ReActAgentComponent` 的 Agent 组件；
- 在 EL 中组合 Agent、普通节点、条件路由和并行节点；
- 通过 `ExecuteOption.eventListener(...)` 接收 Agent 执行中的流式事件；
- 正确理解 conversation、agentKey、memory、workspace、内置文件工具、Shell 工具和 Skills 的边界。

> 当前仓库根版本：`2.16.0`。
>
> 当前源码中 `liteflow-react-agent` 聚合模块的 `maven.compiler.source` / `target` 为 `17`，根 `compile-17+` profile 会在 JDK 17 及以上激活该模块。实际运行时还需要满足 agentscope-java 及具体模型 SDK 的运行要求。

---

## 1. 模块说明

`liteflow-react-agent` 是一个聚合模块，核心能力在 `liteflow-react-agent-core`，不同模型供应商由独立子模块提供便捷入口。

| 模块 | 作用 |
| --- | --- |
| `liteflow-react-agent-core` | `ReActAgentComponent`、`ModelSpec` 基础设施、conversation / agentKey 会话管理、memory 持久化、流式事件桥接、workspace 文件工具、受管 Shell 工具 |
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

最小配置需要包含规则文件、workspace 根目录和模型凭据。生产环境建议先关闭 Shell 工具，再按需开启。

```properties
liteflow.rule-source=agent/flow.el.xml

liteflow.agent.workspace.root=/var/lib/liteflow/agent-workspaces
liteflow.agent.shell.mode=disabled

liteflow.agent.openai-compatible.deepseek.api-key=${DEEPSEEK_API_KEY}
# DeepSeek.of(...) 已内置默认 baseUrl；如需覆盖再配置下一行。
liteflow.agent.openai-compatible.deepseek.base-url=https://api.deepseek.com/v1
```

`liteflow.agent.workspace.root` 是必填项。没有配置时，首次执行 Agent 组件会抛出：

```text
AgentConfigException: liteflow.agent.workspace.root is required
```

**路径形式**：`workspace.root` 同时支持绝对路径与相对路径。框架内部通过 `Paths.get(root).toAbsolutePath().normalize()` 解析，相对路径会基于 JVM 启动时的 `user.dir`（当前工作目录）拼成绝对路径。由于 `user.dir` 在 IDE 启动、`java -jar`、systemd、容器等不同场景下差异巨大，**生产环境一律建议使用绝对路径**（如示例中的 `/var/lib/liteflow/agent-workspaces`），避免同一份配置在不同部署方式下落盘到不同位置，进而影响 session 持久化、conversation workspace 复用等行为。

### 2.3 编写 Agent 组件

Agent 组件继承 `ReActAgentComponent`，至少实现三个无参方法：

- `model()`：返回一个 `ModelSpec<?>`，声明使用哪个平台、哪个模型及可选高级参数；
- `systemPrompt()`：创建 Agent 时使用的系统提示词（框架会在它前面自动拼接一段统一系统提示词，详见 [§ 3](#3-reactagentcomponent-扩展点)）；
- `userPrompt()`：每次调用时发送给 Agent 的用户消息。

当前源码已把执行上下文改为通过 `ctx()` 动态获取，而不是把 `ReActAgentContext` 作为参数传入各个 hook。`ctx()` 只能在 `process()` 生命周期内调用，包括 `systemPrompt()`、`userPrompt()`、自定义工具回调、Hook 回调和 `handleReply()`。

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
    protected String userPrompt() {
        Object req = getSlot().getChainReqData(getSlot().getChainId());
        return req == null ? "" : req.toString();
    }

    @Override
    protected boolean enableShellTool() { return false; }

    @Override
    protected boolean enableWorkspaceFileTools() { return false; }
}
```

如果自定义工具、Hook 或 Model 会被缓存并跨多次 invocation 复用，不要在这些对象中保存某次调用的 `ReActAgentContext` 引用；应保存组件实例，并在运行时通过组件内部类或组件暴露的公开方法间接调用受保护的 `ctx()`，获取当次上下文。

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

默认情况下，Agent 回复会通过 `reply.getTextContent()` 写入 `slot.responseData`。如果后续节点希望从指定位置读取回复，可以覆写 `handleReply(reply)`，或者像测试用例中的 `RecordReplyCmp` 一样把 `responseData` 转存到节点输出。

### 2.5 下游节点如何拿到 Agent 的执行结果

ReAct Agent 节点执行完后，结果传递给下一个节点有两种方式。

**方式 1：默认走 `slot.responseData`（最简单）**

`ReActAgentComponent#handleReply()` 默认实现：

```java
protected void handleReply(Msg reply) {
    ctx().getSlot().setResponseData(reply == null ? null : reply.getTextContent());
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
protected void handleReply(Msg reply) {
    String text = reply == null ? null : reply.getTextContent();
    // 选择 1：写入自定义 ContextBean
    ctx().getSlot().getContextBean(MyAgentCtx.class).setReply(getNodeId(), text);
    // 选择 2：以 nodeId 为 key 存到 slot 输出，避免相互覆盖
    ctx().getSlot().setOutput(getNodeId(), text);
    // 选择 3：把本轮累计 token 用量一起落盘，供下游节点或调用方读取
    ChatUsage usage = ctx().getChatUsage();
    if (usage != null) {
        ctx().getSlot().setOutput(getNodeId() + ".usage", Map.of(
                "inputTokens", usage.getInputTokens(),
                "outputTokens", usage.getOutputTokens(),
                "totalTokens", usage.getTotalTokens(),
                "timeSeconds", usage.getTime()
        ));
    }
}
```

下游节点对应使用 `slot.getContextBean(MyAgentCtx.class)` 或 `slot.getOutput(nodeId)` 读取。`ChatUsage` 来自 `io.agentscope.core.model.ChatUsage`；`ctx().getChatUsage()` 的语义与边界见 [§ 3](#3-reactagentcomponent-扩展点) 中 `ReActAgentContext` 表格说明。

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
| `agent.result` | 本轮 Agent 最终结果 | 与最终 `handleReply(reply)` 使用的消息一致 |

`FlowEvent` 会携带 `chainId`、`nodeId`、`requestId`、`conversationId`、`text`、`last`、`timestamp` 和原始 `data`。其中 `nodeId` 对多 Agent 链路很重要：`WHEN(agentA, agentB)` 并发执行时，多个 Agent 的流式事件可能交错到达，调用方应按 `nodeId`、`conversationId` 或业务自定义字段分组展示。

没有注册 `eventListener` 时，`ReActAgentComponent` 会继续走原来的阻塞调用路径，不会产生额外事件开销；注册 listener 后，组件会使用 AgentScope 的 `agent.stream(...)` 执行，并在流结束后照常调用 `handleReply(reply)`、保存 memory、返回 `LiteflowResponse`。

`eventListener` 在 chain 执行线程中同步回调。生产环境转发到 SSE、WebSocket 或消息队列时，建议在 listener 中只做轻量入队或缓冲，不要执行耗时 I/O；listener 抛出的异常会向上传播，并可能导致本次链路失败。

---

## 3. ReActAgentComponent 扩展点

`ReActAgentComponent#process()` 是 `final`。框架在其中统一完成配置读取、conversation 解析、`agentKey` 解析、Session 获取、加锁、Agent 懒构建、调用、回复处理和 memory 保存。业务侧通过覆写受保护方法定制行为。

| 方法 | 是否必须 | 默认行为 | 说明 |
| --- | --- | --- | --- |
| `model()` | 是 | 无 | 返回 `ModelSpec<?>`，由框架从 `AgentConfig` 解析凭据并构造 agentscope `Model` |
| `systemPrompt()` | 是 | 无 | 返回系统提示词，同一 `(conversationId, agentKey)` 首次构建 Agent 时调用 |
| `userPrompt()` | 是 | 无 | 返回本轮用户消息，每次 `process()` 都调用 |
| `tools()` | 否 | 空列表 | 注册自定义 `@Tool` 对象 |
| `skills()` | 否 | 空列表 | 返回本 Agent 可使用的技能名白名单；空列表表示允许使用配置目录中的全部技能 |
| `enableSkills()` | 否 | 读取 `liteflow.agent.skills.enabled`（默认 `false`） | 是否为本组件启用 AgentScope skills；全局关闭时可保持既有 Agent 行为不变 |
| `usedSkills()` | 否 | 本轮已成功加载技能名列表 | 只能在 `process()` 触发的生命周期内读取，例如 `userPrompt()`、工具回调、Hook 回调和 `handleReply()` |
| `resolveConversationId()` | 否 | 先复用 `slot.conversationId`，再读 `chainReqData` Map 中的 `conversationId`，最后调用 `ConversationIdGenerator.generate()` | 决定本次调用所属业务会话；同一条 chain 内首个 Agent 写回 slot 后，后续 Agent 默认复用 |
| `agentKey()` | 否 | 当前 `nodeId`，为空时为 `default` | 在同一 conversation 中区分不同 Agent 实例和记忆；默认不同节点互相隔离 |
| `maxIterations()` | 否 | `-1` | 返回正数时覆盖全局 `defaults.max-iterations` |
| `enableShellTool()` | 否 | `true` | 是否注册内置受管 Shell 工具；与 `shell.mode` 取**逻辑与**——组件返回 `true` 且配置不为 `DISABLED` 时才注册，任一方关闭即不注册 |
| `enableWorkspaceFileTools()` | 否 | `true` | 是否注册内置 workspace 文件工具 |
| `hooks()` | 否 | 空列表 | 注册 agentscope `Hook` |
| `enableReActLogging()` | 否 | 读 `liteflow.agent.logging.react-enabled`（默认 `true`） | 是否注册内置 `ReActLoggingHook`，将 reason / act / error 事件写到日志 |
| `handleReply(reply)` | 否 | 写入 `slot.responseData` | 自定义回复处理逻辑 |
| `buildModel()` | 否 | 委派 `model().resolve(agentConfig())` | 逃生舱：完全自行构造 agentscope `Model` |

`ReActAgentContext` 可通过组件的 `ctx()` 方法取得，提供以下执行上下文：

| 方法 | 说明 |
| --- | --- |
| `getSlot()` | 当前 LiteFlow `Slot` |
| `getConversationId()` | 安全化后的 conversation ID，决定 workspace 子目录 |
| `getAgentKey()` | 安全化后的 Agent key，默认来自 `nodeId` |
| `getWorkspaceDir()` | 当前 conversation 对应的 workspace 目录 |
| `getChatUsage()` | 本次 `process()` 截至当前已累计的 token 用量（agentscope `ChatUsage`，含 `getInputTokens()` / `getOutputTokens()` / `getTotalTokens()` / `getTime()`（秒））；模型未上报或本轮尚未发生过 reasoning step 时返回 `null` |

注意：`systemPrompt()` 只在同一 `(conversationId, agentKey)` 下首次构建 Agent 时调用；后续调用会复用同一个 Agent 实例和 memory。动态输入应放在 `userPrompt()` 中，并通过 `ctx()` 或 `getSlot()` 读取当次 invocation 的数据。

**框架统一系统提示词**：你在 `systemPrompt()` 中返回的内容不是最终系统提示词。框架在 `effectiveSystemPrompt()` 中会**始终在你的提示词前面拼接**一段内置的 `DEFAULT_SYSTEM_PROMPT`，最终下发给底层 ReActAgent 的是 `DEFAULT_SYSTEM_PROMPT + "\n\n" + 你的 systemPrompt()`。这段默认提示词的内容大致是：

```text
请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
```

由此带来的几个行为，做提示词工程时需要心里有数：

- 模型默认会**用用户提问所用的语言**回答；如需固定输出语言，应在自己的 `systemPrompt()` 里显式覆盖；
- 模型在每次调用工具前会**先输出一两句推理摘要**——这正是 `ReActLoggingHook` 日志和流式 `agent.reasoning` 事件里出现简短中文/原语言推理片段的来源，属于预期行为，而非你的 `systemPrompt()` "没生效"；
- 当 `systemPrompt()` 返回空字符串或空白时，框架会**单独使用**这段默认提示词。

`getChatUsage()` 的累计口径：底层 agentscope 每次 `reasoning(iter)` 都会新建一个 `ReasoningContext`，所以 `Msg#getChatUsage()` 反映的是**当前这一步** LLM 调用的累计（流式聚合），而不是跨多步 ReAct 循环的累计。框架默认始终注册一个内部 `ChatUsageTrackingHook`，在每次 `PostReasoningEvent` 时把该步 usage 累加到 Session 缓存的计数器，并在每次 `process()` 开始前清零，因此 `ctx().getChatUsage()` 给出的是**整次 `process()` 调用**累计后的值。该 hook 无配置开关，业务无需启用；如果完全不希望产生这部分计数开销，可通过覆写 `buildModel()` 走完全自定义路径绕开 `ReActAgentComponent` 的默认构建流程。

同理，`tools()`、`hooks()`、`skills()`、`enableSkills()`、`enableReActLogging()` 和 `buildModel()` 都属于 Agent 构建期能力声明，通常只在同一 `(conversationId, agentKey)` 首次构建缓存 Agent 时生效。不要让这些方法依赖单次请求数据；如果确实需要按请求隔离模型、工具或 hook，请把请求维度体现在 `agentKey()` 或 `conversationId` 中，让框架构建新的 AgentSession。

**组件方法与 application 配置的优先级**

`ReActAgentComponent` 的部分受保护方法与 `liteflow.agent.*` 配置项控制的是同一件事。它们冲突时的合并规则并不统一，分三种：

| 重叠项 | 组件方法 | 对应配置 | 合并规则 |
| --- | --- | --- | --- |
| ReAct 日志 | `enableReActLogging()` | `liteflow.agent.logging.react-enabled` | 方法默认实现即读该配置；**一旦覆写，返回值取代配置** |
| Skills 开关 | `enableSkills()` | `liteflow.agent.skills.enabled` | 同上：默认读配置，覆写后以组件为准（可在全局开启时单独关闭某组件） |
| 最大迭代 | `maxIterations()` | `liteflow.agent.defaults.max-iterations` | 方法**返回正数时覆盖**配置；返回默认 `-1`（或非正数）时用配置 |
| Shell 工具 | `enableShellTool()` | `liteflow.agent.shell.mode` | 两者取**逻辑与**：组件返回 `true` 且配置不为 `DISABLED` 才注册，任一方关闭即不注册；`shell.mode=DISABLED` 是组件无法突破的安全底线 |
| workspace 文件工具 | `enableWorkspaceFileTools()` | （无全局开关） | 纯组件级，默认 `true` |

可以记成两句话：**能在组件里覆写的开关，覆写后基本以组件为准**（`enableReActLogging` / `enableSkills` / `maxIterations`）；**唯独 Shell 例外——它与 `shell.mode` 取最严格的一方，`shell.mode=disabled` 是组件破不了的安全底线**。

其余 `liteflow.agent.*` 配置（如 `workspace.root`、`session.*`、`workspace.max-file-bytes` 等）没有对应的组件方法，只能通过配置设置；而 `model()` / `buildModel()` 与凭据配置是分工而非重叠——组件声明“用哪个模型 + 参数”，凭据（`api-key` / `base-url`）始终从 `liteflow.agent.<platform>.*` 读取。

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

### 5.1 两层标识分别负责什么

当前源码不是单一 `sessionId` 模型，而是把会话拆成两层：

- `conversationId`：业务 / 对话维度，整条 chain 内一致，决定 workspace 子目录；
- `agentKey`：组件维度，默认是 `nodeId`，用于在同一段 conversation 中区分不同 Agent 的 `ReActAgent` 实例和记忆。

每次执行 Agent 组件时，框架会通过 `AgentSessionManager.acquire(conversationId, agentKey)` 获取一个 `AgentSession`。同一个 `(conversationId, agentKey)` 会复用：

- 同一个 `ReActAgent` 实例；
- 同一个 Agent memory；
- 同一把 `ReentrantLock`；
- 同一个持久化 key，格式为安全化后的 `conversationId + "__" + agentKey`。

同一个 `conversationId` 下的不同 `agentKey` 默认拥有独立 Agent 和独立 memory，但共享同一个 workspace 子目录，便于多个 Agent 通过文件协作。

因此，同一个 `(conversationId, agentKey)` 下的调用会串行执行，避免多线程同时修改同一份 memory。不同 `agentKey` 可以并行执行，但如果共享 workspace，需要由业务自行避免写同名文件造成冲突。

### 5.2 conversationId 从哪里来

默认实现是：

```java
protected String resolveConversationId() {
    Slot slot = getSlot();
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
protected String resolveConversationId() {
    ChatRequest req = getSlot().getChainReqData(getSlot().getChainId());
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

### 5.4 JVM 热 Session 配置

`liteflow.agent.session.*` 控制当前 JVM 中热 Session 的缓存时间和数量：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `idle-timeout` | `30m` | Session 空闲多久后可被清理 |
| `cleanup-interval` | `1m` | 后台清理线程检查间隔，最低 20ms |
| `max-sessions` | `10000` | 当前 JVM 中最多保留多少个热 Session，按 `(conversationId, agentKey)` 计数 |

清理线程名为 `liteflow-agent-session-cleaner`。它只会清理已经超过 `idle-timeout` 且当前没有被加锁的 Session。

当 Session 数量超过 `max-sessions` 时，会按 `lastActive` 淘汰最旧的 JVM 内缓存。这个淘汰只移除热 Agent 实例，不删除 workspace，也不删除 Redis、MySQL 或文件中的持久化记忆。

注意：`AgentSessionManager` 在首次执行 Agent 组件时按当前 `liteflow.agent.*` 配置懒创建，已有热 Session 中的 Agent 也会被复用。运行中修改 memory、skills、模型、工具或 hook 配置后，已有 `(conversationId, agentKey)` 不一定立即体现新配置；通常需要使用新的 conversation / agentKey，或重启应用。

### 5.5 memory 持久化模式

`liteflow.agent.session.memory.*` 控制 Agent memory 保存在哪里。它和热 Session 缓存是两件事：热缓存决定当前 JVM 里保留多久，memory 持久化决定重启或重新加载后能否恢复对话历史。

| 模式 | 含义 | 适用场景 |
| --- | --- | --- |
| `JVM` | 默认值，使用 AgentScope 的 JVM 内 Session；进程退出后丢失 | 单进程内多轮对话 |
| `NONE` | 不加载也不保存持久化 Session；热缓存中的同一 `(conversationId, agentKey)` 仍会复用到过期或淘汰 | 不需要持久化；如需严格无状态，配合每次唯一的 conversationId 或 agentKey 使用 |
| `LOCAL_FILE` | 保存到 `workspace.root/.agent-session/` 下，由 AgentScope 文件 Session 管理 | 本地文件持久化、开发测试、小规模部署 |
| `REDIS` | 使用用户提供的 Redis 客户端 Bean | 多实例共享短期会话 |
| `MYSQL` | 使用用户提供的 `DataSource` Bean | 需要数据库持久化和运维治理 |

通用开关：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `load-on-first-use` | `true` | 当前 JVM 首次构建某个 Session 的 Agent 时，尝试恢复历史 |
| `save-after-call` | `true` | 调用成功后保存记忆 |
| `save-on-error` | `true` | 调用抛错时也尝试保存记忆 |

`LOCAL_FILE` 示例：

```properties
liteflow.agent.session.memory.mode=LOCAL_FILE
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

### 5.6 自定义持久化后端（SPI）

除内置的五种 `memory.mode` 外，框架还通过 `AgentSessionFactory` SPI 开放了持久化后端扩展点。如果要接入 PostgreSQL、对象存储、加密 JSON 等其它后端，可以实现该接口并在 `META-INF/services/com.yomahub.liteflow.agent.session.factory.AgentSessionFactory` 中注册：

```java
public class EncryptedFileSessionFactory implements AgentSessionFactory {

    @Override
    public MemoryStorageMode mode() {
        // 绑定到某个已有模式枚举；与内置工厂同模式时由本实现覆盖内置实现
        return MemoryStorageMode.LOCAL_FILE;
    }

    @Override
    public Session create(AgentConfig agentConfig) {
        // 返回 AgentScope Session；返回 null 表示本模式跳过持久化
        return new MyEncryptedJsonSession(agentConfig);
    }
}
```

规则与边界：

- `AgentSessionFactoryRegistry` 先注册全部内置工厂，再加载 SPI 工厂；同一个 `mode()` 冲突时 **SPI 工厂优先**，因此自定义实现的典型用途是**覆盖某个内置模式的 Session 构建逻辑**（例如把 `LOCAL_FILE` 换成加密落盘）；
- `mode()` 返回的是固定的 `MemoryStorageMode` 枚举，SPI **不能新增模式名**，只能复用并覆盖已有的五种之一；
- `create(...)` 在首次执行 Agent 组件时**懒调用**，而不是框架启动时；
- 返回 `null` 表示该模式不做持久化（内置 `NONE` 模式即如此）。

---

## 6. Workspace 与内置工具

### 6.1 Workspace 目录结构

每个 conversation 都会在 `liteflow.agent.workspace.root` 下获得一个独立子目录：

```text
/var/lib/liteflow/agent-workspaces/
├── chat-user-1-conv-1/
├── chat-user-1-conv-2/
└── .agent-session/
```

`<conversationId>/` 是同一段 conversation 中多个 Agent 共享的工具读写目录；`.agent-session/` 只在 `session.memory.mode=LOCAL_FILE` 时创建，用于保存 AgentScope 的记忆文件。

### 6.2 Workspace 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `root` | 无 | 必填，workspace 根目录 |
| `auto-create` | `true` | 根目录不存在时是否自动创建 |
| `cleanup-on-session-expire` | `true` | 空闲 Session 过期清理时，若该 conversation 下没有其他存活 AgentSession，是否删除对应 workspace 子目录 |
| `cleanup-on-jvm-shutdown` | `false` | `AgentSessionManager.close()` 时是否删除当前存活 Session 对应的 workspace |
| `max-file-bytes` | `10485760` | `read_file` 单次最多读取的字节数；超出时截断读取 |
| `max-list-size` | `1000` | `list_files` 单次最多返回的条目数 |

如果 Agent 生成的文件需要长期保留，例如报告、草稿、审计材料，应把 `cleanup-on-session-expire` 设为 `false`，并由业务侧做归档。

### 6.3 Workspace 文件工具

`enableWorkspaceFileTools()` 默认为 `true`。开启后，框架会注册 `WorkspaceFileTools`：

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

`enableShellTool()` 默认为 `true`。如果配置中的 `liteflow.agent.shell.mode` 不是 `DISABLED`，框架会注册 `ManagedShellCommandTool`，工具名为 `execute_shell_command`。

Shell 工具在当前 conversation workspace 下执行命令。当前实现会按空白符切分命令字符串，用 `ProcessBuilder` 直接执行 token 列表，并通过首 token 做白名单或黑名单判断；不会通过系统 shell 解释管道、重定向、变量展开等语法。配置项如下：

| 配置项 | 默认值 |
| --- | --- |
| `mode` | `WHITELIST` |
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

框架内置 `ReActLoggingHook`，把 agent 的 `reason` / `act` / `error` 事件写入 SLF4J，便于在终端直接观察 ReAct 推理过程。日志中的方括号 ID 为 `conversationId:agentKey`。

| 事件 | 日志格式 |
| --- | --- |
| `PreReasoningEvent` | `[agent:reason][conversationId:agentKey] >>> model=... messages=N` |
| `PostReasoningEvent` | `[agent:reason][conversationId:agentKey] <<< text=... toolCalls=[...]` |
| `PostReasoningEvent`（usage 附加行） | `[agent:reason][conversationId:agentKey] <<< usage input=N output=N total=N time=Ns`（仅当本步消息上报了 `ChatUsage` 时输出） |
| `PreActingEvent` | `[agent:act][conversationId:agentKey] >>> tool=... input=...` |
| `PostActingEvent` | `[agent:act][conversationId:agentKey] <<< tool=... result=...` |
| `ErrorEvent` | `[agent:error][conversationId:agentKey] ...` |

`PostReasoningEvent` 的 usage 行直接来自当前这一步 reasoning message 的 `ChatUsage`（agentscope 单步累计 / 流式聚合的结果），不是跨步累计；想要看整次 `process()` 调用的总 token，请使用 `ctx().getChatUsage()`（见 [§ 3](#3-reactagentcomponent-扩展点)）。

- 全局开关：`liteflow.agent.logging.react-enabled`（默认 `true`）。
- 单组件开关：覆写 `enableReActLogging()` 强制返回 `true` / `false`。
- 输出 logger 名：`com.yomahub.liteflow.agent.hook.ReActLoggingHook`（可在 logback / log4j2 中独立调级）。
- 文本字段超过 500 字会被截断为 `...(truncated)`。

如果业务侧已经通过自定义 `Hook` 处理事件流，建议关掉内置日志钩子以避免重复输出。

---

## 7. Skills 支持

`liteflow-react-agent` 支持加载 AgentScope 的 filesystem skills。Skill 适合承载“什么时候使用”“如何执行”的长指令，也可以把某些 Java `@Tool` 只绑定到指定 skill 上，避免所有工具都全局暴露给 Agent。

### 7.1 开启 Skills

Skills 默认关闭。开启后，框架会从 `liteflow.agent.skills.path` 指向的目录扫描技能：

```properties
liteflow.agent.skills.enabled=true
liteflow.agent.skills.path=./skills
liteflow.agent.skills.strict=true
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 是否启用配置驱动的 skills 支持；关闭时不会注册 `load_skill_through_path` 工具 |
| `path` | `./skills` | skills 根目录，目录下每个子目录表示一个 skill |
| `strict` | `true` | 严格模式。目录缺失、声明的 skill 不存在、`tools` 类加载失败等问题会快速失败；设为 `false` 时记录 warn 并尽量继续 |

**路径形式**：`skills.path` 同时支持绝对路径与相对路径，与 [`workspace.root`](#22-配置-liteflow-与-agent) 同样基于 JVM `user.dir` 解析相对路径。默认值 `./skills` 只是开发与本地 demo 的便利值，**生产环境建议改成绝对路径**（如 `/opt/liteflow/skills`），既能避免不同启动方式定位到不同目录，又便于配合只读挂载、版本化发布等运维约束。若开启 skills 但目录不存在：`strict=true` 抛出 `Skills root not found`；`strict=false` 仅记录 warn 后跳过 skills 加载。

配置只是把可选技能放入 Agent 的 `SkillBox`。某个技能是否在本轮真正被使用，取决于 AgentScope ReAct 运行过程中是否调用 `load_skill_through_path` 加载该技能。

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

`name` 是组件 `skills()` 过滤时使用的技能名，也会出现在 `usedSkills()` 返回值中。建议让目录名与 `name` 保持一致，便于排查。

### 7.3 组件级技能过滤

如果某个 Agent 只能在指定技能内选择，覆写 `skills()` 即可：

```java
@Override
protected List<String> skills() {
    return List.of("demo", "tool-skill");
}
```

语义如下：

- `skills()` 返回空列表：允许使用 `skills.path` 下的全部技能；
- `skills()` 返回非空列表：只把这些技能放入本 Agent 的 `SkillBox`；
- 严格模式下，白名单中有不存在的技能会抛出 `AgentConfigException`；非严格模式下会记录 warn，并加载能找到的技能；
- 不需要额外的 `dependentSkills()`。`skills()` 本身就是本 Agent 的技能选择范围。

也可以让某个组件在全局开启时仍然禁用 skills：

```java
@Override
protected boolean enableSkills() {
    return false;
}
```

注意：`skills()` 与 `enableSkills()` 只在某个 `(conversationId, agentKey)` 首次构建并缓存 `ReActAgent` 时求值。它们表示组件能力声明，不应依赖单次请求数据；同一 Session 后续复用缓存 Agent 时不会重新读取这些声明。

### 7.4 Skill 专属 Java 工具

如果某个 Java 工具只应随指定 skill 可用，可以在 `SKILL.md` frontmatter 中声明 `tools`：

```markdown
---
name: tool-skill
description: Skill that binds a Java tool
tools: com.example.agent.tool.SkillEchoTool
---

# Tool Skill

Use this skill when a Java tool should be available after loading the skill.
```

多个工具类可以用逗号分隔：

```markdown
tools: com.example.agent.tool.SkillEchoTool, com.example.agent.tool.OrderTool
```

工具类要求：

- 必须在应用 classpath 上；
- **推荐注册为框架容器（Spring / Solon）的 bean**：`SkillToolResolver` 会优先按类型从容器获取实例，从而让依赖注入生效；只有当容器中不存在该类型、容器尚未就绪、或获取时抛异常时，才**降级为反射调用无参构造器**实例化（此时依赖注入不可用，并会打印一条降级日志）。因此工具类要么是可被容器管理的 bean，要么至少提供一个公有无参构造器作为兜底；
- 工具方法仍按 agentscope-java 的 `@Tool` / `@ToolParam` 方式声明；
- LiteFlow 会通过 `SkillBox.registration().skill(skill).tool(tool).apply()` 把工具绑定到该 skill，不会把它作为全局 `tools()` 工具注册。

`tools` 字段直接取自 AgentScope 已用 SnakeYAML 解析好的 skill frontmatter（`AgentSkill#getMetadataValue("tools")`），因此除了上面的标量、逗号分隔写法外，也**原生支持 `tools: [a, b]` 这类 YAML 行内数组写法**：

```markdown
tools: [com.example.agent.tool.SkillEchoTool, com.example.agent.tool.OrderTool]
```

### 7.5 记录本轮使用的技能

`usedSkills()` 返回当前 invocation 中已经成功加载过的技能名列表。典型用法是在 `handleReply()` 中把回复和技能使用情况一起写到下游可读的位置：

```java
@Override
protected void handleReply(Msg reply) {
    ctx().getSlot().setOutput(getNodeId(), Map.of(
            "reply", reply == null ? "" : reply.getTextContent(),
            "skillsUsed", usedSkills()
    ));
}
```

使用边界：

- 每次 `process()` 开始前会清空上一轮记录；
- 只有 `load_skill_through_path` 成功加载的技能会被记录；
- 可在 `process()` 触发的调用链内读取，例如 `userPrompt()`、工具回调、Hook 回调和 `handleReply()`；
- `process()` 最终清理后，后续 LiteFlow 生命周期回调不应再依赖 `usedSkills()`。

### 7.6 当前边界

当前实现接入了 AgentScope 的 skill repository、`SkillBox` 和 skill-loading 工具。`SkillBox` 由 `SkillBoxFactory` 构建：当存在 conversation workspace 目录时（正常执行下一定存在），框架会调用 `skillBox.codeExecution().workDir(<当前 conversation workspace>).enable()`，即**在该 workspace 目录内启用 AgentScope 的 skill 代码执行能力**。

需要特别注意这条安全边界：一旦为某个组件开启 skills（`enableSkills()` 为 `true`），就等于在其 conversation workspace 内启用了 AgentScope 的 skill 代码执行。它与 LiteFlow 自带的 `WorkspaceFileTools`、`ManagedShellCommandTool` 是**并存的两条执行路径**——后两者仍照常注册，并继续受 `liteflow.agent.workspace.*` 与 `liteflow.agent.shell.*` 配置控制；而 AgentScope 的 skill 代码执行走的是 AgentScope 自身路径，不经过 `ManagedShellCommandTool`，因此**不受 `liteflow.agent.shell.*` 的白名单 / 黑名单约束**。即使把 `liteflow.agent.shell.mode` 设为 `disabled`，开启 skills 后这条代码执行路径依然存在。生产环境开启 skills 前应一并评估该路径的安全影响，并严格控制 `skills.path` 下的内容来源（见 [§ 10](#10-安全建议)）。

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

> **与 Skill 层 `tools` 的区别**：Skill frontmatter 中的 `tools` 字段（见 [§ 7.4](#74-skill-专属-java-工具)）写的是类名，框架的 `SkillToolResolver` 会自动按类型从容器查找 bean（DI ✅），找不到时降级为反射实例化（DI ❌）。而组件层 `tools()` 返回的是对象实例，获取实例的工作由开发者完成。

如果工具需要访问当前 `Slot`、workspace 或 conversation 信息，不要在构造工具时捕获 `ctx()` 的返回值；可以把工具写成组件内部类，或由组件提供一个公开代理方法，在工具方法执行时再间接调用受保护的 `ctx()`。

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

默认情况下，同一条 chain 内的多个 Agent 会共享同一个 `conversationId`，但因为默认 `agentKey()` 是各自的 `nodeId`，所以 `deepseekAgent` 与 `dashscopeAgent` 会使用不同的 `AgentSession`、不同的锁和不同的 memory，可以并行执行；它们共享同一个 workspace 子目录。

如果多个 Agent 覆写为相同的 `agentKey()`，它们会因为同一把 Session 锁而串行执行。要真正隔离并行，请确保 `(conversationId, agentKey)` 组合不同；如需文件层面也隔离，则同时让它们使用不同的 `conversationId` 或在共享 workspace 内写入不同子目录。

```java
@Override
protected String agentKey() {
    return getNodeId() + "-" + getSlot().getRequestId();
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
protected String resolveConversationId() {
    ChatRequest req = getSlot().getChainReqData(getSlot().getChainId());
    return "chat-" + req.getUserId() + "-" + req.getConversationId();
}
```

只要多次执行解析出的 `(conversationId, agentKey)` 相同，Agent 就会复用同一份 memory。是否能跨 JVM 重启恢复，取决于 `liteflow.agent.session.memory.mode`。

---

## 9. 完整配置速查

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
liteflow.agent.defaults.max-iterations=50

# Skills
liteflow.agent.skills.enabled=false
liteflow.agent.skills.path=./skills
liteflow.agent.skills.strict=true

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
5. 根据业务体量设置 `max-file-bytes`、`max-output-bytes`、`timeout` 和 `max-sessions`，避免 Agent 调用消耗不可控。
6. 选择 `REDIS` 或 `MYSQL` 记忆模式时，连接 Bean 由业务应用提供，权限和网络访问也应由业务应用控制。
7. 多个 Agent 共享同一个 conversation workspace 时，建议约定各自的文件名前缀或子目录，避免并行写冲突。
8. 开启 Skills 时，`skills.path` 建议使用只读、受版本管理或受发布流程控制的目录；不要让普通用户直接写入 `SKILL.md` 或其中声明的 Java 工具类。
9. 注意开启 Skills 会在 conversation workspace 内启用 AgentScope 的 skill 代码执行路径（见 [§ 7.6](#76-当前边界)）。该路径独立于 `liteflow.agent.shell.*`，即使 Shell 工具已 `disabled` 也不受其白名单约束。对安全敏感的部署，应在开启 skills 前评估这条执行路径，并严格管控技能目录与其引用工具类的来源。

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
| 重启后没有历史记忆 | `session.memory.mode=JVM` 或 `NONE` | 改为 `LOCAL_FILE`、`REDIS` 或 `MYSQL` |
| Redis 模式启动失败 | 未配置 Bean 名，或 Bean 类型与 `client-type` 不匹配 | 检查 `bean-name`、`client-type` 和 classpath 依赖 |
| MySQL 模式启动失败 | 未配置 `DataSource` Bean，或 Bean 类型错误 | 配置正确的 `data-source-bean-name` |
| Shell 返回 `command 'xxx' not allowed by whitelist` | 白名单模式下命令未放行 | 加入白名单，或继续保持禁用 |
| `path escapes workspace` | 文件工具收到绝对路径或越界路径 | 使用相对路径，并限制在当前 workspace 内 |
| `Skills root not found` | 开启 skills 后，`liteflow.agent.skills.path` 指向的目录不存在 | 创建 skills 根目录；或关闭 skills；或在开发环境把 `strict=false` 以记录 warn 后继续 |
| `Declared skills not found: [...]` | 组件 `skills()` 声明了不存在的技能名，或 `SKILL.md` 中的 `name` 与预期不一致 | 检查技能目录与 `SKILL.md` frontmatter 的 `name`；严格模式下必须全部存在 |
| `references unknown tool class` | `SKILL.md` 的 `tools` 字段声明了 classpath 上找不到的 Java 工具类 | 确认工具类全限定名正确、依赖已打包 |
| `tool class ... instantiation failed` | 工具类既不在容器中（无法按类型取 bean），又没有可用的公有无参构造器 | 把工具类注册为 Spring / Solon bean 以走依赖注入，或为它补一个公有无参构造器作为兜底 |
| `usedSkills()` 为空 | 本轮 Agent 没有成功调用 `load_skill_through_path`，或读取时已离开 `process()` 生命周期 | 在 `handleReply()` 或工具回调中读取；确认模型确实加载了对应 skill |
| 没有收到流式事件 | 本次调用没有使用 `ExecuteOption.eventListener(...)`，或链路中没有 ReAct Agent 节点 | 注册 listener；确认事件类型是否为 `agent.reasoning`、`agent.tool_result`、`agent.summary` 或 `agent.result` |
| `ctx() must be called during process()` | 在构造器、Bean 初始化、异步线程或 `process()` 结束后的生命周期中调用了 `ctx()` | 只在 `userPrompt()`、工具回调、Hook 回调和 `handleReply()` 等 `process()` 触发的调用链内读取；跨 invocation 缓存的对象不要保存 `ReActAgentContext` |
| 注册 listener 后链路失败 | `eventListener` 回调中抛出了异常，或执行了阻塞 I/O 导致上游超时 | listener 内只做轻量处理并自行捕获异常；重型转发逻辑放到外部队列或线程池 |
| `WHEN` 中多个 Agent 看起来没有并行 | 多个组件解析到了相同 `(conversationId, agentKey)`，共用了同一把锁；或下游等待最慢分支 | 确保需要并行的 Agent 使用不同 `agentKey()`；如还要隔离文件，则使用不同 conversation 或子目录 |
| `ctx().getChatUsage()` 返回 `null` | 本轮还没发生过 reasoning step（如 `userPrompt()` 阶段就读取），或模型 / 网关未在响应里上报 `ChatUsage`（部分流式实现或代理网关会丢失 usage） | 改到 `handleReply()` 等本轮 reasoning 结束后的时机再读；或确认模型 / 网关在响应 metadata 中带回 usage 字段 |
| `ctx().getChatUsage()` 的累计 token 比 SDK 单次响应大 | 同一次 `process()` 内 ReAct 触发了多轮 reasoning，框架内置 `ChatUsageTrackingHook` 会把每步 LLM 调用的 usage 都累加进来 | 这是预期行为：`getChatUsage()` 是本次调用的总计；若需要单步 usage，从 `ReActLoggingHook` 的 `PostReasoningEvent` 日志或自定义 hook 读取 |

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
