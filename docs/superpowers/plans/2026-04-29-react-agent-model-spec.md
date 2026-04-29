# ReAct Agent ModelSpec 重设计 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `ReActAgentComponent` 的模型声明 API 从「子类直接构造 agentscope `Model`」重构为「子类返回 vendor-neutral 的 `ModelSpec`、由框架根据 `AgentConfig` 解析 credential 并构造 `Model`」。

**Architecture:** 核心模块新增 `ModelSpec<SELF>` 抽象类（共性 setter + `resolve(AgentConfig)` SPI）和 `CredentialResolver` 帮助类。每个 provider 子模块新增以平台命名的入口类（如 `OpenAI`、`DeepSeek`、`Anthropic`、`Gemini`、`DashScope`）和对应的 `XxxSpec` 子类（暴露各家平台特异参数）。`ReActAgentComponent` 的 `buildModel` 由 `abstract` 降级为带默认实现的可选钩子，新抽象方法 `model(ctx)` 取而代之。

**Tech Stack:** Java 17、Maven 多模块、JUnit 5、AssertJ、agentscope-core SDK、Spring Boot（测试）。

参考设计文档：`docs/superpowers/specs/2026-04-29-react-agent-model-spec-design.md`

---

## 文件结构总览

### 新增文件

**core 模块** (`liteflow-react-agent/liteflow-react-agent-core`)
- `src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java` — 描述符基类（共性 setter + resolve SPI）
- `src/main/java/com/yomahub/liteflow/agent/model/CredentialResolver.java` — credential 解析帮助类

**openai 模块** (`liteflow-react-agent/liteflow-react-agent-openai`)
- `src/main/java/com/yomahub/liteflow/agent/openai/OpenAISpec.java` — OpenAI 共用 spec（reasoningEffort/frequency/presence）
- `src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpec.java` — OpenAI 兼容族 spec（不同的 credential 路径）
- `src/main/java/com/yomahub/liteflow/agent/openai/OpenAI.java` — 入口类
- `src/main/java/com/yomahub/liteflow/agent/openai/DeepSeek.java`
- `src/main/java/com/yomahub/liteflow/agent/openai/Kimi.java`
- `src/main/java/com/yomahub/liteflow/agent/openai/GLM.java`
- `src/main/java/com/yomahub/liteflow/agent/openai/Minimax.java`
- `src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatible.java` — 自定义兜底入口

**anthropic 模块** (`liteflow-react-agent/liteflow-react-agent-anthropic`)
- `src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicSpec.java`
- `src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicThinking.java`
- `src/main/java/com/yomahub/liteflow/agent/anthropic/Anthropic.java`
- `src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicCompatible.java`

**gemini 模块** (`liteflow-react-agent/liteflow-react-agent-gemini`)
- `src/main/java/com/yomahub/liteflow/agent/gemini/GeminiSpec.java`
- `src/main/java/com/yomahub/liteflow/agent/gemini/GeminiThinking.java`
- `src/main/java/com/yomahub/liteflow/agent/gemini/Gemini.java`

**dashscope 模块** (`liteflow-react-agent/liteflow-react-agent-dashscope`)
- `src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeSpec.java`
- `src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeThinking.java`
- `src/main/java/com/yomahub/liteflow/agent/dashscope/DashScope.java`

**测试**（`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/...`）
- `com/yomahub/liteflow/agent/model/ModelSpecTest.java`
- `com/yomahub/liteflow/agent/model/CredentialResolverTest.java`
- `com/yomahub/liteflow/agent/openai/OpenAIEntryTest.java`
- `com/yomahub/liteflow/agent/openai/OpenAICompatibleEntryTest.java`
- `com/yomahub/liteflow/agent/anthropic/AnthropicEntryTest.java`
- `com/yomahub/liteflow/agent/gemini/GeminiEntryTest.java`
- `com/yomahub/liteflow/agent/dashscope/DashScopeEntryTest.java`

### 修改文件

- `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java` — 抽象方法切换
- `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/DeepSeekAgentCmp.java`
- `.../cmp/OpenAIAgentCmp.java`
- `.../cmp/AnthropicAgentCmp.java`
- `.../cmp/GeminiAgentCmp.java`
- `.../cmp/DashScopeAgentCmp.java`
- `.../cmp/MathAgentCmp.java`（如有 `buildModel` 调用）

---

## Task 1: 在 core 中添加 `ModelSpec` 抽象基类

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/ModelSpecTest.java`

- [ ] **Step 1: 写失败测试 — `ModelSpec` 提供共性 fluent setter**

`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/ModelSpecTest.java`:

```java
package com.yomahub.liteflow.agent.model;

import io.agentscope.core.model.Model;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelSpecTest {

    /** 仅用于测试的最小 ModelSpec 子类。 */
    static class TestSpec extends ModelSpec<TestSpec> {
        @Override protected Model resolve(AgentConfig cfg) { return null; }
    }

    @Test
    void fluentSettersReturnSelfAndStoreValues() {
        TestSpec spec = new TestSpec()
                .temperature(0.5)
                .topP(0.9)
                .topK(40)
                .maxTokens(1024)
                .seed(42L)
                .stream(true)
                .cacheControl(true);

        assertEquals(0.5, spec.getTemperature());
        assertEquals(0.9, spec.getTopP());
        assertEquals(40, spec.getTopK());
        assertEquals(1024, spec.getMaxTokens());
        assertEquals(42L, spec.getSeed());
        assertEquals(Boolean.TRUE, spec.getStream());
        assertEquals(Boolean.TRUE, spec.getCacheControl());
    }

    @Test
    void unsetValuesReturnNull() {
        TestSpec spec = new TestSpec();
        assertNull(spec.getTemperature());
        assertNull(spec.getTopP());
        assertNull(spec.getTopK());
        assertNull(spec.getMaxTokens());
        assertNull(spec.getSeed());
        assertNull(spec.getStream());
        assertNull(spec.getCacheControl());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd /Users/bryan31/openSource/liteFlow
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=ModelSpecTest -DfailIfNoTests=false
```

期望：编译失败（`ModelSpec` 类不存在）。

- [ ] **Step 3: 实现 `ModelSpec`**

`liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java`:

```java
package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;

/**
 * Vendor-neutral 模型描述符。
 * <p>
 * 子类按平台命名（{@code OpenAISpec} / {@code AnthropicSpec} / 等），
 * 并暴露平台个性参数。共性参数（temperature、topP 等）在本基类提供。
 * <p>
 * {@link #resolve(AgentConfig)} 由各 provider 模块的子类实现：
 * 从 {@link AgentConfig} 取出 credential，把共性 + 个性参数翻译成
 * agentscope 的 {@code GenerateOptions}，并构造对应的 {@link Model}。
 *
 * @param <SELF> fluent self-type，便于子类链式调用保留具体类型
 */
public abstract class ModelSpec<SELF extends ModelSpec<SELF>> {

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Long seed;
    private Boolean stream;
    private Boolean cacheControl;

    @SuppressWarnings("unchecked")
    protected final SELF self() { return (SELF) this; }

    public SELF temperature(double v) { this.temperature = v; return self(); }
    public SELF topP(double v)        { this.topP = v;        return self(); }
    public SELF topK(int v)           { this.topK = v;        return self(); }
    public SELF maxTokens(int v)      { this.maxTokens = v;   return self(); }
    public SELF seed(long v)          { this.seed = v;        return self(); }
    public SELF stream(boolean v)     { this.stream = v;      return self(); }
    public SELF cacheControl(boolean v) { this.cacheControl = v; return self(); }

    public Double getTemperature()   { return temperature; }
    public Double getTopP()          { return topP; }
    public Integer getTopK()         { return topK; }
    public Integer getMaxTokens()    { return maxTokens; }
    public Long getSeed()            { return seed; }
    public Boolean getStream()       { return stream; }
    public Boolean getCacheControl() { return cacheControl; }

    /**
     * 把本描述符解析为 agentscope {@link Model} 实例。
     * 实现需从 {@link AgentConfig} 中读取对应平台的 credential，
     * 并把共性 + 个性参数翻译成 agentscope 的 GenerateOptions。
     */
    protected abstract Model resolve(AgentConfig cfg);
}
```

- [ ] **Step 4: 再次运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=ModelSpecTest -DfailIfNoTests=false
```

期望：2 tests pass。

- [ ] **Step 5: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/ModelSpec.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/ModelSpecTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-core): introduce ModelSpec base class

Vendor-neutral model descriptor with common fluent setters
(temperature/topP/topK/maxTokens/seed/stream/cacheControl) and an
abstract resolve(AgentConfig) SPI that provider modules implement.
EOF
)"
```

---

## Task 2: 在 core 中添加 `CredentialResolver`

为 provider 模块的 `resolve()` 提供统一的 credential 取值与错误信息。

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/CredentialResolver.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/CredentialResolverTest.java`

- [ ] **Step 1: 写失败测试**

`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/CredentialResolverTest.java`:

```java
package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialResolverTest {

    @Test
    void requireFirstClassReturnsCredentialWhenApiKeySet() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenai().setApiKey("sk-xxx");

        PlatformCredential c =
                CredentialResolver.requireFirstClass(cfg.getOpenai(), "liteflow.agent.openai");
        assertEquals("sk-xxx", c.getApiKey());
    }

    @Test
    void requireFirstClassThrowsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireFirstClass(
                        cfg.getOpenai(), "liteflow.agent.openai"));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"),
                "message should mention the config path");
    }

    @Test
    void requireCompatibleReturnsCredentialFromMap() {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("ds-key");
        cfg.getOpenaiCompatible().put("deepseek", cred);

        PlatformCredential c = CredentialResolver.requireCompatible(
                cfg.getOpenaiCompatible(), "deepseek", "liteflow.agent.openai-compatible");
        assertEquals("ds-key", c.getApiKey());
    }

    @Test
    void requireCompatibleThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        cfg.getOpenaiCompatible(), "deepseek",
                        "liteflow.agent.openai-compatible"));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.deepseek"));
    }

    @Test
    void requireCompatibleThrowsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenaiCompatible().put("deepseek", new PlatformCredential());

        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        cfg.getOpenaiCompatible(), "deepseek",
                        "liteflow.agent.openai-compatible"));
        assertTrue(ex.getMessage().contains("api-key"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=CredentialResolverTest -DfailIfNoTests=false
```

期望：编译失败。

- [ ] **Step 3: 实现 `CredentialResolver`**

`liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/CredentialResolver.java`:

```java
package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.PlatformCredential;

import java.util.Map;

/**
 * 帮助 provider 模块的 {@link ModelSpec#resolve} 实现统一地从
 * {@link com.yomahub.liteflow.property.agent.AgentConfig} 中取出
 * {@link PlatformCredential}，并在缺失时抛出带配置路径提示的
 * {@link AgentConfigException}。
 */
public final class CredentialResolver {

    private CredentialResolver() {}

    /**
     * 取头等平台 credential（如 openai / anthropic / gemini / dashscope）。
     *
     * @param cred       从 AgentConfig 中拿到的 credential 实例（可能为 null）
     * @param configPath 配置路径前缀，例如 "liteflow.agent.openai"，用于错误信息
     */
    public static PlatformCredential requireFirstClass(PlatformCredential cred, String configPath) {
        if (cred == null || isBlank(cred.getApiKey())) {
            throw new AgentConfigException(
                    "Missing API key: please configure " + configPath + ".api-key");
        }
        return cred;
    }

    /**
     * 取兼容 Map 中的 credential（如 openaiCompatible.deepseek）。
     *
     * @param map        兼容 Map（可能为 null 或缺 key）
     * @param key        平台 key，如 "deepseek"
     * @param configPath 配置路径前缀，例如 "liteflow.agent.openai-compatible"
     */
    public static PlatformCredential requireCompatible(
            Map<String, PlatformCredential> map, String key, String configPath) {
        PlatformCredential cred = (map == null) ? null : map.get(key);
        if (cred == null) {
            throw new AgentConfigException(
                    "Missing platform credential: please configure "
                            + configPath + "." + key + ".api-key");
        }
        if (isBlank(cred.getApiKey())) {
            throw new AgentConfigException(
                    "Missing API key: please configure "
                            + configPath + "." + key + ".api-key");
        }
        return cred;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=CredentialResolverTest -DfailIfNoTests=false
```

期望：5 tests pass。

- [ ] **Step 5: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/model/CredentialResolver.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/model/CredentialResolverTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-core): add CredentialResolver helper

Centralizes credential lookup with consistent AgentConfigException
messages that point at the relevant liteflow.agent.* config path.
EOF
)"
```

---

## Task 3: OpenAI 模块 — `OpenAISpec` 与 `OpenAI` 入口类

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAISpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAI.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAIEntryTest.java`

- [ ] **Step 1: 写失败测试**

`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAIEntryTest.java`:

```java
package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIEntryTest {

    @Test
    void buildsOpenAIChatModelWithGivenModelName() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenai().setApiKey("sk-test");

        OpenAISpec spec = OpenAI.of("gpt-4o").temperature(0.7);
        Model model = spec.resolve(cfg);

        assertTrue(model instanceof OpenAIChatModel);
        assertEquals("gpt-4o", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();   // openai credential not set
        OpenAISpec spec = OpenAI.of("gpt-4o");
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> spec.resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"));
    }

    @Test
    void specSettersReturnSubclassType() {
        // 编译期断言：fluent 链返回 OpenAISpec，能链式调用 OpenAI 特有方法
        OpenAISpec spec = OpenAI.of("gpt-4o")
                .temperature(0.7)
                .topP(0.9)
                .reasoningEffort("high")
                .frequencyPenalty(0.1)
                .presencePenalty(0.2);
        assertNotNull(spec);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=OpenAIEntryTest -DfailIfNoTests=false
```

期望：编译失败（`OpenAISpec` / `OpenAI` 不存在）。

- [ ] **Step 3: 实现 `OpenAISpec`**

`liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAISpec.java`:

```java
package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;

/**
 * OpenAI 系（含 OpenAI 兼容族）通用 spec。
 * 暴露 OpenAI 平台特有的 reasoningEffort / frequencyPenalty / presencePenalty 等参数。
 */
public class OpenAISpec extends ModelSpec<OpenAISpec> {

    private final String modelName;
    private String reasoningEffort;
    private Double frequencyPenalty;
    private Double presencePenalty;

    public OpenAISpec(String modelName) {
        this.modelName = modelName;
    }

    public OpenAISpec reasoningEffort(String level) { this.reasoningEffort = level; return this; }
    public OpenAISpec frequencyPenalty(double v)    { this.frequencyPenalty = v;   return this; }
    public OpenAISpec presencePenalty(double v)     { this.presencePenalty = v;    return this; }

    public String getModelName()         { return modelName; }
    public String getReasoningEffort()   { return reasoningEffort; }
    public Double getFrequencyPenalty()  { return frequencyPenalty; }
    public Double getPresencePenalty()   { return presencePenalty; }

    @Override
    protected Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getOpenai(), "liteflow.agent.openai");
        return buildModel(cred.getApiKey(), cred.getBaseUrl());
    }

    /** 子类（OpenAICompatibleSpec）可覆盖以提供不同 baseUrl / apiKey 来源。 */
    protected Model buildModel(String apiKey, String baseUrl) {
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.generateOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    /** 把共性 + 个性参数装配成 GenerateOptions；全部为 null 时返回 null。 */
    protected GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null
                && reasoningEffort == null
                && frequencyPenalty == null && presencePenalty == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (getCacheControl() != null) b.cacheControl(getCacheControl());
        if (reasoningEffort != null)   b.reasoningEffort(reasoningEffort);
        if (frequencyPenalty != null)  b.frequencyPenalty(frequencyPenalty);
        if (presencePenalty != null)   b.presencePenalty(presencePenalty);
        return b.build();
    }
}
```

- [ ] **Step 4: 实现 `OpenAI` 入口类**

`liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAI.java`:

```java
package com.yomahub.liteflow.agent.openai;

/**
 * OpenAI 官方 API 入口。credential 来源：{@code liteflow.agent.openai}。
 */
public final class OpenAI {

    private OpenAI() {}

    public static OpenAISpec of(String modelName) {
        return new OpenAISpec(modelName);
    }
}
```

- [ ] **Step 5: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=OpenAIEntryTest -DfailIfNoTests=false
```

期望：3 tests pass。

- [ ] **Step 6: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAISpec.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAI.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAIEntryTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-openai): add OpenAI entry and OpenAISpec

OpenAI.of(modelName) returns an OpenAISpec exposing reasoningEffort,
frequencyPenalty, and presencePenalty in addition to the common
ModelSpec setters; resolve() reads liteflow.agent.openai credential.
EOF
)"
```

---

## Task 4: OpenAI 模块 — `OpenAICompatibleSpec` 与各兼容厂商入口

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/DeepSeek.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Kimi.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/GLM.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Minimax.java`
- Create: `liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatible.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleEntryTest.java`

- [ ] **Step 1: 写失败测试**

`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleEntryTest.java`:

```java
package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAICompatibleEntryTest {

    private static AgentConfig cfgWith(String key, String apiKey) {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential c = new PlatformCredential();
        c.setApiKey(apiKey);
        cfg.getOpenaiCompatible().put(key, c);
        return cfg;
    }

    @Test
    void deepseekResolvesFromOpenaiCompatibleDeepseek() {
        AgentConfig cfg = cfgWith("deepseek", "ds-key");
        Model model = DeepSeek.of("deepseek-chat").temperature(0.7).resolve(cfg);
        assertTrue(model instanceof OpenAIChatModel);
        assertEquals("deepseek-chat", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void kimiResolvesFromOpenaiCompatibleKimi() {
        AgentConfig cfg = cfgWith("kimi", "kimi-key");
        Model model = Kimi.of("kimi-k2").resolve(cfg);
        assertEquals("kimi-k2", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void glmResolvesFromOpenaiCompatibleGlm() {
        AgentConfig cfg = cfgWith("glm", "glm-key");
        Model model = GLM.of("glm-4").resolve(cfg);
        assertEquals("glm-4", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void minimaxResolvesFromOpenaiCompatibleMinimax() {
        AgentConfig cfg = cfgWith("minimax", "mm-key");
        Model model = Minimax.of("abab6.5").resolve(cfg);
        assertEquals("abab6.5", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void customResolvesFromGivenConfigKey() {
        AgentConfig cfg = cfgWith("myvendor", "my-key");
        PlatformCredential c = cfg.getOpenaiCompatible().get("myvendor");
        c.setBaseUrl("https://my.vendor/v1");

        Model model = OpenAICompatible.custom("myvendor", "my-model").resolve(cfg);
        assertEquals("my-model", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void customThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> OpenAICompatible.custom("myvendor", "x").resolve(cfg));
        assertTrue(ex.getMessage().contains("openai-compatible.myvendor"));
    }

    @Test
    void deepseekThrowsWhenCredentialMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> DeepSeek.of("deepseek-chat").resolve(cfg));
        assertTrue(ex.getMessage().contains("openai-compatible.deepseek"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=OpenAICompatibleEntryTest -DfailIfNoTests=false
```

期望：编译失败。

- [ ] **Step 3: 实现 `OpenAICompatibleSpec`**

`liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpec.java`:

```java
package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.Model;

/**
 * OpenAI 兼容族 spec。与 {@link OpenAISpec} 共享所有可调参数，
 * 但 credential 来源换成 {@code liteflow.agent.openai-compatible.<configKey>}。
 * 子类内置默认 baseUrl，用户配置中的 baseUrl 优先生效。
 */
public class OpenAICompatibleSpec extends OpenAISpec {

    private final String configKey;
    private final String defaultBaseUrl;

    public OpenAICompatibleSpec(String configKey, String modelName, String defaultBaseUrl) {
        super(modelName);
        this.configKey = configKey;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @Override
    protected Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireCompatible(
                cfg.getOpenaiCompatible(), configKey, "liteflow.agent.openai-compatible");
        String baseUrl = (cred.getBaseUrl() != null && !cred.getBaseUrl().isBlank())
                ? cred.getBaseUrl()
                : defaultBaseUrl;
        return buildModel(cred.getApiKey(), baseUrl);
    }
}
```

- [ ] **Step 4: 实现各厂商入口类**

`DeepSeek.java`:

```java
package com.yomahub.liteflow.agent.openai;

public final class DeepSeek {
    private static final String CONFIG_KEY = "deepseek";
    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private DeepSeek() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
```

`Kimi.java`:

```java
package com.yomahub.liteflow.agent.openai;

public final class Kimi {
    private static final String CONFIG_KEY = "kimi";
    private static final String BASE_URL = "https://api.moonshot.cn/v1";
    private Kimi() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
```

`GLM.java`:

```java
package com.yomahub.liteflow.agent.openai;

public final class GLM {
    private static final String CONFIG_KEY = "glm";
    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private GLM() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
```

`Minimax.java`:

```java
package com.yomahub.liteflow.agent.openai;

public final class Minimax {
    private static final String CONFIG_KEY = "minimax";
    private static final String BASE_URL = "https://api.minimax.chat/v1";
    private Minimax() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
```

`OpenAICompatible.java`:

```java
package com.yomahub.liteflow.agent.openai;

/**
 * 自定义 OpenAI 兼容厂商兜底入口。
 * 用户在配置中挂 {@code liteflow.agent.openai-compatible.<configKey>}，
 * 至少要提供 api-key；base-url 也由用户配置决定（无默认值）。
 */
public final class OpenAICompatible {
    private OpenAICompatible() {}

    public static OpenAICompatibleSpec custom(String configKey, String modelName) {
        return new OpenAICompatibleSpec(configKey, modelName, null);
    }
}
```

- [ ] **Step 5: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=OpenAICompatibleEntryTest -DfailIfNoTests=false
```

期望：7 tests pass。

- [ ] **Step 6: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleSpec.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/DeepSeek.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Kimi.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/GLM.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/Minimax.java \
        liteflow-react-agent/liteflow-react-agent-openai/src/main/java/com/yomahub/liteflow/agent/openai/OpenAICompatible.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/openai/OpenAICompatibleEntryTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-openai): add OpenAI-compatible entries

DeepSeek/Kimi/GLM/Minimax entry classes return an OpenAICompatibleSpec
preconfigured with each vendor's default baseUrl and config key.
OpenAICompatible.custom(configKey, modelName) covers custom vendors.
EOF
)"
```

---

## Task 5: Anthropic 模块 — `AnthropicSpec`、`AnthropicThinking`、入口类

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicSpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/Anthropic.java`
- Create: `liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicCompatible.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/anthropic/AnthropicEntryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnthropicEntryTest {

    @Test
    void buildsAnthropicChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getAnthropic().setApiKey("ak-test");

        Model model = Anthropic.of("claude-sonnet-4-6")
                .temperature(0.5)
                .thinking(t -> t.budget(2000).enabled(true))
                .resolve(cfg);

        assertTrue(model instanceof AnthropicChatModel);
        assertEquals("claude-sonnet-4-6", ((AnthropicChatModel) model).getModelName());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> Anthropic.of("claude-sonnet-4-6").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.anthropic.api-key"));
    }

    @Test
    void compatibleResolvesFromAnthropicCompatibleMap() {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential c = new PlatformCredential();
        c.setApiKey("anc-key");
        c.setBaseUrl("https://my.anthropic-mirror/v1");
        cfg.getAnthropicCompatible().put("mirror", c);

        Model model = AnthropicCompatible.custom("mirror", "claude-haiku")
                .resolve(cfg);
        assertEquals("claude-haiku", ((AnthropicChatModel) model).getModelName());
    }

    @Test
    void compatibleThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> AnthropicCompatible.custom("mirror", "x").resolve(cfg));
        assertTrue(ex.getMessage().contains("anthropic-compatible.mirror"));
    }

    @Test
    void thinkingBuilderStoresBudgetAndEnabled() {
        AnthropicSpec spec = Anthropic.of("claude-sonnet-4-6")
                .thinking(t -> t.budget(1500).enabled(true));
        assertEquals(1500, spec.getThinkingBudget());
        assertEquals(Boolean.TRUE, spec.getThinkingEnabled());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=AnthropicEntryTest -DfailIfNoTests=false
```

期望：编译失败。

- [ ] **Step 3: 实现 `AnthropicThinking`**

```java
package com.yomahub.liteflow.agent.anthropic;

/** Anthropic 平台 thinking 子构建器。沿用 Anthropic 原生术语。 */
public final class AnthropicThinking {
    private Integer budget;
    private Boolean enabled;

    public AnthropicThinking budget(int tokens) { this.budget = tokens; return this; }
    public AnthropicThinking enabled(boolean v) { this.enabled = v;     return this; }

    public Integer getBudget() { return budget; }
    public Boolean getEnabled() { return enabled; }
}
```

- [ ] **Step 4: 实现 `AnthropicSpec`**

```java
package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class AnthropicSpec extends ModelSpec<AnthropicSpec> {

    private final String modelName;
    private Integer thinkingBudget;
    private Boolean thinkingEnabled;

    /** null 表示走头等平台 (cfg.getAnthropic())；非 null 表示走 anthropic-compatible map。 */
    private final String compatibleConfigKey;

    public AnthropicSpec(String modelName) {
        this(modelName, null);
    }

    public AnthropicSpec(String modelName, String compatibleConfigKey) {
        this.modelName = modelName;
        this.compatibleConfigKey = compatibleConfigKey;
    }

    public AnthropicSpec thinking(Consumer<AnthropicThinking> c) {
        AnthropicThinking t = new AnthropicThinking();
        c.accept(t);
        this.thinkingBudget = t.getBudget();
        this.thinkingEnabled = t.getEnabled();
        return this;
    }

    public String getModelName()         { return modelName; }
    public Integer getThinkingBudget()   { return thinkingBudget; }
    public Boolean getThinkingEnabled()  { return thinkingEnabled; }

    @Override
    protected Model resolve(AgentConfig cfg) {
        PlatformCredential cred;
        if (compatibleConfigKey == null) {
            cred = CredentialResolver.requireFirstClass(
                    cfg.getAnthropic(), "liteflow.agent.anthropic");
        } else {
            cred = CredentialResolver.requireCompatible(
                    cfg.getAnthropicCompatible(), compatibleConfigKey,
                    "liteflow.agent.anthropic-compatible");
        }

        AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        if (cred.getBaseUrl() != null && !cred.getBaseUrl().isBlank()) {
            builder.baseUrl(cred.getBaseUrl());
        }
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.generateOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null
                && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (getCacheControl() != null) b.cacheControl(getCacheControl());
        if (thinkingBudget != null)    b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
```

- [ ] **Step 5: 实现 `Anthropic` 与 `AnthropicCompatible`**

```java
package com.yomahub.liteflow.agent.anthropic;

public final class Anthropic {
    private Anthropic() {}
    public static AnthropicSpec of(String modelName) {
        return new AnthropicSpec(modelName);
    }
}
```

```java
package com.yomahub.liteflow.agent.anthropic;

public final class AnthropicCompatible {
    private AnthropicCompatible() {}
    public static AnthropicSpec custom(String configKey, String modelName) {
        return new AnthropicSpec(modelName, configKey);
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=AnthropicEntryTest -DfailIfNoTests=false
```

期望：5 tests pass。

- [ ] **Step 7: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicThinking.java \
        liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicSpec.java \
        liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/Anthropic.java \
        liteflow-react-agent/liteflow-react-agent-anthropic/src/main/java/com/yomahub/liteflow/agent/anthropic/AnthropicCompatible.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/anthropic/AnthropicEntryTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-anthropic): add Anthropic entry and AnthropicSpec

Anthropic.of(modelName) and AnthropicCompatible.custom(configKey,
modelName) return AnthropicSpec, with a thinking sub-builder using
Anthropic's native budget/enabled terms.
EOF
)"
```

---

## Task 6: Gemini 模块 — `GeminiSpec`、`GeminiThinking`、`Gemini`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiSpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/Gemini.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/gemini/GeminiEntryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiEntryTest {

    @Test
    void buildsGeminiChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getGemini().setApiKey("g-key");

        Model model = Gemini.of("gemini-2.5-pro")
                .temperature(0.6)
                .thinking(t -> t.level("high"))
                .resolve(cfg);

        assertTrue(model instanceof GeminiChatModel);
        assertEquals("gemini-2.5-pro", ((GeminiChatModel) model).getModelName());
    }

    @Test
    void thinkingLevelAndBudgetStored() {
        GeminiSpec spec = Gemini.of("gemini-2.5-pro")
                .thinking(t -> t.level("medium").budget(1024));
        assertEquals("medium", spec.getThinkingLevel());
        assertEquals(1024, spec.getThinkingBudget());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> Gemini.of("gemini-2.5-pro").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.gemini.api-key"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=GeminiEntryTest -DfailIfNoTests=false
```

期望：编译失败。

- [ ] **Step 3: 实现 `GeminiThinking`**

```java
package com.yomahub.liteflow.agent.gemini;

/**
 * Gemini 平台 thinking 子构建器。
 * Gemini 2.5 使用 "thinking_level"（low/medium/high），老接口用 "thinking_budget"（token 数）。
 */
public final class GeminiThinking {
    private String level;
    private Integer budget;

    public GeminiThinking level(String level)   { this.level = level;   return this; }
    public GeminiThinking budget(int tokens)    { this.budget = tokens; return this; }

    public String  getLevel()  { return level; }
    public Integer getBudget() { return budget; }
}
```

- [ ] **Step 4: 实现 `GeminiSpec`**

```java
package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class GeminiSpec extends ModelSpec<GeminiSpec> {

    private final String modelName;
    private String  thinkingLevel;
    private Integer thinkingBudget;

    public GeminiSpec(String modelName) { this.modelName = modelName; }

    public GeminiSpec thinking(Consumer<GeminiThinking> c) {
        GeminiThinking t = new GeminiThinking();
        c.accept(t);
        this.thinkingLevel  = t.getLevel();
        this.thinkingBudget = t.getBudget();
        return this;
    }

    public String  getModelName()      { return modelName; }
    public String  getThinkingLevel()  { return thinkingLevel; }
    public Integer getThinkingBudget() { return thinkingBudget; }

    @Override
    protected Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getGemini(), "liteflow.agent.gemini");

        GeminiChatModel.Builder builder = GeminiChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.generateOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null
                && thinkingLevel == null && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)   b.temperature(getTemperature());
        if (getTopP() != null)          b.topP(getTopP());
        if (getTopK() != null)          b.topK(getTopK());
        if (getMaxTokens() != null)     b.maxTokens(getMaxTokens());
        if (getSeed() != null)          b.seed(getSeed());
        if (getCacheControl() != null)  b.cacheControl(getCacheControl());
        if (thinkingLevel != null)      b.reasoningEffort(thinkingLevel);   // 复用 GenerateOptions.reasoningEffort 字段承载 level
        if (thinkingBudget != null)     b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
```

> **说明**：`GenerateOptions` 没有专属 `thinkingLevel`，但其 `reasoningEffort`（"low"/"medium"/"high"）语义恰好对应 Gemini 2.5 的 `thinking_level`，故在 spec → options 翻译时复用该字段。如果未来 agentscope 引入专属字段，只需在此处替换。

- [ ] **Step 5: 实现 `Gemini`**

```java
package com.yomahub.liteflow.agent.gemini;

public final class Gemini {
    private Gemini() {}
    public static GeminiSpec of(String modelName) {
        return new GeminiSpec(modelName);
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=GeminiEntryTest -DfailIfNoTests=false
```

期望：3 tests pass。

- [ ] **Step 7: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiThinking.java \
        liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/GeminiSpec.java \
        liteflow-react-agent/liteflow-react-agent-gemini/src/main/java/com/yomahub/liteflow/agent/gemini/Gemini.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/gemini/GeminiEntryTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-gemini): add Gemini entry and GeminiSpec

Gemini.of(modelName) returns GeminiSpec exposing a thinking sub-builder
that supports both Gemini 2.5's level() and the legacy budget() form.
EOF
)"
```

---

## Task 7: DashScope 模块 — `DashScopeSpec`、`DashScopeThinking`、`DashScope`

**Files:**
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeThinking.java`
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeSpec.java`
- Create: `liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScope.java`
- Test: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/dashscope/DashScopeEntryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeEntryTest {

    @Test
    void buildsDashScopeChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getDashscope().setApiKey("ds-key");

        Model model = DashScope.of("qwen-max")
                .temperature(0.4)
                .thinking(t -> t.budget(2048))
                .resolve(cfg);

        assertTrue(model instanceof DashScopeChatModel);
        assertEquals("qwen-max", ((DashScopeChatModel) model).getModelName());
    }

    @Test
    void thinkingBudgetStored() {
        DashScopeSpec spec = DashScope.of("qwen-max")
                .thinking(t -> t.budget(1024));
        assertEquals(1024, spec.getThinkingBudget());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> DashScope.of("qwen-max").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.dashscope.api-key"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=DashScopeEntryTest -DfailIfNoTests=false
```

期望：编译失败。

- [ ] **Step 3: 实现 `DashScopeThinking`**

```java
package com.yomahub.liteflow.agent.dashscope;

/** DashScope（通义千问）thinking 子构建器，沿用 thinking_budget 术语。 */
public final class DashScopeThinking {
    private Integer budget;

    public DashScopeThinking budget(int tokens) { this.budget = tokens; return this; }
    public Integer getBudget() { return budget; }
}
```

- [ ] **Step 4: 实现 `DashScopeSpec`**

```java
package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class DashScopeSpec extends ModelSpec<DashScopeSpec> {

    private final String modelName;
    private Integer thinkingBudget;

    public DashScopeSpec(String modelName) { this.modelName = modelName; }

    public DashScopeSpec thinking(Consumer<DashScopeThinking> c) {
        DashScopeThinking t = new DashScopeThinking();
        c.accept(t);
        this.thinkingBudget = t.getBudget();
        return this;
    }

    public String  getModelName()      { return modelName; }
    public Integer getThinkingBudget() { return thinkingBudget; }

    @Override
    protected Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getDashscope(), "liteflow.agent.dashscope");

        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.generateOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (getCacheControl() != null) b.cacheControl(getCacheControl());
        if (thinkingBudget != null)    b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
```

- [ ] **Step 5: 实现 `DashScope`**

```java
package com.yomahub.liteflow.agent.dashscope;

public final class DashScope {
    private DashScope() {}
    public static DashScopeSpec of(String modelName) {
        return new DashScopeSpec(modelName);
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -Dtest=DashScopeEntryTest -DfailIfNoTests=false
```

期望：3 tests pass。

- [ ] **Step 7: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeThinking.java \
        liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScopeSpec.java \
        liteflow-react-agent/liteflow-react-agent-dashscope/src/main/java/com/yomahub/liteflow/agent/dashscope/DashScope.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/agent/dashscope/DashScopeEntryTest.java
git commit -m "$(cat <<'EOF'
feat(react-agent-dashscope): add DashScope entry and DashScopeSpec

DashScope.of(modelName) returns DashScopeSpec with a thinking
sub-builder using DashScope's native budget term.
EOF
)"
```

---

## Task 8: 切换 `ReActAgentComponent` 抽象方法

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`

- [ ] **Step 1: 修改 `ReActAgentComponent`**

把 `protected abstract Model buildModel(ReActAgentContext ctx);` 替换为：

```java
    /**
     * 构建本组件使用的模型描述符。子类按"哪个平台 + 哪个模型 + 可选高级参数"
     * 三段式给出，由框架负责从 {@link AgentConfig} 解析 credential 并构造
     * agentscope {@link Model}。例如：
     * <pre>{@code
     *     return DeepSeek.of("deepseek-chat").temperature(0.7);
     * }</pre>
     */
    protected abstract ModelSpec<?> model(ReActAgentContext ctx);

    /**
     * Escape hatch：高级用户可整体绕过 {@link ModelSpec} 自行构造 {@link Model}。
     * 默认实现委派给 {@code model(ctx).resolve(agentConfig())}，无需覆写。
     */
    protected Model buildModel(ReActAgentContext ctx) {
        return model(ctx).resolve(agentConfig());
    }
```

需要新增 import：

```java
import com.yomahub.liteflow.agent.model.ModelSpec;
```

`buildAgent(ctx)` 内部对 `buildModel(ctx)` 的调用保持不变。

- [ ] **Step 2: 编译 core 模块**

```bash
mvn -q -DskipTests compile -pl liteflow-react-agent/liteflow-react-agent-core -am
```

期望：编译通过。

- [ ] **Step 3: 提交**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java
git commit -m "$(cat <<'EOF'
refactor(react-agent-core): switch abstract from buildModel to model

ReActAgentComponent now requires subclasses to override model(ctx)
returning a ModelSpec. The legacy buildModel(ctx) becomes a
non-abstract escape hatch with a default implementation delegating
to model(ctx).resolve(agentConfig()).
EOF
)"
```

---

## Task 9: 迁移测试组件到新 API

**Files:**
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/DeepSeekAgentCmp.java`
- Modify: `.../OpenAIAgentCmp.java`
- Modify: `.../AnthropicAgentCmp.java`
- Modify: `.../GeminiAgentCmp.java`
- Modify: `.../DashScopeAgentCmp.java`
- Modify: `.../MathAgentCmp.java`（如使用 `buildModel` 则一并迁移）

- [ ] **Step 1: 迁移 `DeepSeekAgentCmp`**

完整替换为：

```java
package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("deepseekAgent")
public class DeepSeekAgentCmp extends ReActAgentComponent {

    @Value("${test.deepseek.model:deepseek-chat}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DeepSeek.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是一名简洁的中文助理，回答严格控制在两句话以内。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}
```

- [ ] **Step 2: 迁移 `GeminiAgentCmp`**

完整替换为：

```java
package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("geminiAgent")
public class GeminiAgentCmp extends ReActAgentComponent {

    @Value("${test.gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return Gemini.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "You are a concise assistant. Answer in Chinese, one sentence.";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}
```

- [ ] **Step 3: 迁移 `OpenAIAgentCmp`**

先 `Read` 该文件，按相同模式迁移：

```bash
cat liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/OpenAIAgentCmp.java
```

把 `protected Model buildModel(ReActAgentContext ctx) { ... OpenAIModelFactory.openai(...) ... }` 替换为：

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return OpenAI.of(modelName);
}
```

并新增 import：

```java
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAI;
```

删除原 `import io.agentscope.core.model.Model;` 与 `import com.yomahub.liteflow.agent.openai.OpenAIModelFactory;`（如不再使用）。

- [ ] **Step 4: 迁移 `AnthropicAgentCmp`**

先 `Read`，把 `buildModel` 替换为：

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return Anthropic.of(modelName);
}
```

import：

```java
import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.model.ModelSpec;
```

删除 `import com.yomahub.liteflow.agent.anthropic.AnthropicModelFactory;` / `import io.agentscope.core.model.Model;` 等不再使用的 import。

- [ ] **Step 5: 迁移 `DashScopeAgentCmp`**

```java
@Override
protected ModelSpec<?> model(ReActAgentContext ctx) {
    return DashScope.of(modelName);
}
```

import：

```java
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.model.ModelSpec;
```

删除不再使用的旧 import。

- [ ] **Step 6: 检查 `MathAgentCmp` 是否使用 `buildModel`**

```bash
grep -n "buildModel" liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/MathAgentCmp.java || echo "no buildModel usage"
```

若有 `buildModel`，按对应平台的入口类同样迁移到 `model(ctx)`。

- [ ] **Step 7: 编译并跑全部测试**

```bash
mvn -q test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent
```

期望：所有 react-agent 模块的测试（含已有 + 新加）全部通过。

如果遇到编译报错（残留旧 import 等），按错误信息逐个修复后重跑。

- [ ] **Step 8: 提交**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/
git commit -m "$(cat <<'EOF'
test(react-agent): migrate test cmps to ModelSpec API

Replaces buildModel(ctx) overrides with model(ctx) returning
DeepSeek/OpenAI/Anthropic/Gemini/DashScope spec instances.
EOF
)"
```

---

## Task 10: 终态校验

确保整个项目编译通过、所有测试通过。

- [ ] **Step 1: 全量编译**

```bash
cd /Users/bryan31/openSource/liteFlow
mvn -q -DskipTests package
```

期望：BUILD SUCCESS。

- [ ] **Step 2: 跑 react-agent 相关模块测试**

```bash
mvn -q test \
    -pl liteflow-react-agent/liteflow-react-agent-core,liteflow-react-agent/liteflow-react-agent-openai,liteflow-react-agent/liteflow-react-agent-anthropic,liteflow-react-agent/liteflow-react-agent-gemini,liteflow-react-agent/liteflow-react-agent-dashscope,liteflow-testcase-el/liteflow-testcase-el-react-agent \
    -am
```

期望：所有测试通过。

- [ ] **Step 3: 二次确认无残留 `buildModel` 抽象使用**

```bash
grep -rn "extends ReActAgentComponent" liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java | while read line; do
    file=$(echo "$line" | awk -F: '{print $1}')
    if ! grep -q "protected ModelSpec" "$file"; then
        echo "FAIL: $file does not override model(ctx)"
    fi
done
echo "scan done"
```

期望：仅输出 "scan done"，没有 FAIL。

- [ ] **Step 4: 验证 javax-pro 脚本场景**

确认入口类（如 `DeepSeek`、`Gemini`）确实是 `public final class` 且方法 `public static`，能在脚本里 import 直接调用。可以 spot-check：

```bash
grep -E "public (final )?class (OpenAI|DeepSeek|Kimi|GLM|Minimax|Anthropic|AnthropicCompatible|Gemini|DashScope|OpenAICompatible)" \
    liteflow-react-agent/*/src/main/java/com/yomahub/liteflow/agent/*/*.java
```

期望：每个入口类一条匹配，且都是 `public`。

- [ ] **Step 5: 终态提交**

如果前面任务的修改全部已经各自 commit，本步骤无需新增提交。运行：

```bash
git status
```

确认 working tree clean。

---

## 完成标志

- 所有 10 个 task 的所有 step 全部勾选；
- `mvn -DskipTests package` 通过；
- `liteflow-testcase-el-react-agent` 全部测试通过；
- 测试组件（`DeepSeekAgentCmp` 等）均使用 `model(ctx)` 而非 `buildModel(ctx)`；
- `git status` 显示 clean。
