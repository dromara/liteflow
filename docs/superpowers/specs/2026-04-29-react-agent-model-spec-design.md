# ReActAgentComponent 模型声明 API 重设计

- 日期: 2026-04-29
- 范围: liteflow-react-agent-core 与四个 provider 子模块
- 状态: 已对齐，待实现

## 1. 背景

当前 `ReActAgentComponent` 暴露 `protected abstract Model buildModel(ReActAgentContext ctx)`，子类必须自行：

1. 从 `agentConfig().getOpenaiCompatible().get("deepseek")` 这类硬编码路径里取 credential；
2. 调用 `OpenAICompatiblePresets.deepseek(apiKey, modelName)` 等静态工厂；
3. 直接返回 agentscope 的 `Model` 对象。

存在三个问题：

- **第三方 SDK 泄漏**：业务子类直接依赖 `io.agentscope.core.model.Model`。
- **配置查找泄漏**：业务子类得知道 `AgentConfig` 的内部结构与平台 key 字符串。
- **缺少高级参数入口**：`temperature`、`thinkingBudget`、`reasoningEffort` 等参数没有方便的位置可配，必须自己重新走 agentscope builder。

## 2. 目标

1. 子类不再构造 agentscope `Model`。
2. 子类不再硬编码配置查找路径。
3. 平台通过方法调用而不是 String 字面量指定（编译期可发现错误）。
4. 共性参数（temperature 等）由类型安全 setter 提供；平台个性参数（thinkingBudget / thinkingLevel / reasoningEffort）由各家自己的子构建器提供，命名沿用各家原生术语。
5. 最终覆写形式必须可以直接放进 javax-pro 脚本的 `return` 表达式。

## 3. 非目标

- 不引入注解声明（用户明确不需要）。
- 不引入运行时动态选模型机制（用户明确不需要）。
- 不调整 session、memory、toolkit 等现有钩子。

## 4. 核心抽象

### 4.1 ModelSpec

位置：`liteflow-react-agent-core`，包 `com.yomahub.liteflow.agent.model`。

```
abstract class ModelSpec<SELF extends ModelSpec<SELF>> {
    // 共性 setter（fluent，泛型 SELF 保留子类返回类型）
    SELF temperature(double v)
    SELF topP(double v)
    SELF topK(int v)
    SELF maxTokens(int v)
    SELF seed(long v)
    SELF stream(boolean v)
    SELF cacheControl(boolean v)

    // SPI 入口：把 ModelSpec 解析成 agentscope Model
    abstract Model resolve(AgentConfig cfg);
}
```

`resolve(AgentConfig)` 是 provider 模块要实现的核心方法：从 `AgentConfig` 取出 credential，把共性 + 个性参数翻译成 `GenerateOptions`，构造对应的 `OpenAIChatModel` / `AnthropicChatModel` / `GeminiChatModel` / `DashScopeChatModel`。

### 4.2 平台入口类

每个 provider 模块各自暴露入口类，类名即平台。

#### liteflow-react-agent-openai

```
class OpenAI         { static OpenAISpec of(String modelName) }              // agent.openai
class DeepSeek       { static OpenAICompatibleSpec of(String modelName) }    // openaiCompatible.deepseek
class Kimi           { static OpenAICompatibleSpec of(String modelName) }    // openaiCompatible.kimi
class GLM            { static OpenAICompatibleSpec of(String modelName) }    // openaiCompatible.glm
class Minimax        { static OpenAICompatibleSpec of(String modelName) }    // openaiCompatible.minimax

class OpenAICompatible {
    // 自定义兜底：用户在配置里挂一个 openaiCompatible.<configKey>
    static OpenAICompatibleSpec custom(String configKey, String modelName)
}

class OpenAISpec extends ModelSpec<OpenAISpec> {
    OpenAISpec reasoningEffort(String level)   // "low" | "medium" | "high"，o1 系列
    OpenAISpec frequencyPenalty(double v)
    OpenAISpec presencePenalty(double v)
}

// OpenAICompatibleSpec 与 OpenAISpec 可调参数一致；继承 OpenAISpec，
// 仅在 resolve 时换 credential 解析路径与 baseUrl 默认值。
class OpenAICompatibleSpec extends OpenAISpec { ... }
```

每个内置兼容厂商在自己的入口类内部硬编码默认 baseUrl（沿用现有 `OpenAICompatiblePresets`），用户在配置文件中显式提供 baseUrl 时以用户配置为准。

#### liteflow-react-agent-anthropic

```
class Anthropic            { static AnthropicSpec of(String modelName) }              // agent.anthropic
class AnthropicCompatible  { static AnthropicSpec custom(String configKey, String modelName) }

class AnthropicSpec extends ModelSpec<AnthropicSpec> {
    AnthropicSpec thinking(Consumer<AnthropicThinking> c)
}
class AnthropicThinking {
    AnthropicThinking budget(int tokens)        // budget_tokens
    AnthropicThinking enabled(boolean v)
}
```

#### liteflow-react-agent-gemini

```
class Gemini { static GeminiSpec of(String modelName) }   // agent.gemini

class GeminiSpec extends ModelSpec<GeminiSpec> {
    GeminiSpec thinking(Consumer<GeminiThinking> c)
}
class GeminiThinking {
    GeminiThinking level(String level)          // "low" | "medium" | "high"，Gemini 2.5 用语
    GeminiThinking budget(int tokens)           // 兼容老接口
}
```

#### liteflow-react-agent-dashscope

```
class DashScope { static DashScopeSpec of(String modelName) }   // agent.dashscope

class DashScopeSpec extends ModelSpec<DashScopeSpec> {
    DashScopeSpec thinking(Consumer<DashScopeThinking> c)
}
class DashScopeThinking {
    DashScopeThinking budget(int tokens)        // 通义千问 thinking_budget
}
```

**关键特性**：thinking 在不同平台的子构建器使用各厂商的原生术语（Gemini `level()`、DashScope `budget()`、Anthropic `budget()/enabled()`），不强行统一。

## 5. ReActAgentComponent 的变化

```diff
 public abstract class ReActAgentComponent extends NodeComponent {
-    protected abstract Model buildModel(ReActAgentContext ctx);
+    /** 子类只需返回 ModelSpec；框架负责解析 credential 与构造 Model。 */
+    protected abstract ModelSpec<?> model(ReActAgentContext ctx);
+
+    /** Escape hatch：高级用户可整体绕过 ModelSpec。默认实现委派给 model().resolve(cfg)。 */
+    protected Model buildModel(ReActAgentContext ctx) {
+        return model(ctx).resolve(agentConfig());
+    }
     ...
 }
```

`buildAgent(ctx)` 内部仍调用 `buildModel(ctx)`——只是默认实现现在走 `model() → resolve(cfg)` 这条路。

### 5.1 典型子类

```java
@Component("deepseekAgent")
public class DeepSeekAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DeepSeek.of("deepseek-chat")
                .temperature(0.7);
    }

    @Override protected String systemPrompt(ReActAgentContext ctx) { ... }
    @Override protected String userPrompt(ReActAgentContext ctx)   { ... }
    @Override protected boolean enableShellTool()           { return false; }
    @Override protected boolean enableWorkspaceFileTools()  { return false; }
}
```

### 5.2 Gemini 高级用法

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return Gemini.of("gemini-2.5-pro")
            .temperature(0.7)
            .thinking(t -> t.level("high"));
}
```

### 5.3 javax-pro 脚本

```java
return DeepSeek.of("deepseek-chat").temperature(0.7);
```

## 6. credential 解析规则

| 入口 | credential 来源 | baseUrl 来源 |
|---|---|---|
| `OpenAI.of(...)` | `cfg.getOpenai()` | preset 默认 + 用户 override |
| `DeepSeek/Kimi/GLM/Minimax.of(...)` | `cfg.getOpenaiCompatible().get("<platform>")` | 入口类内置 preset baseUrl，用户配置中的 baseUrl 优先 |
| `OpenAICompatible.custom(key, ...)` | `cfg.getOpenaiCompatible().get(key)` | 用户配置中 baseUrl 必填，缺失抛 `AgentConfigException` |
| `Anthropic.of(...)` | `cfg.getAnthropic()` | preset + override |
| `AnthropicCompatible.custom(key, ...)` | `cfg.getAnthropicCompatible().get(key)` | 用户配置 baseUrl 必填 |
| `Gemini.of(...)` | `cfg.getGemini()` | preset + override |
| `DashScope.of(...)` | `cfg.getDashscope()` | preset + override |

`PlatformCredential` 缺失或 `apiKey` 为空时抛 `AgentConfigException`，错误信息明确给出 `liteflow.agent.<path>.api-key` 配置路径。

## 7. 模块影响

- **新增**：`liteflow-react-agent-core` 中的 `com.yomahub.liteflow.agent.model.ModelSpec`。
- **改动**：`liteflow-react-agent-core/ReActAgentComponent`——`buildModel` 不再 `abstract`，新增 `abstract model(ctx)`。
- **改动**：四个 provider 模块各新增入口类与对应 `XxxSpec`。
- **保留**：现有 `OpenAIModelFactory` / `OpenAICompatiblePresets` / `AnthropicModelFactory` / `GeminiModelFactory` / `DashScopeModelFactory`——`Spec.resolve(cfg)` 内部继续复用，避免重复 builder 逻辑。
- **测试**：`liteflow-testcase-el-react-agent` 增补针对新 API 的用例。

## 8. 向后兼容

`buildModel(ctx)` 不再是 `abstract`，但仍存在。已直接 override `buildModel` 的子类继续编译通过、运行无变化。新代码推荐 override `model(ctx)`。后续视情况再决定是否对 `buildModel` 加 `@Deprecated`。

## 9. 命名约定

- 抽象方法：`model(ReActAgentContext ctx)`。
- 描述符基类：`ModelSpec<SELF>`。
- 平台入口类：`OpenAI` / `DeepSeek` / `Kimi` / `GLM` / `Minimax` / `OpenAICompatible` / `Anthropic` / `AnthropicCompatible` / `Gemini` / `DashScope`。
- 平台 spec：`OpenAISpec` / `OpenAICompatibleSpec` / `AnthropicSpec` / `GeminiSpec` / `DashScopeSpec`。
- thinking 子构建器：`AnthropicThinking` / `GeminiThinking` / `DashScopeThinking`。
