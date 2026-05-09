# ReActAgentContext 重构设计

**日期**: 2026-05-09
**作用范围**: `liteflow-core` (Slot)、`liteflow-react-agent-core`、`liteflow-testcase-el-react-agent`
**类型**: 破坏式重构

---

## 1. 背景与问题

当前 `ReActAgentComponent` 的 hook 方法签名混乱、语义不清：

```java
protected ModelSpec<?> model(ReActAgentContext ctx);
protected String systemPrompt(ReActAgentContext ctx);
protected String userPrompt(ReActAgentContext ctx);
protected List<Object> tools(ReActAgentContext ctx);
protected List<Hook> hooks(ReActAgentContext ctx);
protected void handleReply(Msg reply, ReActAgentContext ctx);

protected String resolveConversationId(Slot slot);   // 传 Slot

protected String agentKey();                          // 无参
protected int maxIterations();                        // 无参
protected boolean enableShellTool();                  // 无参
```

### 1.1 三类问题

**问题 A：签名不统一**
同一抽象类的 hook，有的传 `ReActAgentContext`，有的传 `Slot`，有的无参，使用者很难记忆。

**问题 B：构建期与运行期上下文混合**
- `tools / hooks / systemPrompt / model` 是**构建期 hook**——agent 按 `(conversationId, agentKey)` 缓存（见 `ReActAgentComponent.java:221-225`），这些方法只在 agent 首次构建时调用一次。
- `userPrompt / handleReply` 是**运行期 hook**——每次 `process()` 都调用。
- 但当前两类方法都接收同一个 `ReActAgentContext`。该 ctx 内部持有 `Slot`，而 Slot 是**每次执行**才有效的对象。
- 用户在 `tools(ctx)` 里若把 ctx 捕获进工具实例，工具被 LLM 异步触发时通过 `ctx.getSlot()` 拿到的是**构建那一次的 slot**——该 slot 已被 `DataBus.releaseSlot` 回收并复用给别的 chain，是悬挂引用。

**问题 C：ctx 与 agent 生命周期错位**
`ReActAgentContext` 语义上是"本次调用的上下文"，本应与 `Slot`（per-invocation 容器）同生命周期；但当前它被作为参数传给 agent 构建函数，又因为 agent 缓存导致 ctx 被永久捕获，与"per-invocation"语义矛盾。

### 1.2 重构目标

1. 所有 hook 方法签名统一为**无参**。
2. ctx 与 Slot 同生命周期，挂在 Slot 上（**方案 B**：通过 Slot 的通用 attachment 机制）。
3. `this.ctx()` 是动态访问入口，每次返回**当次** invocation 的 ctx。
4. agent 不再持有 ctx 引用——构建期从 `this.ctx()` 拿到的不变量字段（conversationId / agentKey / workspaceDir）按值使用即可。

---

## 2. 设计方案

### 2.1 Slot 增加通用 attachment API

**文件**: `liteflow-core/src/main/java/com/yomahub/liteflow/slot/Slot.java`

新增四个 public 方法，复用已有 `metaDataMap`：

```java
public <T> void setAttachment(String key, T value) {
    putMetaDataMap(key, value);  // 复用现有私有方法，含 null 校验
}

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

**说明**:
- 这是 Slot 上的通用 KV 入口，不感知任何插件类型。Slot 本身保持插件无关。
- 命名前缀建议使用插件特定前缀（如 `_react_agent_ctx_`）避免与 Slot 已有的内部 key（`_request_`/`_response_`/`_chain_id` 等）冲突。

### 2.2 ReActAgentComponent 全面无参化

**文件**: `liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/component/ReActAgentComponent.java`

#### 2.2.1 ctx 挂载机制

```java
private static final String CTX_KEY_PREFIX = "_react_agent_ctx_";

// 按 nodeId 隔离，支持同一 chain 内 WHEN 并行执行多个 agent 节点而互不干扰
private String ctxKey() {
    return CTX_KEY_PREFIX + getNodeId();
}

/**
 * 获取当次执行的 ReActAgent 运行上下文。
 * 必须在 process() 执行期间内调用（包括 process() 触发的工具回调内）。
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
```

**正确性论证**:
- `NodeComponent.getSlot()` 通过 `Node.slotIndexTL`（`TransmittableThreadLocal<Integer>`，见 `Node.java:86`）动态返回当前 slot——TTL 会在 reactor 异步线程间正确传播。
- 工具被 `agent.call().block()` 内部回调时，仍处于本次 `process()` 的执行栈/响应链上，TTL 中的 slotIndex 仍为当次值，故 `ctx()` 返回当次 ctx。

#### 2.2.2 process() 改造

```java
@Override
public final void process() throws Exception {
    AgentConfig cfg = agentConfig();
    AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(cfg);
    MemoryStorageConfig mc = cfg.getSession().getMemory();
    Slot slot = this.getSlot();

    // 第一步：解析 conversationId（此时 ctx 还未构造，只能用 slot）
    String cid = resolveConversationId();   // 已无参，内部用 getSlot()
    slot.setConversationId(cid);

    String akey = agentKey();
    AgentSession session = mgr.acquire(cid, akey);
    session.getLock().lock();
    try {
        // 第二步：构造当次 ctx 并绑定到 slot
        ReActAgentContext ctx = new ReActAgentContext(
                slot, session.getConversationId(), session.getAgentKey(), session.getWorkspaceDir());
        slot.setAttachment(ctxKey(), ctx);

        try {
            ReActAgent agent = (ReActAgent) session.getAgent();
            if (agent == null) {
                agent = buildAgent();   // 内部使用 ctx() 取构建期不变量
                mgr.loadIfExists(session, agent);
                session.setAgent(agent);
            }
            // 第三步：执行 agent
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
            // 清理 attachment，避免 slot 池化复用时读到陈旧 ctx
            // （理论上下次执行会重新绑定，但显式 remove 更稳健）
            slot.removeAttachment(ctxKey());
        }
    } finally {
        session.getLock().unlock();
    }
}
```

#### 2.2.3 hook 全部无参

```java
// 必须实现
protected abstract ModelSpec<?> model();
protected abstract String systemPrompt();
protected abstract String userPrompt();

// 可选覆写
protected Model buildModel() {
    return model().resolve(agentConfig());
}
protected List<Object> tools()       { return List.of(); }
protected List<Hook> hooks()         { return List.of(); }
protected String resolveConversationId() {
    Slot slot = getSlot();
    String existing = slot.getConversationId();
    if (existing != null && !existing.isEmpty()) return existing;
    Object req = slot.getChainReqData(slot.getChainId());
    if (req instanceof Map<?, ?> map) {
        Object v = map.get(CONVERSATION_ID_REQUEST_KEY);
        if (v != null && !v.toString().isEmpty()) return v.toString();
    }
    return ConversationIdGenerator.generate();
}
protected void handleReply(Msg reply) {
    ctx().getSlot().setResponseData(reply == null ? null : reply.getTextContent());
}

// 既有无参方法保持不变
protected String agentKey()                { ... }
protected int maxIterations()              { return -1; }
protected boolean enableShellTool()        { return true; }
protected boolean enableWorkspaceFileTools() { return true; }
protected boolean enableReActLogging()     { ... }
```

#### 2.2.4 buildAgent 改造

```java
private ReActAgent buildAgent() {
    AgentConfig cfg = agentConfig();
    int iters = maxIterations() > 0 ? maxIterations() : cfg.getDefaults().getMaxIterations();
    ReActAgentContext ctx = ctx();   // 取构建期不变量

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
```

注意：`buildAgent()` 自身仍然只在第一次执行时被调用一次，但调用它时 ctx 已经绑在 slot 上，所以内部一次性读取 `ctx().getWorkspaceDir()` 等不变量是安全的——读到的是首次调用的值，之后这些值也不会变（因为 agent 按 `(cid, akey)` 缓存，对应 workspaceDir 一致）。

### 2.3 ReActAgentContext 保持不变

`ReActAgentContext` 类本身**不改动**：依然包含 `slot / conversationId / agentKey / workspaceDir` 四个字段。改动的是**它如何被访问**——从"参数传递"改为"通过 slot attachment 间接获取"。

> 备选方案：移除 `ReActAgentContext.getSlot()`，强制用户走 `ctx().getSlot()` 而非缓存 ctx 引用。但这会让现有用户代码（如测试中的 StubModel）改动量更大。本次重构**保留 `getSlot()`**，在文档中明确警告"勿在跨 invocation 缓存的对象（工具/Hook 等）中持有 ctx 引用"。

### 2.4 用户代码迁移示例

**重构前** (`StubReActAgentCmp`):
```java
@Override
protected String userPrompt(ReActAgentContext ctx) {
    Object reqData = ctx.getSlot().getChainReqData(ctx.getSlot().getChainId());
    return reqData == null ? "" : reqData.toString();
}
```

**重构后**:
```java
@Override
protected String userPrompt() {
    Slot slot = getSlot();   // 或 ctx().getSlot()
    Object reqData = slot.getChainReqData(slot.getChainId());
    return reqData == null ? "" : reqData.toString();
}
```

工具/Model 类内部如果需要 ctx，要持有组件引用并通过 `comp.ctx()` 动态访问，而不是缓存 ctx 本身：

```java
// 错误：捕获了 ctx，构建后失效
class BadModel {
    private final ReActAgentContext ctx;
    BadModel(ReActAgentContext ctx) { this.ctx = ctx; }
    void use() { ctx.getSlot().setData(...); }   // stale slot
}

// 正确：捕获组件引用，运行时动态取 ctx
class GoodModel {
    private final ReActAgentComponent comp;
    GoodModel(ReActAgentComponent comp) { this.comp = comp; }
    void use() { comp.ctx().getSlot().setData(...); }   // 当次 slot
}
```

---

## 3. 测试改动范围

需要按新签名改造的文件：
- `liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/cmp/StubReActAgentCmp.java`
- `ReActAgentELChainTest.java`
- `ReActAgentSessionTest.java`
- `ReActAgentWorkspaceTest.java`
- `ReActAgentConversationContinuityTest.java`（新文件）
- `ReActAgentMultiAgentChainTest.java`（新文件）

新增测试用例（在现有覆盖之外补强）：
1. **构建期工具不会捕获陈旧 ctx**：用一个工具方法，内部对当前 chain 写入 slot 数据，验证多次执行同 chain 时每次都写入"当次的 slot"，而非首次的 slot。
2. **WHEN 并行执行**：同一 chain 内并行触发两个不同 nodeId 的 agent，验证各自 `ctx()` 返回正确的 ctx 实例（按 nodeId 隔离的 attachment key 验证）。
3. **`ctx()` 在 process() 之外抛 `IllegalStateException`**。

---

## 4. 不在范围内（YAGNI）

- 不在 NodeComponent 层提取通用 per-invocation context 机制（用户已确认作用域控制在 react-agent 模块）。
- 不引入 BuildContext / ExecContext 类型拆分（用户选择"无参 + this.ctx() 动态访问"路线，而非类型系统强制路线）。
- 不保留 deprecated 旧签名（用户选择直接破坏式修改）。
- 不修改 `AgentSession` / `AgentSessionManager` 的内部存储结构。
- 不改动 `ModelSpec` 抽象。

---

## 5. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 用户在工具/Model 中错误捕获 ctx 引用 | 拿到 stale slot | Javadoc 明确警告，新增专项测试覆盖正确写法 |
| Slot 新增 public API 命名冲突 | 用户子类已有同名方法 | `setAttachment / getAttachment / removeAttachment / hasAttachment` 名字够具体，冲突概率低；Slot 不是常被继承的类 |
| WHEN 并行下两个 agent 节点共享 slot | attachment key 冲突 | 设计已用 `CTX_KEY_PREFIX + nodeId` 隔离 |
| `slot.removeAttachment` 在异常路径被跳过 | slot 复用时残留 ctx | finally 兜底；下次同 nodeId 再次 process() 也会覆盖；slot 释放回池时 metaDataMap 会被清空（需在实施时验证） |

---

## 6. 实施顺序

1. liteflow-core: 给 `Slot` 加 `setAttachment / getAttachment / removeAttachment / hasAttachment`
2. react-agent-core: 改 `ReActAgentComponent`——加 `ctx()` 访问器、改 `process()` 绑定/解绑、所有 hook 改无参
3. 同步更新 `ReActAgentContext` 的 Javadoc（警告勿持有 ctx 引用）
4. 改造 `StubReActAgentCmp` 和现有 5 个测试文件
5. 新增 3 个专项测试（陈旧 ctx 防护 / WHEN 并发 / 越界调用 ctx()）
6. 跑全量测试：`mvn test -pl liteflow-testcase-el/liteflow-testcase-el-react-agent`
7. 跑 liteflow-core 测试确认 Slot 改动无回归
