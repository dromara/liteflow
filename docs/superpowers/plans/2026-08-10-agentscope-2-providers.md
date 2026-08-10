# AgentScope 2.0 Model Providers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 OpenAI、Anthropic、Gemini、DashScope 及 DeepSeek／GLM／Kimi／MiniMax 迁移到 AgentScope Java 2.0.2 的细粒度模型扩展，保留 `ModelSpec`／`buildModel()` 逃生口，并建立无网络 Provider 契约与显式 live smoke。

**Architecture:** 公共 `ModelSpec` 负责 credential 与 `GenerateOptions` 合并；四个 Provider 模块仅将 vendor-specific 选项映射到 2.0.2 原生 builder。DeepSeek、GLM、Kimi、MiniMax 通过 OpenAI extension 的 ServiceLoader `ModelProvider` 和 `ModelRegistry.resolve(...)` 构建，不再维护 LiteFlow 固定 URL。调用期路由只能选择 runtime 已拥有的 Model。

**Tech Stack:** Java 17、Maven、AgentScope Java 2.0.2 model extensions、Reactor、JUnit 5、Spring Boot Test。

## Global Constraints

- 本计划在 `2026-08-10-agentscope-2-core-runtime.md` 完成后执行；根 POM 已导入 2.0.2 BOM，core 已提供 `ModelSpec`、runtime ownership 与 `ModelRoutingMiddleware`。
- Provider 模块分别只引入 `agentscope-extensions-model-openai`、`-anthropic`、`-gemini`、`-dashscope`；禁止 shaded `io.agentscope:agentscope`。
- 删除 LiteFlow 对 `anthropic-java` 和 `google-genai` 的直接依赖与版本属性；SDK 版本由对应 AgentScope extension 管理。
- 所有 builder 契约测试不得发送 HTTP 请求。使用测试子类捕获参数，或只构建、读取属性并关闭 Model。
- `OpenAIChatModel.Builder` 使用 `.generateOptions(...)`；Anthropic、Gemini、DashScope 使用 `.defaultOptions(...)`。Gemini 流式开关是 `.streamEnabled(...)`。
- builder customizer 最后执行，作为高级用户逃生口；每个 Spec 在组件 runtime 构建期 resolve 一次，请求路径不得反复创建客户端。
- 默认测试排除 `@Tag("agentscope-live-smoke")`；真实请求只由 `agent-live` profile 显式运行。
- 所有 Maven 测试命令显式携带 `-DskipTests=false`；定向 reactor 测试携带 `-Dsurefire.failIfNoSpecifiedTests=false`。

## Verified AgentScope 2.0.2 API

```java
OpenAIChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .stream(stream)
        .generateOptions(options)
        .baseUrl(baseUrl)
        .endpointPath(endpointPath)
        .nativeStructuredOutput(enabled)
        .nativeStructuredOutputWithTools(enabled)
        .build();

AnthropicChatModel.builder().defaultOptions(options);
GeminiChatModel.builder().streamEnabled(stream).defaultOptions(options);
DashScopeChatModel.builder().stream(stream).enableThinking(enabled).defaultOptions(options);
```

```java
ModelCreationContext context = ModelCreationContext.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .endpointPath(endpointPath)
        .stream(stream)
        .enableThinking(enableThinking)
        .component(GenerateOptions.class, options)
        .build();

Model model = ModelRegistry.resolve(providerId + ":" + modelName, context);
```

内置 Provider ID：`deepseek:`、`glm:`、`kimi:`、`minimax:`。2.0.2 默认地址分别为 `https://api.deepseek.com`、`https://open.bigmodel.cn/api/paas/v4`、`https://api.moonshot.cn/v1`、`https://api.minimaxi.com/v1`。

---

## Task 1: 切换四个 Provider POM 到细粒度 extension

**Files:**

- Modify: `pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-gemini/pom.xml`
- Modify: `liteflow-react-agent/liteflow-react-agent-dashscope/pom.xml`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/test/java/com/yomahub/liteflow/agent/openai/ProviderExtensionClasspathTest.java`

- [ ] **Step 1: 写 extension classpath 失败测试**

测试导入并断言 `OpenAIChatModel`、`AnthropicChatModel`、`GeminiChatModel`、`DashScopeChatModel` 可加载，同时通过 classpath 检查拒绝 aggregate artifact 对应的 distribution marker。

- [ ] **Step 2: 运行测试并确认 extension 尚未直接声明**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=ProviderExtensionClasspathTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，Provider POM 仍只依赖旧 aggregate 的传递类。

- [ ] **Step 3: 修改 POM 和根版本属性**

四个 POM 分别增加无版本 extension 依赖；Anthropic 删除直接 `com.anthropic:anthropic-java`；Gemini 删除直接 `com.google.genai:google-genai`。根 POM 删除不再使用的 `anthropic-java.version` 与 `google-genai.version`。

- [ ] **Step 4: 运行 classpath 测试与依赖树**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=ProviderExtensionClasspathTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn dependency:tree -pl liteflow-react-agent -am \
  -Dincludes=io.agentscope:*,com.anthropic:anthropic-java,com.google.genai:google-genai,com.alibaba:dashscope-sdk-java \
  -Dverbose
```

Expected: 测试 PASS；四个 extension 均为 2.0.2；无 aggregate；厂商 SDK 只由 extension 传递。

- [ ] **Step 5: 提交依赖切换**

```bash
git add pom.xml liteflow-react-agent/liteflow-react-agent-*/pom.xml \
  liteflow-react-agent/liteflow-react-agent-openai/src/test
git commit -m "build(agent): adopt AgentScope 2 model extensions"
```

---

## Task 2: 集中公共 `GenerateOptions` 合并语义

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/CredentialResolver.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/model/ModelSpecTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/test/java/com/yomahub/liteflow/agent/model/CredentialResolverTest.java`

- [ ] **Step 1: 写 common options 和 credential 优先级失败测试**

覆盖 temperature、topP、topK、maxTokens、maxCompletionTokens、seed、cacheControl、parallelToolCalls、executionConfig、additional header/body/query。覆盖用户原生 `GenerateOptions`，优先级固定为：原生 options 最高、Provider 专有 options 次之、公共 fluent options 最后；无配置时返回 null，避免覆盖 Provider 默认值。显式 spec credential 高于 `AgentConfig` 平台默认值；缺失 apiKey 抛 `AgentConfigException`。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ModelSpecTest,CredentialResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，新字段和合并规则尚未实现。

- [ ] **Step 3: 实现统一 API**

```java
public SELF maxCompletionTokens(int value);
public SELF parallelToolCalls(boolean value);
public SELF generateOptions(GenerateOptions options);

protected final GenerateOptions mergeGenerateOptions(
        GenerateOptions providerOptions);
```

使用 `GenerateOptions.mergeOptions(primary, fallback)` 固定优先级；Provider 不再复制公共 option 装配代码。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-core -am \
  -DskipTests=false -DskipITs \
  -Dtest=ModelSpecTest,CredentialResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src
git commit -m "refactor(agent): centralize model generate options"
```

---

## Task 3: 迁移 OpenAI 和通用 OpenAI Compatible

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAI.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAISpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatible.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAIModelFactory.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/test/java/com/yomahub/liteflow/agent/openai/OpenAISpecTest.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/test/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpecTest.java`

- [ ] **Step 1: 写 builder 参数失败测试**

断言 apiKey、baseUrl、endpointPath、modelName、stream、reasoningEffort、frequencyPenalty、presencePenalty、formatter、native structured output 两个标志。使用覆写 `buildModel(...)` 的测试子类捕获参数，不发网络请求；通用 compatible endpoint 无 baseUrl 时明确失败。

- [ ] **Step 2: 运行测试并确认旧 package／builder 失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=OpenAISpecTest,OpenAICompatibleSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 迁移到 OpenAI extension API**

import 切换到 `io.agentscope.extensions.model.openai.*`。`OpenAISpec` 增加：

```java
public OpenAISpec endpointPath(String endpointPath);
public OpenAISpec formatter(
        Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter);
public OpenAISpec nativeStructuredOutput(boolean enabled);
public OpenAISpec nativeStructuredOutputWithTools(boolean enabled);
public OpenAISpec customizeBuilder(Consumer<OpenAIChatModel.Builder> customizer);
protected Model buildModel(String apiKey, String baseUrl);
```

构建时使用 `.generateOptions(options)`，customizer 最后执行。`OpenAICompatibleSpec` 只处理用户自定义 endpoint，不再承载四家 first-party preset。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=OpenAISpecTest,OpenAICompatibleSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-openai
git commit -m "feat(agent): migrate OpenAI adapter to AgentScope 2"
```

---

## Task 4: 改用官方 DeepSeek／GLM／Kimi／MiniMax ModelProvider

**Files:**

- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAIProviderSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/DeepSeek.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/GLM.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Kimi.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Minimax.java`
- Delete: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatiblePresets.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/test/java/com/yomahub/liteflow/agent/openai/FirstPartyModelProviderTest.java`

- [ ] **Step 1: 写 registry 和地址契约失败测试**

用显式 `ModelCreationContext.apiKey(...)`，不读开发机环境变量。断言四个前缀可 resolve；context 正确传入 apiKey／baseUrl／endpointPath／stream／`GenerateOptions`；DeepSeek、GLM、MiniMax 覆盖 enableThinking；Kimi reasoning 参数只走 GenerateOptions；四者 native structured output 默认 false；DeepSeek／MiniMax 不再使用 LiteFlow 旧 URL。

- [ ] **Step 2: 运行测试并确认 preset 实现失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=FirstPartyModelProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL，当前实现仍维护本地 compatible preset。

- [ ] **Step 3: 实现 Provider spec**

```java
public OpenAIProviderSpec(
        String providerId, String configKey, String modelName);
public OpenAIProviderSpec enableThinking(boolean enabled);
public OpenAIProviderSpec customizeContext(
        Consumer<ModelCreationContext.Builder> customizer);
protected Model buildModel(String modelId, ModelCreationContext context);
```

生产逻辑不得调用 `ModelRegistry.reset()`；`DeepSeek.of`、`GLM.of`、`Kimi.of`、`Minimax.of` 返回 `OpenAIProviderSpec`，并在 runtime 构建期只 resolve 一次。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-openai -am \
  -DskipTests=false -DskipITs \
  -Dtest=FirstPartyModelProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-openai
git commit -m "feat(agent): use first-party compatible model providers"
```

---

## Task 5: 迁移 Anthropic builder

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/Anthropic.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicCompatible.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicModelFactory.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/src/test/java/com/yomahub/liteflow/agent/anthropic/AnthropicSpecTest.java`

- [ ] **Step 1: 写 thinking、baseUrl 和 customizer 失败测试**

断言 baseUrl 传给 builder；`thinking.enabled(false)` 不遗留 thinkingBudget；enabled true 要求正 budget；只设置正 budget 时视为启用；formatter 与 builder customizer 生效；缺失 apiKey 失败。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-anthropic -am \
  -DskipTests=false -DskipITs \
  -Dtest=AnthropicSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 迁移到 `AnthropicChatModel`**

```java
public AnthropicSpec formatter(AnthropicBaseFormatter formatter);
public AnthropicSpec customizeBuilder(
        Consumer<AnthropicChatModel.Builder> customizer);
protected Model buildModel(String apiKey, String baseUrl);
```

使用 `io.agentscope.extensions.model.anthropic.*` 和 `.defaultOptions(options)`。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-anthropic -am \
  -DskipTests=false -DskipITs \
  -Dtest=AnthropicSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-anthropic
git commit -m "feat(agent): migrate Anthropic adapter to AgentScope 2"
```

---

## Task 6: 迁移 Gemini builder

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/Gemini.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiModelFactory.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/src/test/java/com/yomahub/liteflow/agent/gemini/GeminiSpecTest.java`

- [ ] **Step 1: 写 baseUrl、stream 和 thinking 失败测试**

断言读取 credential baseUrl；stream 映射到 `streamEnabled`；thinking.level 映射 `reasoningEffort`；thinking.budget 映射 `thinkingBudget`；formatter 与 customizer 生效；构造的 `GeminiChatModel` 在 finally 中 close。

- [ ] **Step 2: 运行测试并确认当前 baseUrl 缺陷**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-gemini -am \
  -DskipTests=false -DskipITs \
  -Dtest=GeminiSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 迁移到 `GeminiChatModel`**

```java
public GeminiSpec formatter(
        Formatter<Content, GenerateContentResponse,
                  GenerateContentConfig.Builder> formatter);
public GeminiSpec customizeBuilder(
        Consumer<GeminiChatModel.Builder> customizer);
protected Model buildModel(String apiKey, String baseUrl);
```

使用 `.streamEnabled(stream)` 与 `.defaultOptions(options)`。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-gemini -am \
  -DskipTests=false -DskipITs \
  -Dtest=GeminiSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-gemini
git commit -m "feat(agent): migrate Gemini adapter to AgentScope 2"
```

---

## Task 7: 迁移 DashScope builder

**Files:**

- Modify: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScope.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeModelFactory.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeSpec.java`
- Modify: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/src/test/java/com/yomahub/liteflow/agent/dashscope/DashScopeSpecTest.java`

- [ ] **Step 1: 写 baseUrl、thinking、structured output 失败测试**

断言读取 credential baseUrl；覆盖 stream、cacheControl、thinkingBudget；thinking.enabled(false) 调 `.enableThinking(false)` 并忽略 budget；formatter、native structured output 两个标志和 customizer 生效；缺失 apiKey 失败。

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-dashscope -am \
  -DskipTests=false -DskipITs \
  -Dtest=DashScopeSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL。

- [ ] **Step 3: 迁移到 `DashScopeChatModel`**

```java
public DashScopeSpec formatter(
        Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> formatter);
public DashScopeSpec nativeStructuredOutput(boolean enabled);
public DashScopeSpec nativeStructuredOutputWithTools(boolean enabled);
public DashScopeSpec customizeBuilder(
        Consumer<DashScopeChatModel.Builder> customizer);
protected Model buildModel(String apiKey, String baseUrl);
```

使用 `.stream(stream)`、`.enableThinking(enabled)` 和 `.defaultOptions(options)`。

- [ ] **Step 4: 运行测试并提交**

Run:

```bash
mvn test -pl liteflow-react-agent/liteflow-react-agent-dashscope -am \
  -DskipTests=false -DskipITs \
  -Dtest=DashScopeSpecTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS。

```bash
git add liteflow-react-agent/liteflow-react-agent-dashscope
git commit -m "feat(agent): migrate DashScope adapter to AgentScope 2"
```

Tasks 5、6、7 在 Task 2 完成后可并行执行，但每个任务只修改自己的 Provider 模块。

---

## Task 8: 隔离离线 Provider 契约与真实 smoke

**Files:**

- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/pom.xml`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/support/BaseAgentLiveTest.java`
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/model/ProviderClasspathContractTest.java`

- [ ] **Step 1: 写无网络 classpath 契约测试**

构造 OpenAI、OpenAI Compatible、DeepSeek、GLM、Kimi、MiniMax、Anthropic、Gemini、DashScope，读取 modelName／structured capability，并关闭所有 `AutoCloseable`。测试使用显式假 apiKey，不能读取环境变量或调用模型。

- [ ] **Step 2: 标记 live 测试并配置 profile**

`BaseAgentLiveTest` 增加可继承的 `@Tag("agentscope-live-smoke")`。默认 Surefire 排除该 tag；`agent-live` profile 用 `combine.self="override"` 只包含该 tag。即使开发机存在真实 API key，默认测试也不得发外网请求。

- [ ] **Step 3: 运行默认离线套件**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -DskipITs
```

Expected: PASS，不访问网络，不因缺少 credential 跳过 Provider classpath 契约。

- [ ] **Step 4: 在明确授权且有密钥时运行 live smoke**

Run:

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -DskipTests=false -Pagent-live
```

Expected: 有配置的 Provider 通过；未配置的 Provider 以现有 `LiveTestEnv` 条件跳过。未运行时交付报告必须写“未执行”，不能写通过。

- [ ] **Step 5: 提交测试分层**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent
git commit -m "test(agent): separate provider contracts from live smoke"
```

---

## Task 9: 验证依赖版本与运行时链接

**Files:**

- Modify if linkage test proves it necessary: `liteflow-react-agent/pom.xml`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/model/ProviderClasspathContractTest.java`

- [ ] **Step 1: 扩充运行时链接测试**

除构建 Model 外，调用每个 formatter／options 合并／structured capability 的无网络路径，主动触发 Jackson、Reactor、OkHttp、SLF4J 与厂商 SDK 的核心类加载，防止只编译成功却在运行时 `NoSuchMethodError`。

- [ ] **Step 2: 检查实际依赖树**

Run:

```bash
mvn dependency:tree -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -am \
  -Dincludes=io.agentscope:*,io.projectreactor:reactor-core,com.fasterxml.jackson.core:*,org.slf4j:slf4j-api,com.squareup.okhttp3:*,com.anthropic:anthropic-java,com.google.genai:google-genai,com.alibaba:dashscope-sdk-java \
  -Dverbose
```

AgentScope 2.0.2 发布图的参考版本是 Reactor 3.8.2、Jackson Databind 2.21.1、SLF4J 2.0.17、OkHttp 5.3.2、Anthropic Java 2.14.0、Google GenAI 1.45.0、DashScope SDK 2.22.9。

- [ ] **Step 3: 按测试证据处理版本冲突**

先保留 LiteFlow 根工程已有的跨模块约束并运行链接测试；不得为了数字一致直接全局升级 JDK 8／Spring Boot 2 模块。若链接测试出现 AgentScope 依赖缺失的方法或类，在 `liteflow-react-agent/pom.xml` 的局部 `dependencyManagement` 对对应库使用 AgentScope 2.0.2 BOM 的上述精确版本，再重新执行全部 Provider 和 Spring Boot testcase。不得用排除测试或捕获 `LinkageError` 绕过。

- [ ] **Step 4: 运行全部 Provider 测试和打包**

Run:

```bash
mvn test -pl \
liteflow-react-agent/liteflow-react-agent-core,\
liteflow-react-agent/liteflow-react-agent-openai,\
liteflow-react-agent/liteflow-react-agent-anthropic,\
liteflow-react-agent/liteflow-react-agent-gemini,\
liteflow-react-agent/liteflow-react-agent-dashscope \
  -am -DskipTests=false -DskipITs
mvn package -pl liteflow-react-agent -am -DskipTests
```

Expected: PASS，无网络测试全部执行；四个 Provider 模块可打包。

- [ ] **Step 5: 提交必要的版本收敛修复**

若产生修改：

```bash
git add liteflow-react-agent/pom.xml \
  liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/model/ProviderClasspathContractTest.java
git commit -m "fix(agent): align AgentScope provider runtime dependencies"
```

---

## Completion Criteria

- 四个 Provider 只依赖 AgentScope 2.0.2 对应细粒度 extension；Anthropic／Gemini SDK 不再由 LiteFlow 直接固定版本。
- OpenAI 使用 `.generateOptions(...)`，Anthropic／Gemini／DashScope 使用 `.defaultOptions(...)`，Gemini 使用 `.streamEnabled(...)`。
- DeepSeek、GLM、Kimi、MiniMax 通过官方 `ModelProvider` resolve，LiteFlow 不维护重复 URL preset。
- `ModelSpec` 公共 options 合并优先级有确定性测试，所有 builder customizer 最后执行。
- 调用期路由只能选择 runtime 已管理 Model，关闭 ownership 明确且不在请求路径创建客户端。
- 默认 Provider 套件完全离线；live smoke 只能通过 `-Pagent-live` 显式执行。
- classpath 链接测试覆盖 AgentScope 2.0.2 的主要传递依赖，相关模块在 JDK 17 上测试和打包成功。
