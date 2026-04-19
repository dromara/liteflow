# LiteFlow ReAct Agent 模块设计

- **日期**：2026-04-19
- **作者**：Bryan.Zhang（与 Claude Code 协同设计）
- **状态**：待实施
- **依赖框架**：agentscope-java（ReActAgent、Model、Toolkit、@Tool 等）

## 1. 目标与范围

为 LiteFlow 新增一套 ReAct Agent 组件能力，使用户能在 LiteFlow 的规则编排（EL）中像使用普通 `NodeComponent` 一样使用 ReAct Agent。Agent 底层由 agentscope-java 驱动，支持多个主流大模型平台。

**核心需求**：

1. 用户通过**继承抽象类**的方式定义 Agent 组件；抽象类最终继承自 `NodeComponent`。
2. 每个 session 拥有独立的临时 workspace；workspace 根目录由配置文件指定。
3. 每个平台的 apiKey 由配置文件指定。
4. ReAct Agent 只能操作其所属 session 的 workspace，不能穿越到 workspace 之外。
5. ReAct Agent 可执行的 shell 命令可控，由配置文件定义白名单/黑名单/禁用策略。

**非目标**：

- 不提供 "chain 自动变 tool" 或 "NodeComponent 自动桥接为 tool" 的二次抽象；用户使用 agentscope 原生 `@Tool` 体验。
- 不二次封装 agentscope 的 Hook 机制；直接复用原生接口。

## 2. 总体架构

```
用户继承 ReActAgentComponent  ──►  LiteFlow 的 EL 编排（THEN/WHEN/RETRY/CATCH/TIMEOUT ...）
                │
                ▼
   ReActAgentComponent.process()  (final)
                │
     ┌──────────┼──────────────┬────────────────────┐
     ▼          ▼              ▼                    ▼
AgentSession  Model       Toolkit              Hooks
Manager       (平台模块)   (用户 tools +        (agentscope 原生)
  │                        内置 workspace/
workspace 目录              shell 工具)
  │
文件系统隔离 + 空闲回收
```

## 3. 模块结构

```
liteflow-react-agent/                       <pom>  父模块
├── liteflow-react-agent-core               <jar>  核心：抽象类、配置、workspace/shell/session 管控
├── liteflow-react-agent-openai             <jar>  OpenAI + OpenAI 兼容（DeepSeek/Kimi/GLM/MiniMax）
├── liteflow-react-agent-anthropic          <jar>
├── liteflow-react-agent-gemini             <jar>  传递 google-genai 依赖
└── liteflow-react-agent-dashscope          <jar>  阿里千问
```

**依赖关系**：

- 所有平台模块 → `liteflow-react-agent-core`
- `liteflow-react-agent-core` → `agentscope-core` + `liteflow-core` + `liteflow-spring`
- 父 pom 在 LiteFlow 根 `pom.xml` 的 `<modules>` 列表新增一行

**OpenAI 兼容厂商处理**：`-openai` 模块提供 `OpenAICompatiblePresets` 便捷工厂：

```java
OpenAICompatiblePresets.deepseek(apiKey, "deepseek-chat")
OpenAICompatiblePresets.kimi(apiKey, "moonshot-v1-8k")
OpenAICompatiblePresets.glm(apiKey, "glm-4")
OpenAICompatiblePresets.minimax(apiKey, "abab6.5s-chat")
```

所有 preset 内部都构造 `OpenAIChatModel`，仅 baseUrl 不同，避免冗余子模块。

## 4. 核心抽象类 `ReActAgentComponent`

位于 `com.yomahub.liteflow.agent.core`，继承 `NodeComponent`。

```java
public abstract class ReActAgentComponent extends NodeComponent {

    /* ===== 框架提供的 final 访问器 ===== */

    /** 统一从 LiteflowConfig 取 agent 配置，子类无需 @Resource 注入。final 不可覆写 */
    protected final AgentConfig agentConfig() {
        return LiteflowConfigGetter.get().getAgent();
    }

    /* ===== 必须实现 ===== */

    /** 构建 agentscope 的 Model（平台+模型+apiKey+参数）。
     *  子类通过 agentConfig() 直接拿 apiKey / baseUrl，无需注入配置类 */
    protected abstract Model buildModel(ReActAgentContext ctx);

    /** 系统提示词；ctx 含 sessionId / workspace 路径，可拼入 prompt。
     *  一次性会话场景下可返回空串，所有业务意图写在 userPrompt 里即可 */
    protected abstract String systemPrompt(ReActAgentContext ctx);

    /** 本次要发送给 agent 的用户消息（LLM Role=user）。
     *  必须实现——一次性会话时核心意图全部写在这里 */
    protected abstract String userPrompt(ReActAgentContext ctx);

    /* ===== 可选覆写 ===== */

    /** 用户自定义 tools，默认空；框架会自动追加 WorkspaceFileTools + ShellCommandTool（可关） */
    protected List<Object> tools(ReActAgentContext ctx) { return List.of(); }

    /** session 解析。默认 slot.requestId（无状态模式）；覆写可实现多轮对话 */
    protected String resolveSessionId(Slot slot) { return slot.getRequestId(); }

    /** 最大迭代次数；返回 -1 使用全局配置 */
    protected int maxIterations() { return -1; }

    /** 是否启用内置 ManagedShellCommandTool */
    protected boolean enableShellTool() { return true; }

    /** 是否启用内置 WorkspaceFileTools */
    protected boolean enableWorkspaceFileTools() { return true; }

    /** agentscope Hook 列表（日志、指标等） */
    protected List<AgentHook> hooks(ReActAgentContext ctx) { return List.of(); }

    /** 结果写回 slot 的方式；默认 setResponseData(textContent) */
    protected void handleReply(Msg reply, ReActAgentContext ctx) {
        ctx.getSlot().setResponseData(reply.getTextContent());
    }

    /* ===== 框架 final ===== */

    @Override
    public final void process() throws Exception { /* 见 §8 */ }
}
```

**配套类型**：

| 类型 | 作用 |
|------|------|
| `ReActAgentContext` | 持有 `Slot`, `sessionId`, `Path workspaceDir`（`AgentConfig` 通过 `agentConfig()` 取，不重复放入 ctx） |
| `AgentSession` | 单 sessionId 对应的 `ReActAgent` 实例 + `workspace` + `ReentrantLock` + `lastActive` |
| `AgentSessionManager` | `ConcurrentHashMap<String, AgentSession>` + 定时回收 + LRU 淘汰 |

**关键设计点**：

- `process()` final：保证 session 获取/锁/释放与异常处理由框架掌控。
- Agent 实例**懒构建**并缓存在 `AgentSession`，同一 sessionId 复用，memory 自动累积（多轮对话能力来自此）。
- 默认 `resolveSessionId = slot.requestId` → 每次执行独立 session（无状态）；覆写后可跨次共享。

### 4.1 模型高级参数（如 thinking level）的处置

各平台的 "thinking level" 参数形态差异很大（Gemini 用 `ThinkingLevelFormatter`、OpenAI o 系列用 `reasoning_effort`、Anthropic 用 `thinking.budget_tokens`、DashScope 用 `enable_thinking`、DeepSeek 用模型名区分）。

**设计决策**：抽象类**不**统一封装，`AgentConfig` 里也**不**提供 thinking 配置项。用户在 `buildModel(ctx)` 内用 agentscope 原生 builder 自行设置。好处：平台演进新增高级参数（temperature / top_p / safety_settings / reasoning_timeout 等）不需要修改 LiteFlow 的 schema 或 core 抽象。

示例：

```java
// Gemini
return GeminiChatModel.builder()
        .apiKey(apiKey).modelName("gemini-3-flash-preview")
        .formatter(new ThinkingLevelFormatter("high"))
        .build();

// Anthropic
return AnthropicChatModel.builder()
        .apiKey(apiKey).modelName("claude-sonnet-4-6")
        .thinkingBudgetTokens(8000)
        .build();

// DashScope（千问）
return DashScopeChatModel.builder()
        .apiKey(apiKey).modelName("qwen3-max")
        .enableThinking(true)
        .build();
```

## 5. 平台子模块便捷工厂

每个平台模块**极薄**，只提供一个 Model 工厂类。

```java
// liteflow-react-agent-openai
public final class OpenAIModelFactory {
    public static OpenAIChatModel openai(String apiKey, String model);
    public static OpenAIChatModel custom(String apiKey, String baseUrl, String model);
}
public final class OpenAICompatiblePresets {
    public static OpenAIChatModel deepseek(String apiKey, String model); // baseUrl 预设
    public static OpenAIChatModel kimi(String apiKey, String model);
    public static OpenAIChatModel glm(String apiKey, String model);
    public static OpenAIChatModel minimax(String apiKey, String model);
}

// liteflow-react-agent-anthropic
public final class AnthropicModelFactory {
    public static AnthropicChatModel of(String apiKey, String model);
}

// liteflow-react-agent-gemini
public final class GeminiModelFactory {
    public static GeminiChatModel of(String apiKey, String model);
    public static GeminiChatModel of(String apiKey, String model, String thinkingLevel);
}

// liteflow-react-agent-dashscope
public final class DashScopeModelFactory {
    public static DashScopeChatModel of(String apiKey, String model);
}
```

**用户代码示例**（无 `@Resource`，apiKey 走 `agentConfig()`）：

```java
@LiteflowComponent("reviewAgent")
public class ReviewAgent extends ReActAgentComponent {

    @Override
    protected Model buildModel(ReActAgentContext ctx) {
        PlatformCredential c = agentConfig().getOpenaiCompatible().get("deepseek");
        return OpenAICompatiblePresets.deepseek(c.getApiKey(), "deepseek-chat");
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是合同审核专家，当前工作区：" + ctx.getWorkspaceDir();
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return ctx.getSlot().getRequestData(String.class);
    }

    @Override
    protected List<Object> tools(ReActAgentContext ctx) {
        return List.of(new ContractTools());
    }
}
```

EL 里像普通节点编排：`THEN(prepare, reviewAgent, notify);`

## 6. 配置结构 `AgentConfig`

**与 `LiteflowConfig` 的融合**：不新增独立的 `@ConfigurationProperties` 类。在 `LiteflowConfig` 上新增一个字段 `private AgentConfig agent`，Spring Boot 的 `@ConfigurationProperties(prefix = "liteflow")`（LiteFlow 现有装配入口）会自动把 `liteflow.agent.*` 映射到 `LiteflowConfig.agent.*`，零改造。

**类的归属**（方案 Z）：`AgentConfig` 及其嵌套类型（`Workspace` / `Session` / `Shell` / `Defaults` / `PlatformCredential`）放在 `liteflow-core` 模块的 `com.yomahub.liteflow.property.agent` 包下。**严格保证纯 POJO**：只有配置字段 + getter/setter，不引用 agentscope、不引用任何第三方 SDK。这样 `liteflow-core` 仅多一个配置容器，零额外传递依赖；真正 agent 的执行代码全在 `liteflow-react-agent-core` 及各平台模块。

**访问方式**：`LiteflowConfigGetter.get().getAgent()`——复用现有机制，Spring / Solon / 非 Spring 三种场景统一。

前缀 `liteflow.agent`。Spring Boot 下自动装配；非 Spring 场景通过 `new LiteflowConfig()` + `setAgent(...)` 手动构造后传给 `FlowExecutorHolder.loadInstance(config)`。

```yaml
liteflow:
  agent:
    workspace:
      root: /var/lib/liteflow/agent-workspaces    # 必填
      auto-create: true
      cleanup-on-session-expire: true
      cleanup-on-jvm-shutdown: false
      max-file-bytes: 10485760                    # 单文件 10MB
      max-list-size: 1000                         # list_files 单次条数上限

    session:
      idle-timeout: 30m                           # java.time.Duration
      cleanup-interval: 1m
      max-sessions: 10000                         # 达上限按 LRU 淘汰

    shell:
      mode: whitelist                             # whitelist | blacklist | disabled
      whitelist: [ls, cat, grep, find, head, tail, wc, sed, awk, python3, node]
      blacklist: [rm, sudo, shutdown, mkfs, dd]   # mode=blacklist 时生效
      timeout: 30s
      max-output-bytes: 1048576

    defaults:
      max-iterations: 15

    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: https://api.openai.com/v1
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
    gemini:
      api-key: ${GEMINI_API_KEY:}
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}

    openai-compatible:                            # Map<厂商名, PlatformCredential>
      deepseek:
        api-key: ${DEEPSEEK_API_KEY:}
        base-url: https://api.deepseek.com/v1
      kimi:
        api-key: ${MOONSHOT_API_KEY:}
        base-url: https://api.moonshot.cn/v1
      glm:
        api-key: ${ZHIPUAI_API_KEY:}
        base-url: https://open.bigmodel.cn/api/paas/v4
      minimax:
        api-key: ${MINIMAX_API_KEY:}
        base-url: https://api.minimax.chat/v1
```

**Java 侧**（`liteflow-core` 中）：

```java
// liteflow-core: LiteflowConfig 上新增一个字段
public class LiteflowConfig {
    // ... 现有所有字段保持不变 ...
    private AgentConfig agent;

    public AgentConfig getAgent() { return agent; }
    public void setAgent(AgentConfig agent) { this.agent = agent; }
}

// liteflow-core 新包 com.yomahub.liteflow.property.agent
public class AgentConfig {
    private Workspace workspace = new Workspace();
    private Session session = new Session();
    private Shell shell = new Shell();
    private Defaults defaults = new Defaults();
    private PlatformCredential openai = new PlatformCredential();
    private PlatformCredential anthropic = new PlatformCredential();
    private PlatformCredential gemini = new PlatformCredential();
    private PlatformCredential dashscope = new PlatformCredential();
    private Map<String, PlatformCredential> openaiCompatible = new LinkedHashMap<>();
    // + getter/setter（全部纯 POJO，无 agentscope / 第三方 SDK 依赖）
}

public class PlatformCredential {
    private String apiKey;
    private String baseUrl;
    private Map<String, String> extra = new LinkedHashMap<>();  // 组织 ID、代理等
}
```

**装配逻辑**：
- Spring Boot：`LiteflowProperty` 已经以 prefix `liteflow` 映射到 `LiteflowConfig`，`liteflow.agent.*` 会被 Spring Boot 自动嵌套装配进去，无需额外 `@ConfigurationProperties`
- `AgentSessionManager` 在 `liteflow-react-agent-core` 模块中以 Spring Bean 注册（或非 Spring 下懒加载静态单例），不需要修改 starter 模块

## 7. Workspace 与 Shell 管控

### 7.1 Workspace 隔离

- 路径：`{workspace.root}/{safeSessionId}/`
- `safeSessionId` 只保留 `[a-zA-Z0-9_\-]`，其他字符 URL-encode，防路径穿越
- `AgentSessionManager.getOrCreate(sessionId)` 首次调用时 `Files.createDirectories`，路径缓存在 `AgentSession`
- `ReActAgentContext.getWorkspaceDir()` 暴露路径给子类，可写入 systemPrompt

**内置 `WorkspaceFileTools`**（agent 可调用的 `@Tool` 方法）：

- `read_file(relativePath)`
- `write_file(relativePath, content)`
- `list_files(relativePath)`
- `delete_file(relativePath)`

每个方法入参先 `resolve` 再 `normalize`，检查 `startsWith(workspaceDir)`，否则抛 `SecurityException`；单文件大小、单次列表条数受配置约束。

### 7.2 Shell 命令管控

**`ManagedShellCommandTool`** 包装 agentscope 原生 `ShellCommandTool`，执行前三重校验：

1. `mode=disabled` 直接拒绝
2. 解析命令首 token（用 `ProcessBuilder` 参数列表而非 `sh -c`，避免 shell 注入）
   - `whitelist` 模式：首 token ∉ whitelist 拒绝
   - `blacklist` 模式：首 token ∈ blacklist 拒绝
3. 强制 `workingDirectory = workspaceDir`；timeout / 输出上限由 config 控制

**拒绝返回结构化错误**给 LLM（`{"error":"command 'rm' not allowed by policy"}`），不中断 agent 循环。

### 7.3 禁用开关

子类可覆写 `enableShellTool()` / `enableWorkspaceFileTools()` 返回 `false`，完全自定义 tools 集合。

## 8. 执行流程与错误处理

### 8.1 单次 `process()` 完整时序

```
1. sessionId = resolveSessionId(slot)
2. AgentSession session = AgentSessionManager.acquire(sessionId)
     - 新 session：建 workspace，新建 AgentSession（不含 agent 实例）
     - 老 session：touch lastActive
3. session.lock.lock()
   try {
4.   if (session.agent == null) {
5.       AgentConfig cfg = LiteflowConfigGetter.get().getAgent()
6.       ctx = new ReActAgentContext(slot, sessionId, workspace)
7.       Model model = buildModel(ctx)                                     // 子类内可调 agentConfig() 取 apiKey
8.       Toolkit toolkit = new Toolkit()
9.       tools(ctx).forEach(toolkit::registerTool)
10.      if (enableWorkspaceFileTools()) toolkit.registerTool(new WorkspaceFileTools(workspace, cfg))
11.      if (enableShellTool())          toolkit.registerTool(new ManagedShellCommandTool(workspace, cfg))
12.      session.agent = ReActAgent.builder()
                         .model(model).toolkit(toolkit)
                         .sysPrompt(systemPrompt(ctx))
                         .memory(new InMemoryMemory())
                         .maxIters(resolvedMaxIterations())
                         .hooks(hooks(ctx))
                         .build()
13.  }
14.  Msg reply = session.agent.call(Msg.builder().textContent(userPrompt(ctx)).build()).block()
15.  handleReply(reply, ctx)
   } finally {
16.  session.lock.unlock()
   }
```

### 8.2 异常分类

| 异常 | 来源 | 处理 |
|------|------|------|
| `AgentConfigException` | 配置缺失（apiKey 空、workspace root 无权限） | 启动 `@PostConstruct` 快速失败；运行期直接抛 |
| `AgentInvocationException` | `agent.call()` 抛出（网络/模型/token 超限） | 透出到 LiteFlow，由 `onError()` / `CATCH` / `RETRY` 处理 |
| `SecurityException` | 路径穿越 / shell 拦截 | 工具内部转结构化错误返回给 LLM，不中断循环；框架侧越权才抛 |
| `InterruptedException` | agent 调用期间线程中断 | 还原中断状态后抛 `AgentInvocationException`，session 锁一定释放 |

**原则**：异常不被 agent 组件吞掉；LiteFlow 自身的 RETRY/CATCH/TIMEOUT 对它们生效。

### 8.3 生命周期回收

- `AgentSessionManager` 启动时 `scheduleAtFixedRate(cleanup, 0, cleanupInterval)`
- `cleanup()` 遍历：`lastActive + idleTimeout < now` → `tryLock(0ms)` 成功则移除并递归删 workspace（避免删除正在执行的 session）
- JVM shutdown hook：根据 `cleanup-on-jvm-shutdown` 决定是否全清
- 实现 `DisposableBean`，Spring 容器关闭时停调度器

### 8.4 与 LiteFlow 既有能力的结合

- **监控**：`NodeComponent` 自带 `MonitorBus`，agent 耗时/成功率自动纳入监控
- **重试**：EL 中 `RETRY(reviewAgent).times(3).forException(AgentInvocationException.class)` 开箱可用
- **超时**：EL 中 `TIMEOUT(reviewAgent).time(60000)` 生效（走 LiteFlow 自身超时机制）
- **回滚**：子类可覆写 `rollback()` 清理 workspace 中本次产生的文件，参与 LiteFlow 回滚链

## 9. 测试策略

**单元测试（core 模块内，无网络）**：

- `WorkspaceFileToolsTest`：`../../etc/passwd` 拒绝；正常读写；超大文件截断；list 超条数截断
- `ManagedShellCommandToolTest`：whitelist 放行/拦截；blacklist 拦截；`disabled` 全拒；timeout；workingDir 强制 = workspace；多命令首 token 解析正确
- `AgentSessionManagerTest`：idle 过期回收；`tryLock` 失败不回收运行中 session；workspace 真被删除；max-sessions LRU 淘汰
- `ReActAgentComponentTest`：假 `Model` 驱动 `process()` 时序；lock 释放；异常透出；`resolveSessionId` 默认/覆写行为

**集成测试（需 apikey，默认 skip）**：

- 每平台子模块一个 `@EnabledIfEnvironmentVariable` 测试，真调一次简单问答，验证 `buildModel() → agent.call → workspace 写文件` 链路
- CI 默认不跑

**EL 集成测试（新增 `liteflow-testcase-el/liteflow-testcase-el-springboot-agent`）**：

- `THEN(prepare, reviewAgent, post)`
- `RETRY(reviewAgent).times(2)`
- `TIMEOUT(reviewAgent).time(5000)`
- `CATCH(reviewAgent).DO(fallback)`
- 断言：agent 参与 LiteFlow 编排、MonitorBus 打点、Slot 正确拿到 responseData
- 用 `FakeEchoModel implements Model` 避免网络依赖

**测试约定**：JUnit 5 + `@SpringBootTest` + `classpath:/application.properties`，与现有 testcase 一致。

## 10. 文档与示例

- core 模块 `README.md`：快速上手 + 核心概念（session/workspace/shell/tools）
- 各平台模块 `README.md`：Maven 坐标 + 一段可运行代码
- 官网 `liteflow.cc` 新增 "ReAct Agent 组件" 章节（后续补）
- 关键示例：将 `beast-react-agent-service` 的核心逻辑改写为 LiteFlow chain，放入 `liteflow-testcase-el-springboot-agent/src/main/resources`

## 11. 实施风险与缓解

| 风险 | 缓解 |
|------|------|
| agentscope-java 版本升级导致 Model API 变动 | 平台子模块是 Model 适配的唯一入口；core 不直接依赖 Model 具体类型 |
| workspace 根目录无写权限 | 启动期 `@PostConstruct` 尝试创建/写测试文件，失败抛 `AgentConfigException` |
| 长链路异步调用中 lock 持有时间过长 | `agent.call().block()` 期间 lock 保护 memory 一致性；配合 `TIMEOUT` EL 控制上限 |
| OpenAI 兼容厂商协议偏差 | preset 仅保底常见场景；特殊厂商用户自行 `OpenAIModelFactory.custom()` |
| shell 命令首 token 解析误判（如引号嵌套、管道） | 初期只支持单命令（不拆分 `\|` `&&`）；复杂场景拒绝执行并返错误给 LLM |
| `AgentConfig` 引入 `liteflow-core` 造成 core 污染 | `AgentConfig` 及嵌套类强制纯 POJO，无 agentscope/第三方 SDK 依赖；通过 PR review 卡准 |

## 12. 交付物清单

- `liteflow-core`：在 `LiteflowConfig` 上新增 `private AgentConfig agent` 字段 + 新包 `com.yomahub.liteflow.property.agent`（`AgentConfig`、`Workspace`、`Session`、`Shell`、`Defaults`、`PlatformCredential` 纯 POJO）
- `liteflow-react-agent/` 父 pom
- `liteflow-react-agent-core/` + 源码 + 单元测试（抽象类、`AgentSessionManager`、`WorkspaceFileTools`、`ManagedShellCommandTool` 等）
- `liteflow-react-agent-{openai,anthropic,gemini,dashscope}/` + 源码 + 单元测试
- `liteflow-testcase-el/liteflow-testcase-el-springboot-agent/` 集成测试模块
- 根 `pom.xml` 新增模块声明
- 各模块 `README.md`

## 13. 未决事项（后续实施计划阶段再定）

- agentscope-java 具体版本锁定（需验证 1.0.9 或更新版本的 API 兼容性）
- `InMemoryMemory` 是否足够，是否需要对接 `RogueMap` 等持久化方案作为可选增强
- Hooks 机制是否默认内置一个 `SessionLoggingHook`（默认打印到 LiteFlow 的 `LFLog`）
