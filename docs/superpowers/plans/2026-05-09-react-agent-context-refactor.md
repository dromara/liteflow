# ReActAgentContext 重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `ReActAgentComponent` 的所有 hook 方法重构为无参签名，通过 `this.ctx()` 动态获取当次执行的 `ReActAgentContext`；该 ctx 通过 Slot 的通用 attachment 机制挂载，按 nodeId 隔离以支持 WHEN 并发。

**Architecture:** 在 `Slot` 上新增 4 个通用 KV API（`setAttachment / getAttachment / removeAttachment / hasAttachment`，复用已有 `metaDataMap`）。`ReActAgentComponent.process()` 入口将当次 `ReActAgentContext` 用 `_react_agent_ctx_<nodeId>` 作为 key 挂到 slot，finally 兜底解绑。新增 `protected final ReActAgentContext ctx()` 访问器，所有 hook 方法（`tools/hooks/systemPrompt/userPrompt/model/handleReply/resolveConversationId`）改为无参，内部需要 ctx 时调 `ctx()`。`buildAgent()` 不再接收 ctx 参数，自身从 `ctx()` 取构建期不变量按值使用。

**Tech Stack:** Java 17, Maven, JUnit 5, Spring Boot, alibaba TransmittableThreadLocal (现有)。

**关键引用:**
- 设计文档：`docs/superpowers/specs/2026-05-09-react-agent-context-refactor-design.md`
- Slot 现有 `metaDataMap` 私有字段：`liteflow-core/src/main/java/com/yomahub/liteflow/slot/Slot.java:93`
- Node 的 slotIndex TTL：`liteflow-core/src/main/java/com/yomahub/liteflow/flow/element/Node.java:86`
- NodeComponent.getSlot()：`liteflow-core/src/main/java/com/yomahub/liteflow/core/NodeComponent.java:262`

---

## Task 1: 在 Slot 上新增 attachment API

**Files:**
- Modify: `liteflow-core/src/main/java/com/yomahub/liteflow/slot/Slot.java`
- Test: `liteflow-core/src/test/java/com/yomahub/liteflow/test/slot/SlotAttachmentTest.java`（新建）

**目标:** 在 Slot 上加 4 个 public 方法用作通用 KV 挂载入口，不引入插件特定概念。

- [ ] **Step 1.1: 写失败测试**

新建 `liteflow-core/src/test/java/com/yomahub/liteflow/test/slot/SlotAttachmentTest.java`：

```java
package com.yomahub.liteflow.test.slot;

import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotAttachmentTest {

    @Test
    public void setAndGetAttachment_roundTrip() {
        Slot slot = new Slot();
        slot.setAttachment("foo", "bar");
        String v = slot.getAttachment("foo");
        assertEquals("bar", v);
    }

    @Test
    public void getAttachment_missingKey_returnsNull() {
        Slot slot = new Slot();
        Object v = slot.getAttachment("absent");
        assertNull(v);
    }

    @Test
    public void hasAttachment_reportsPresence() {
        Slot slot = new Slot();
        assertFalse(slot.hasAttachment("k"));
        slot.setAttachment("k", 1);
        assertTrue(slot.hasAttachment("k"));
    }

    @Test
    public void removeAttachment_clearsValue() {
        Slot slot = new Slot();
        slot.setAttachment("k", 1);
        slot.removeAttachment("k");
        assertFalse(slot.hasAttachment("k"));
        assertNull(slot.getAttachment("k"));
    }

    @Test
    public void setAttachment_nullValue_throws() {
        Slot slot = new Slot();
        // 复用 Slot 现有 putMetaDataMap 的 null 校验：null value 触发 NullParamException
        assertThrows(RuntimeException.class, () -> slot.setAttachment("k", null));
    }

    @Test
    public void getAttachment_genericTypeInference() {
        Slot slot = new Slot();
        slot.setAttachment("num", 42);
        Integer n = slot.getAttachment("num");
        assertEquals(42, n);
    }
}
```

- [ ] **Step 1.2: 运行测试，确认失败**

Run: `mvn test -pl liteflow-core -Dtest=SlotAttachmentTest`
Expected: 编译失败（`setAttachment / getAttachment / hasAttachment / removeAttachment` 方法不存在）。

- [ ] **Step 1.3: 在 Slot 上加 attachment API**

在 `Slot.java` 中找到现有 `putMetaDataMap` 私有方法（约第 139 行），在其后添加 public attachment API。把新增方法放在该私有方法之后、`getInput(...)` 之前：

```java
/**
 * 通用挂载点：写入一个由调用方约定 key 的对象。Slot 不感知 value 的具体类型，
 * 用于需要与 slot 同生命周期的运行时上下文挂载（例如插件级 per-invocation context）。
 *
 * @param key 调用方负责选用具有充分前缀的 key，避免与 Slot 内部保留 key 冲突
 *            （Slot 内部以下划线开头，如 {@code _request_}/{@code _response_}）
 * @throws com.yomahub.liteflow.exception.NullParamException value 为 null 时抛出
 */
public <T> void setAttachment(String key, T value) {
    putMetaDataMap(key, value);
}

/**
 * 读取由 {@link #setAttachment(String, Object)} 写入的值，缺失时返回 {@code null}。
 */
@SuppressWarnings("unchecked")
public <T> T getAttachment(String key) {
    return (T) metaDataMap.get(key);
}

public boolean hasAttachment(String key) {
    return metaDataMap.containsKey(key);
}

public void removeAttachment(String key) {
    metaDataMap.remove(key);
}
```

- [ ] **Step 1.4: 运行测试，确认通过**

Run: `mvn test -pl liteflow-core -Dtest=SlotAttachmentTest`
Expected: 6 个测试全部 PASS。

- [ ] **Step 1.5: 跑 liteflow-core 全量回归**

Run: `mvn test -pl liteflow-core`
Expected: 全部通过，无回归（attachment API 只是新增，不改动现有逻辑）。

- [ ] **Step 1.6: 提交**

```bash
git add liteflow-core/src/main/java/com/yomahub/liteflow/slot/Slot.java \
        liteflow-core/src/test/java/com/yomahub/liteflow/test/slot/SlotAttachmentTest.java
git commit -m "feat(core): add generic attachment API to Slot

为后续 per-invocation 插件上下文挂载提供通用 KV 入口。
复用已有 metaDataMap，包含 set/get/has/remove 四个 public 方法。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 重构 ReActAgentComponent 为无参 hook

**Files:**
- Modify: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`

**目标:** 把所有 hook 改成无参，新增 `ctx()` 访问器，`process()` 在 slot 上绑/解绑 ctx，`buildAgent()` 去参化。

**重要：** 这一步会**编译失败下游测试 cmp 子类**（StubReActAgentCmp 等共 7 个），下一个 Task 会修复它们。

- [ ] **Step 2.1: 整体替换 ReActAgentComponent.java**

完整覆盖 `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`：

```java
package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.hook.ReActLoggingHook;
import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.util.ConversationIdGenerator;
import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import com.yomahub.liteflow.agent.model.ModelSpec;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 封装 agentscope ReActAgent 的 LiteFlow 抽象组件。
 * <p>
 * 子类必须提供 {@link #model()}、{@link #systemPrompt()} 和 {@link #userPrompt()}。
 * 可选覆写方法用于自定义工具、钩子和生命周期回调。
 *
 * <p>所有 hook 方法均为无参——通过 {@link #ctx()} 动态获取当次执行的
 * {@link ReActAgentContext}。该 ctx 与 {@link Slot} 同生命周期，由 {@link #process()}
 * 自动挂载与解绑，按 {@code nodeId} 隔离以支持同 chain 内 WHEN 并发执行多个 agent。
 *
 * <p>会话标识被拆为两层：
 * <ul>
 *   <li>{@code conversationId}：业务/对话维度，决定 workspace 共享范围与对话连续性。
 *       由 {@link #resolveConversationId()} 解析，整条 chain 内只解析一次，
 *       后续 agent 通过 {@link Slot#getConversationId()} 复用。</li>
 *   <li>{@code agentKey}：组件维度，默认 {@code nodeId}，用于在同一段对话中区分
 *       不同 agent 的 ReActAgent 实例与对话记忆。</li>
 * </ul>
 *
 * <p><b>注意：勿在跨 invocation 缓存的对象（自定义工具/Hook/Model 等）中持有
 * {@link ReActAgentContext} 引用</b>——这些对象会被缓存的 agent 复用，捕获的 ctx
 * 会在下一次 {@code process()} 时变成陈旧引用（其中的 slot 已经被回收）。
 * 正确做法：持有组件实例引用，运行时通过 {@code component.ctx()} 动态获取。
 *
 * <p>{@link #process()} 方法被声明为 {@code final}，由框架统一保证 session
 * 管理和 ctx 生命周期的正确性。
 */
public abstract class ReActAgentComponent extends NodeComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ReActAgentComponent.class);

    /** 默认从 chain requestData 中读取 conversationId 时使用的约定 key。 */
    public static final String CONVERSATION_ID_REQUEST_KEY = "conversationId";

    /** 在 Slot attachment 上存储 ctx 时使用的 key 前缀，按 nodeId 隔离。 */
    private static final String CTX_KEY_PREFIX = "_react_agent_ctx_";

    private String ctxKey() {
        String nodeId = getNodeId();
        return CTX_KEY_PREFIX + (nodeId == null ? "default" : nodeId);
    }

    /* ===== 框架提供的 final 访问器 ===== */

    /**
     * 返回当前 LiteflowConfig 中的 agent 配置段。
     *
     * @throws AgentConfigException 当 liteflow.agent 尚未配置时抛出
     */
    protected final AgentConfig agentConfig() {
        AgentConfig c = LiteflowConfigGetter.get().getAgent();
        if (c == null) {
            throw new AgentConfigException(
                    "LiteflowConfig.agent is null; configure liteflow.agent.* or setAgent() before use");
        }
        return c;
    }

    /**
     * 返回当次执行的 {@link ReActAgentContext}。
     * 必须在 {@link #process()} 执行期间内调用（包括由 process() 触发的工具回调内）。
     *
     * @throws IllegalStateException 在 process() 生命周期之外调用时抛出
     */
    protected final ReActAgentContext ctx() {
        Slot slot = getSlot();
        ReActAgentContext c = slot.getAttachment(ctxKey());
        if (c == null) {
            throw new IllegalStateException(
                    "ReActAgentContext is not bound on slot; ctx() must be called during process()");
        }
        return c;
    }

    /* ===== 必须实现 ===== */

    /**
     * 构建本组件使用的模型描述符。子类按"哪个平台 + 哪个模型 + 可选高级参数"
     * 三段式给出，由框架负责从 {@link AgentConfig} 解析 credential 并构造
     * agentscope {@link Model}。例如：
     * <pre>{@code
     *     return DeepSeek.of("deepseek-chat").temperature(0.7);
     * }</pre>
     */
    protected abstract ModelSpec<?> model();

    /**
     * Escape hatch：高级用户可整体绕过 {@link ModelSpec} 自行构造 {@link Model}。
     * 默认实现委派给 {@code model().resolve(agentConfig())}，无需覆写。
     */
    protected Model buildModel() {
        return model().resolve(agentConfig());
    }

    /**
     * 返回 agent 的系统提示词。构建 agent 时只调用一次。
     */
    protected abstract String systemPrompt();

    /**
     * 返回本次执行的用户提示词。每次 {@link #process()} 都会调用。
     */
    protected abstract String userPrompt();

    /* ===== 可选覆写 ===== */

    /**
     * 提供要注册到 agent {@link Toolkit} 中的额外工具对象。
     * 对象中标注了 {@code @Tool} 的方法会被自动发现。
     * 默认返回空列表。
     */
    protected List<Object> tools() { return List.of(); }

    /**
     * 解析本次执行的 {@code conversationId}。该值代表"业务/对话"维度，整条 chain
     * 内的所有 agent 共享同一个值（即同一个 workspace 目录、可被持久化恢复）。
     *
     * <p>默认实现的解析顺序：
     * <ol>
     *   <li>{@code slot.getConversationId()} 已经存在（同 chain 内前序 agent 写入）→ 直接复用；</li>
     *   <li>{@link Slot#getChainReqData(String)} 是 {@link Map} 且包含
     *       {@value #CONVERSATION_ID_REQUEST_KEY} → 使用其值；</li>
     *   <li>否则使用 {@link ConversationIdGenerator#generate()} 生成一次性 id。</li>
     * </ol>
     *
     * <p>子类可整体覆写以接入自定义的会话路由策略（例如从 ThreadLocal、HTTP header 取值）。
     * 该方法在 ctx 还未构造时被调用，故无法使用 {@link #ctx()}，只能通过
     * {@link #getSlot()} 访问 slot。
     */
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

    /**
     * 用于在同一段对话中区分不同 agent 的 key，默认是 {@code nodeId}。
     * 高级场景下，多个 nodeId 可以通过覆写返回相同的 {@code agentKey} 来共享同一份记忆与 agent 实例。
     */
    protected String agentKey() {
        String nodeId = getNodeId();
        return (nodeId == null || nodeId.isEmpty()) ? "default" : nodeId;
    }

    /**
     * ReAct 最大迭代次数。返回 -1（默认值）表示使用
     * {@link com.yomahub.liteflow.property.agent.DefaultsConfig} 中的全局默认值。
     */
    protected int maxIterations() { return -1; }

    /**
     * 是否注册内置 {@link ManagedShellCommandTool}。默认开启。
     */
    protected boolean enableShellTool() { return true; }

    /**
     * 是否注册内置 {@link WorkspaceFileTools}。默认开启。
     */
    protected boolean enableWorkspaceFileTools() { return true; }

    /**
     * 提供 agent 钩子。默认返回空列表。
     */
    protected List<Hook> hooks() { return List.of(); }

    /**
     * 是否在日志中输出 agent 的 reason / act / error 事件。
     * <p>默认从配置 {@code liteflow.agent.logging.react-enabled} 读取（默认 true），
     * 子类可覆写返回 {@code true}/{@code false} 强制开关。
     * 输出在 logger {@code com.yomahub.liteflow.agent.hook.ReActLoggingHook} 上。
     */
    protected boolean enableReActLogging() {
        return agentConfig().getLogging().isReactEnabled();
    }

    /**
     * agent 回复后调用。默认实现会把 {@code reply.getTextContent()}
     * 写入 slot 的响应数据。
     */
    protected void handleReply(Msg reply) {
        ctx().getSlot().setResponseData(reply == null ? null : reply.getTextContent());
    }

    /* ===== 框架 final 执行体 ===== */

    /**
     * 在受管 session 中执行 ReActAgent。
     * <ol>
     *   <li>解析 conversationId（首次解析后写回 slot，后续 agent 复用）</li>
     *   <li>按 {@code (conversationId, agentKey)} 获取（或创建）{@link AgentSession}</li>
     *   <li>构造当次 {@link ReActAgentContext} 并挂到 slot 的 attachment</li>
     *   <li>首次使用时构建 {@link ReActAgent}，之后复用</li>
     *   <li>使用用户提示词调用 agent，并处理回复</li>
     *   <li>finally 清理 slot 上的 ctx attachment</li>
     * </ol>
     * 该方法被声明为 {@code final}，用于保证 session 加锁与 ctx 生命周期的正确性。
     */
    @Override
    public final void process() throws Exception {
        AgentConfig cfg = agentConfig();
        AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(cfg);
        MemoryStorageConfig mc = cfg.getSession().getMemory();
        Slot slot = this.getSlot();

        String cid = resolveConversationId();
        // 写回 slot，使同 chain 内后续 agent 直接复用同一段对话标识。
        slot.setConversationId(cid);

        String akey = agentKey();
        AgentSession session = mgr.acquire(cid, akey);
        session.getLock().lock();
        try {
            ReActAgentContext ctx = new ReActAgentContext(
                    slot, session.getConversationId(), session.getAgentKey(), session.getWorkspaceDir());
            slot.setAttachment(ctxKey(), ctx);
            try {
                ReActAgent agent = (ReActAgent) session.getAgent();
                if (agent == null) {
                    agent = buildAgent();
                    // 懒加载：同一个 (conversationId, agentKey) 在当前 JVM 中首次出现时才恢复一次。
                    mgr.loadIfExists(session, agent);
                    session.setAgent(agent);
                }
                Throwable processError = null;
                try {
                    Msg userMsg = Msg.builder().textContent(userPrompt()).build();
                    Msg reply = agent.call(List.of(userMsg)).block();
                    handleReply(reply);
                } catch (Throwable t) {
                    processError = t;
                    throw t;
                } finally {
                    boolean shouldSave = (processError == null) ? mc.isSaveAfterCall() : mc.isSaveOnError();
                    if (shouldSave) {
                        try {
                            mgr.save(session, agent);
                        } catch (Exception persistEx) {
                            if (processError != null) {
                                processError.addSuppressed(persistEx);
                            } else {
                                LOG.warn("session memory save failed for cacheKey={}",
                                        session.getCacheKey(), persistEx);
                            }
                        }
                    }
                }
            } finally {
                slot.removeAttachment(ctxKey());
            }
        } finally {
            session.getLock().unlock();
        }
    }

    private ReActAgent buildAgent() {
        AgentConfig cfg = agentConfig();
        int iters = maxIterations() > 0 ? maxIterations() : cfg.getDefaults().getMaxIterations();
        ReActAgentContext ctx = ctx();

        Toolkit toolkit = new Toolkit();
        tools().forEach(toolkit::registerTool);
        if (enableWorkspaceFileTools()) {
            toolkit.registerTool(new WorkspaceFileTools(ctx.getWorkspaceDir(), cfg));
        }
        if (enableShellTool() && cfg.getShell().getMode() != ShellMode.DISABLED) {
            toolkit.registerTool(new ManagedShellCommandTool(ctx.getWorkspaceDir(), cfg));
        }

        List<Hook> allHooks = new ArrayList<>(hooks());
        if (enableReActLogging()) {
            allHooks.add(new ReActLoggingHook(ctx.getConversationId() + ":" + ctx.getAgentKey()));
        }

        return ReActAgent.builder()
                .name(getNodeId() == null ? "liteflow-agent" : getNodeId())
                .sysPrompt(systemPrompt())
                .model(buildModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(iters)
                .hooks(allHooks)
                .build();
    }

    /** 持有单例 AgentSessionManager；首次 process() 时懒创建。 */
    static final class AgentSessionManagerHolder {
        private static volatile AgentSessionManager INSTANCE;
        static AgentSessionManager getOrCreate(AgentConfig cfg) {
            AgentSessionManager cur = INSTANCE;
            if (cur != null) return cur;
            synchronized (AgentSessionManagerHolder.class) {
                if (INSTANCE == null) INSTANCE = new AgentSessionManager(cfg);
                return INSTANCE;
            }
        }
        static void resetForTesting() {
            AgentSessionManager cur = INSTANCE;
            if (cur != null) {
                try { cur.close(); } catch (Exception ignored) {}
            }
            INSTANCE = null;
        }
    }
}
```

- [ ] **Step 2.2: 在 ReActAgentContext.java javadoc 中加陈旧引用警告**

`liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentContext.java`

在类上现有 javadoc 末尾追加一段警告（Edit 操作，定位到 class 级 javadoc 的 `</ul>` 之后、`*/` 之前）：

替换 old_string：
```
 *   <li>{@link #getWorkspaceDir()}：按 conversationId 创建，同一段对话中的多个 agent 共享。</li>
 * </ul>
 */
public class ReActAgentContext {
```

为 new_string：
```
 *   <li>{@link #getWorkspaceDir()}：按 conversationId 创建，同一段对话中的多个 agent 共享。</li>
 * </ul>
 *
 * <p><b>勿在跨 invocation 缓存的对象中持有 {@code ReActAgentContext} 引用</b>
 * （例如自定义工具实例、Hook、Model 实现）。这些对象会被缓存的 ReActAgent 跨次复用，
 * 而 ctx 是 per-invocation 的——捕获后下一次 process() 时通过该 ctx 访问的 slot
 * 已被 {@code DataBus.releaseSlot} 回收并复用，是悬挂引用。
 *
 * <p>正确做法：在工具/Model 类中持有组件实例引用，运行时通过
 * {@code component.ctx()} 动态获取当次 ctx。
 */
public class ReActAgentContext {
```

- [ ] **Step 2.3: 编译 react-agent-core**

Run: `mvn compile -pl liteflow-react-agent/liteflow-react-agent-core`
Expected: BUILD SUCCESS。core 自身没有引用旧签名（旧签名都在测试 cmp 子类里）。

- [ ] **Step 2.4: 暂不提交**

继续 Task 3 修复下游测试 cmp，避免提交不可编译状态。

---

## Task 3: 迁移所有测试 cmp 子类到无参签名

**Files:**
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/GeminiAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/OpenAIAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/AnthropicAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/DashScopeAgentCmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/CollabAgentACmp.java`
- Modify: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/CollabAgentBCmp.java`

**机械转换规则（每个 cmp 都套用）:**
1. `protected ModelSpec<?> model(ReActAgentContext ctx)` → `protected ModelSpec<?> model()`，方法体里 `ctx.X` 改为 `ctx().X`
2. `protected String systemPrompt(ReActAgentContext ctx)` → `protected String systemPrompt()`
3. `protected String userPrompt(ReActAgentContext ctx)` → `protected String userPrompt()`
4. `protected List<Object> tools(ReActAgentContext ctx)` → `protected List<Object> tools()`
5. `protected List<Hook> hooks(ReActAgentContext ctx)` → `protected List<Hook> hooks()`
6. `protected void handleReply(Msg reply, ReActAgentContext ctx)` → `protected void handleReply(Msg reply)`，调用 super 时去掉 ctx 参数
7. `protected String resolveConversationId(Slot slot)` → `protected String resolveConversationId()`，方法体里如需 slot 用 `getSlot()`
8. 删除不再使用的 `import com.yomahub.liteflow.agent.component.ReActAgentContext;` / `import com.yomahub.liteflow.slot.Slot;`（视实际是否仍需要）

- [ ] **Step 3.1: 重写 StubReActAgentCmp.java**

完整覆盖 `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`：

```java
package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component("stubAgent")
public class StubReActAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "fixed-conversation";
    public static final AtomicInteger SPEC_RESOLVE_COUNT = new AtomicInteger();
    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();
    public static final AtomicInteger SYSTEM_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger USER_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger HANDLE_REPLY_COUNT = new AtomicInteger();
    public static final AtomicInteger CUSTOM_TOOL_REGISTER_COUNT = new AtomicInteger();
    public static final AtomicInteger HOOK_EVENT_COUNT = new AtomicInteger();
    public static final List<Integer> MAX_ITERATIONS_SEEN = new CopyOnWriteArrayList<>();
    public static final List<String> USER_PROMPTS = new CopyOnWriteArrayList<>();
    public static final List<ModelProbe> MODEL_PROBES = new CopyOnWriteArrayList<>();
    public static volatile boolean shellToolEnabled = true;
    public static volatile boolean workspaceFileToolsEnabled = true;
    public static volatile boolean customHandleReply = false;
    public static volatile int overriddenMaxIterations = -1;

    public static void reset() {
        SPEC_RESOLVE_COUNT.set(0);
        BUILD_MODEL_COUNT.set(0);
        SYSTEM_PROMPT_COUNT.set(0);
        USER_PROMPT_COUNT.set(0);
        HANDLE_REPLY_COUNT.set(0);
        CUSTOM_TOOL_REGISTER_COUNT.set(0);
        HOOK_EVENT_COUNT.set(0);
        MAX_ITERATIONS_SEEN.clear();
        USER_PROMPTS.clear();
        MODEL_PROBES.clear();
        shellToolEnabled = true;
        workspaceFileToolsEnabled = true;
        customHandleReply = false;
        overriddenMaxIterations = -1;
    }

    @Override
    protected ModelSpec<?> model() {
        // 持有组件引用而非 ctx —— 运行时通过 this.ctx() 动态取
        return new StubModelSpec(this);
    }

    @Override
    protected String systemPrompt() {
        SYSTEM_PROMPT_COUNT.incrementAndGet();
        return "system:" + ctx().getConversationId() + ":" + ctx().getAgentKey();
    }

    @Override
    protected String userPrompt() {
        USER_PROMPT_COUNT.incrementAndGet();
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        String prompt = reqData == null ? "" : reqData.toString();
        USER_PROMPTS.add(prompt);
        return prompt;
    }

    @Override
    protected List<Object> tools() {
        CUSTOM_TOOL_REGISTER_COUNT.incrementAndGet();
        return List.of(new EchoTool());
    }

    @Override
    protected String resolveConversationId() {
        return FIXED_CONVERSATION_ID;
    }

    @Override
    protected int maxIterations() {
        return overriddenMaxIterations;
    }

    @Override
    protected boolean enableShellTool() {
        return shellToolEnabled;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return workspaceFileToolsEnabled;
    }

    @Override
    protected List<Hook> hooks() {
        return List.of(new Hook() {
            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                HOOK_EVENT_COUNT.incrementAndGet();
                if (event.getAgent() instanceof ReActAgent agent) {
                    MAX_ITERATIONS_SEEN.add(agent.getMaxIters());
                }
                return Mono.just(event);
            }
        });
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected void handleReply(Msg reply) {
        HANDLE_REPLY_COUNT.incrementAndGet();
        if (customHandleReply) {
            ctx().getSlot().setResponseData("handled:" + (reply == null ? null : reply.getTextContent()));
            return;
        }
        super.handleReply(reply);
    }

    public static class StubModelSpec extends ModelSpec<StubModelSpec> {
        private final StubReActAgentCmp comp;

        StubModelSpec(StubReActAgentCmp comp) {
            this.comp = comp;
        }

        @Override
        public Model resolve(AgentConfig cfg) {
            SPEC_RESOLVE_COUNT.incrementAndGet();
            BUILD_MODEL_COUNT.incrementAndGet();
            return new StubModel(comp);
        }
    }

    public static class StubModel implements Model {
        private final StubReActAgentCmp comp;
        private final AtomicInteger callCount = new AtomicInteger();

        StubModel(StubReActAgentCmp comp) {
            this.comp = comp;
        }

        @Override
        public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
            // 每次调用动态拿当次 ctx——保证 model 被 agent 缓存复用时，
            // 拿到的是当次 process() 的 ctx 而非首次的陈旧 ctx。
            var ctx = comp.ctx();
            List<String> toolNames = toolSchemas == null ? List.of() : toolSchemas.stream()
                    .map(ToolSchema::getName)
                    .sorted()
                    .toList();
            ModelProbe probe = new ModelProbe(
                    ctx.getConversationId(),
                    ctx.getAgentKey(),
                    ctx.getWorkspaceDir().toString(),
                    Files.isDirectory(ctx.getWorkspaceDir()),
                    callCount.incrementAndGet(),
                    messages == null ? List.of() : messages.stream().map(Msg::getTextContent).toList(),
                    toolNames,
                    options == null ? null : options.getTemperature());
            MODEL_PROBES.add(probe);
            String text = "reply:" + probe.conversationId + ":" + probe.callCount + ":" + probe.inputTexts;
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text(text).build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "stub-model";
        }
    }

    public static class EchoTool {
        @Tool(name = "custom_echo", description = "Return the provided value")
        public String echo(@ToolParam(name = "value", description = "value") String value) {
            return value;
        }
    }

    public record ModelProbe(
            String conversationId,
            String agentKey,
            String workspaceDir,
            boolean workspaceExists,
            int callCount,
            List<String> inputTexts,
            List<String> toolNames,
            Double temperature) {
    }
}
```

- [ ] **Step 3.2: 修改 GeminiAgentCmp.java**

先 Read：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/GeminiAgentCmp.java`

依次执行以下 Edit（每个独立替换）：

1. **删除 import**——`old_string`: `import com.yomahub.liteflow.agent.component.ReActAgentContext;\n` → `new_string`: ``
2. **改 model 签名**——`old_string`: `protected ModelSpec<?> model(ReActAgentContext ctx) {` → `new_string`: `protected ModelSpec<?> model() {`
3. **改 systemPrompt 签名**——`old_string`: `protected String systemPrompt(ReActAgentContext ctx) {` → `new_string`: `protected String systemPrompt() {`
4. **改 userPrompt 签名**——`old_string`: `protected String userPrompt(ReActAgentContext ctx) {` → `new_string`: `protected String userPrompt() {`
5. **方法体内 ctx 引用**：Read 看方法体里有无 `ctx.getXxx()` / `ctx.getSlot()` 调用，若有，把 `ctx.` 替换为 `ctx().`（用 Edit 精准替换每一处）

编译验证（局部）：`mvn test-compile -pl liteflow-testcase-el/liteflow-testcase-el-react-agent` 应该 GeminiAgentCmp 那部分无错（其他 cmp 仍然报错没关系，这一步只确认这个文件）。

- [ ] **Step 3.3: 修改 OpenAIAgentCmp.java**

文件路径：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/OpenAIAgentCmp.java`

执行与 Step 3.2 完全相同的五项 Edit（删 import / 改三个签名 / `ctx.` → `ctx().` 视方法体而定）。

- [ ] **Step 3.4: 修改 AnthropicAgentCmp.java**

文件路径：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/AnthropicAgentCmp.java`

执行与 Step 3.2 相同的五项 Edit。

- [ ] **Step 3.5: 修改 DashScopeAgentCmp.java**

文件路径：`liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/DashScopeAgentCmp.java`

执行与 Step 3.2 相同的五项 Edit。

- [ ] **Step 3.6: 修改 CollabAgentACmp.java**

注意此文件 `userPrompt(ctx)` 内部使用了 `ctx.getConversationId() / ctx.getAgentKey() / ctx.getWorkspaceDir()`，需要把 `ctx.X` 改为 `ctx().X`。同时更新 javadoc 中 `@link #userPrompt(ReActAgentContext)` 为 `@link #userPrompt()`。

完整方法体替换示例：

```java
@Override
protected String userPrompt() {
    SEEN_CONVERSATION_ID.set(ctx().getConversationId());
    SEEN_AGENT_KEY.set(ctx().getAgentKey());
    SEEN_WORKSPACE.set(ctx().getWorkspaceDir().toString());
    try {
        Path marker = ctx().getWorkspaceDir().resolve(MARKER_FILE);
        MARKER_EXISTED_BEFORE_WRITE.set(Files.exists(marker));
        Files.writeString(marker, MARKER_CONTENT);
    } catch (IOException e) {
        throw new RuntimeException("collab agent A failed to write marker", e);
    }
    return "A says hi";
}
```

- [ ] **Step 3.7: 修改 CollabAgentBCmp.java**

同上规则。

- [ ] **Step 3.8: 编译整个 react-agent 测试模块**

Run: `mvn test-compile -pl liteflow-testcase-el/liteflow-testcase-el-react-agent`
Expected: BUILD SUCCESS。如有编译错误说明某个 cmp 有遗漏，回到对应 step 修复。

- [ ] **Step 3.9: 跑全量测试确认没回归**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent`
Expected: 全部已有测试 PASS。

- [ ] **Step 3.10: 提交 Task 2 + Task 3**

```bash
git add liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java \
        liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/
git commit -m "refactor(react-agent): make all hooks parameterless via slot-bound ctx

- ReActAgentComponent 所有 hook 改为无参签名，新增 protected final ctx() 访问器
- ctx 通过 Slot.setAttachment 按 _react_agent_ctx_<nodeId> 挂载，process() finally 解绑
- 按 nodeId 隔离 attachment key，支持 WHEN 并发场景多个 agent 节点共享 slot
- 7 个测试 cmp 子类全部迁移到新签名
- StubModel 改为持有组件引用，运行时动态调 comp.ctx() 取当次 ctx

破坏性变更：所有 ReActAgentComponent 子类必须更新签名。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 新增防陈旧 ctx 与并发隔离测试

**Files:**
- Create: `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentCtxLifecycleTest.java`

**目标:** 三个专项验证：
1. agent 缓存复用时，`ctx()` 在每次 process() 返回当次 ctx（不是首次的陈旧 ctx）
2. `ctx()` 在 process() 之外调用抛 `IllegalStateException`
3. 同 chain 内通过 `agentKey()` 区分的两个 nodeId，attachment key 不会互相覆盖

> 真正的 WHEN 并行测试涉及搭建 agentscope 异步执行，复杂度较高且本次重构核心目标是签名重构；这里通过单元化的 attachment key 隔离测试来验证设计——如果 key 隔离正确，WHEN 并行行为就由 Slot 的并发安全性（`metaDataMap` 是 `ConcurrentHashMap`）兜底。

- [ ] **Step 4.1: 写新测试文件**

新建 `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentCtxLifecycleTest.java`：

```java
package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 ReActAgentContext 的生命周期边界：
 * 1) agent 跨次复用时，model/工具内通过 comp.ctx() 拿到的是当次 ctx，而非陈旧的构建期 ctx
 * 2) 在 process() 之外调用 ctx() 抛 IllegalStateException
 */
@SpringBootTest(classes = ReActAgentCtxLifecycleTest.class)
@TestPropertySource(value = "classpath:/agent/ctx-lifecycle.properties")
public class ReActAgentCtxLifecycleTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private StubReActAgentCmp stubAgent;

    @BeforeEach
    public void resetState() {
        StubReActAgentCmp.reset();
    }

    @Test
    @DisplayName("agent 复用时，每次执行 model 拿到的是当次 ctx，callCount 累加且 conversationId 一致")
    public void cachedAgent_freshCtxPerInvocation() {
        // 执行两次同一个 chainId（fixed conversation），agent 应该被缓存
        LiteflowResponse r1 = flowExecutor.execute2Resp("stubAgentChain", "first");
        assertTrue(r1.isSuccess());
        LiteflowResponse r2 = flowExecutor.execute2Resp("stubAgentChain", "second");
        assertTrue(r2.isSuccess());

        // BUILD_MODEL_COUNT 只 +1 说明 model 被复用（agent 被缓存）
        assertEquals(1, StubReActAgentCmp.BUILD_MODEL_COUNT.get(),
                "model 应该只构建一次，第二次复用缓存的 agent");

        // 但 stream 被调了两次（第一次和第二次 process）
        // 每次都通过 comp.ctx() 取当次 ctx，callCount 应该是 1, 2 累加
        assertEquals(2, StubReActAgentCmp.MODEL_PROBES.size(), "应有两次 stream 调用");
        assertEquals(1, StubReActAgentCmp.MODEL_PROBES.get(0).callCount());
        assertEquals(2, StubReActAgentCmp.MODEL_PROBES.get(1).callCount());

        // 两次 ctx 的 conversationId/agentKey/workspaceDir 应该完全一致——这些是不变量
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).conversationId(),
                StubReActAgentCmp.MODEL_PROBES.get(1).conversationId());
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).agentKey(),
                StubReActAgentCmp.MODEL_PROBES.get(1).agentKey());
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).workspaceDir(),
                StubReActAgentCmp.MODEL_PROBES.get(1).workspaceDir());

        // 但两次的 input messages 应该不同（第一次 "first"，第二次包含历史 + "second"）
        // 至少 inputTexts 不应该恒等
        assertNotEquals(StubReActAgentCmp.MODEL_PROBES.get(0).inputTexts(),
                StubReActAgentCmp.MODEL_PROBES.get(1).inputTexts(),
                "每次 stream 收到的 messages 应该来自当次 process，不能是首次的陈旧捕获");
    }

    @Test
    @DisplayName("在 process() 之外直接调 ctx() 抛 IllegalStateException")
    public void ctx_outsideProcess_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stubAgent.ctx());
        assertTrue(ex.getMessage().contains("not bound on slot"));
    }
}
```

> **关键** 这个测试需要 properties 文件 `agent/ctx-lifecycle.properties` 复用现有 `application.properties` 的内容，或直接用 `classpath:/application.properties`。先确认现有测试模块的配置文件路径。

- [ ] **Step 4.2: 确认 properties 路径**

Run: `ls /Users/bryan31/openSource/LiteFlow-Jdk17/liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/`
Expected: 看到 `flow.el.xml` 等。如果没有 `ctx-lifecycle.properties`，把测试文件中的 `@TestPropertySource` 改为复用其他测试已用的同模块 properties（参考 `ReActAgentSessionTest` 的 `@TestPropertySource` 写法）。

- [ ] **Step 4.3: 调整测试 properties 引用**

Read 现有测试如 `ReActAgentELChainTest` 或 `ReActAgentSessionTest`，看它们用什么 `@TestPropertySource` 路径。把 Step 4.1 的测试文件 `@TestPropertySource` 改为同样的引用，并把 `@SpringBootTest(classes = ...)` 换成同样的配置类（如果它们用的是某个 `@SpringBootApplication` 的 main class）。

具体替换：先 Read 一个现成测试，然后把 `ReActAgentCtxLifecycleTest` 的 `@SpringBootTest(classes = ...)` 和 `@TestPropertySource(value = ...)` 改成与之一致。

- [ ] **Step 4.4: 跑新测试**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent -Dtest=ReActAgentCtxLifecycleTest`
Expected: 两个测试方法 PASS。

- [ ] **Step 4.5: 跑模块全量回归**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent`
Expected: 全部 PASS。

- [ ] **Step 4.6: 提交**

```bash
git add liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/ReActAgentCtxLifecycleTest.java
git commit -m "test(react-agent): cover ctx() lifecycle invariants

- 验证 agent 缓存复用时 model 内 ctx() 取到当次 ctx 而非陈旧引用
- 验证 process() 之外调 ctx() 抛 IllegalStateException

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: 跑全量测试，最终验证

- [ ] **Step 5.1: 跑 liteflow-core 全量**

Run: `mvn test -pl liteflow-core`
Expected: 全部 PASS（attachment API 是新增，不影响已有逻辑）。

- [ ] **Step 5.2: 跑 react-agent 测试模块全量**

Run: `mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent`
Expected: 全部 PASS，包括新增的 ReActAgentCtxLifecycleTest。

- [ ] **Step 5.3: 检查 git 状态**

Run: `git status`
Expected: 工作树干净，无未提交改动；分支领先 master 3 个 commit（Task 1、Task 2+3、Task 4）。

- [ ] **Step 5.4: 检查 commit log**

Run: `git log --oneline -3`
Expected:
```
<sha> test(react-agent): cover ctx() lifecycle invariants
<sha> refactor(react-agent): make all hooks parameterless via slot-bound ctx
<sha> feat(core): add generic attachment API to Slot
```

---

## Self-Review 备注

执行者完成所有 Task 后，应：
1. 重新阅读 spec（`docs/superpowers/specs/2026-05-09-react-agent-context-refactor-design.md`），逐项核对覆盖
2. 确认 `ReActAgentComponent.AgentSessionManagerHolder.resetForTesting()` 在测试中仍可用（重构未触及该方法）
3. WHEN 并行的真实 e2e 测试本计划未覆盖（YAGNI）——通过 nodeId 隔离的 attachment key + Slot.metaDataMap 的 ConcurrentHashMap 安全性兜底；若后续需要 e2e 验证再单独立项
