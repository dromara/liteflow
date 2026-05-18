package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.hook.ChatUsageTrackingHook;
import com.yomahub.liteflow.agent.hook.ReActLoggingHook;
import com.yomahub.liteflow.agent.skill.SkillBoxFactory;
import com.yomahub.liteflow.agent.skill.SkillLoadResult;
import com.yomahub.liteflow.agent.skill.SkillTrackingHook;
import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.ConversationIdGenerator;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import com.yomahub.liteflow.agent.model.ModelSpec;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

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
 * <p>When {@code liteflow.agent.skills.enabled=true}, the component can load
 * agent-scope skills from {@code liteflow.agent.skills.path}. Override
 * {@link #skills()} to restrict the component to a fixed allow-list; an empty
 * list means all configured skills are available. The allow-list and
 * {@link #enableSkills()} are evaluated only when the cached ReActAgent is built
 * for a {@code (conversationId, agentKey)} session, so they are stable component
 * capability declarations and should not depend on request data.
 *
 * <p><b>注意：勿在跨 invocation 缓存的对象（自定义工具/Hook/Model 等）中持有
 * {@link ReActAgentContext} 引用</b>——这些对象会被缓存的 agent 复用，捕获的 ctx
 * 会在下一次 {@code process()} 时变成陈旧引用（其中的 slot 已经被回收）。
 * 正确做法：持有组件实例引用，运行时通过 {@code component.ctx()} 动态获取。
 *
 * <p>技能相关的 {@link #skills()} 与 {@link #enableSkills()} 只在为某个
 * {@code (conversationId, agentKey)} session 首次构建并缓存 ReActAgent 时求值。
 * 它们表示组件能力声明，不应依赖单次请求数据；同一 session 复用缓存 agent 时不会
 * 重新读取这些声明。
 *
 * <p>{@link #process()} 方法被声明为 {@code final}，由框架统一保证 session
 * 管理和 ctx 生命周期的正确性。
 */
public abstract class ReActAgentComponent extends NodeComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ReActAgentComponent.class);

    public static final String FLOW_EVENT_TYPE_REASONING = "agent.reasoning";
    public static final String FLOW_EVENT_TYPE_TOOL_RESULT = "agent.tool_result";
    public static final String FLOW_EVENT_TYPE_SUMMARY = "agent.summary";
    public static final String FLOW_EVENT_TYPE_RESULT = "agent.result";

    /** 默认从 chain requestData 中读取 conversationId 时使用的约定 key。 */
    public static final String CONVERSATION_ID_REQUEST_KEY = "conversationId";

    /**
     * 框架统一系统提示词。子类 {@link #systemPrompt()} 返回的内容会追加在该提示词之后。
     */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是 LiteFlow ReAct Agent 助手。
            请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
            每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
            不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
            """;

    /** 在 Slot attachment 上存储 ctx 时使用的 key 前缀，按 nodeId 隔离。 */
    private static final String CTX_KEY_PREFIX = "_react_agent_ctx_";

    /** 在 Slot attachment 上存储技能跟踪 Hook 时使用的 key 前缀，按 nodeId 隔离。 */
    private static final String SKILL_HOOK_KEY_PREFIX = "_react_agent_skill_hook_";

    private String ctxKey() {
        String nodeId = getNodeId();
        return CTX_KEY_PREFIX + (nodeId == null ? "default" : nodeId);
    }

    private String skillHookKey() {
        String nodeId = getNodeId();
        return SKILL_HOOK_KEY_PREFIX + (nodeId == null ? "default" : nodeId);
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
     * 构建本组件使用的模型描述符。
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
     * 返回组件自定义系统提示词。构建 agent 时只调用一次，并追加在框架统一系统提示词之后。
     */
    protected abstract String systemPrompt();

    /**
     * 返回传递给底层 ReActAgent 的最终系统提示词。
     */
    protected final String effectiveSystemPrompt() {
        String customPrompt = systemPrompt();
        if (customPrompt == null || customPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT.strip();
        }
        return DEFAULT_SYSTEM_PROMPT.strip() + "\n\n" + customPrompt.strip();
    }

    /**
     * 返回本次执行的用户提示词。每次 {@link #process()} 都会调用。
     */
    protected abstract String userPrompt();

    /* ===== 可选覆写 ===== */

    /**
     * 提供要注册到 agent {@link Toolkit} 中的额外工具对象。
     * 默认返回空列表。
     */
    protected List<Object> tools() { return List.of(); }

    /**
     * Return skill names this component may use. Empty means all configured skills.
     *
     * <p>This is evaluated only when the cached ReActAgent is built for a
     * {@code (conversationId, agentKey)} session. Treat it as a stable component
     * capability declaration; do not vary it per request.
     */
    protected List<String> skills() { return List.of(); }

    /**
     * Whether agent-scope skills should be enabled for this component.
     *
     * <p>This is evaluated only when the cached ReActAgent is built for a
     * {@code (conversationId, agentKey)} session. Treat it as a stable component
     * capability declaration; do not vary it per request.
     */
    protected boolean enableSkills() { return agentConfig().getSkills().isEnabled(); }

    /**
     * Return skill names loaded by this agent during the current invocation.
     *
     * <p>This is available only while this component's {@link #process()} body has
     * bound the invocation skill hook, including calls from {@link #userPrompt()},
     * tool callbacks, and {@link #handleReply(Msg)}. After {@code process()} final
     * cleanup, later lifecycle callbacks must not rely on it.
     */
    protected final List<String> usedSkills() {
        SkillTrackingHook hook = getSlot().getAttachment(skillHookKey());
        return hook == null ? List.of() : hook.getUsedSkills();
    }

    /**
     * 解析本次执行的 {@code conversationId}。
     *
     * <p>默认实现：先看 slot 上已有 conversationId，再看 chainReqData 里有没有，
     * 最后生成一次性 id。
     *
     * <p>该方法在 ctx 还未构造时被调用，只能通过 {@link #getSlot()} 访问 slot。
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
     */
    protected String agentKey() {
        String nodeId = getNodeId();
        return (nodeId == null || nodeId.isEmpty()) ? "default" : nodeId;
    }

    protected int maxIterations() { return -1; }
    protected boolean enableShellTool() { return true; }
    protected boolean enableWorkspaceFileTools() { return true; }
    protected List<Hook> hooks() { return List.of(); }

    protected boolean enableReActLogging() {
        return agentConfig().getLogging().isReactEnabled();
    }

    protected void handleReply(Msg reply) {
        if (reply == null || reply.getTextContent() == null) {
            return;
        }
        ctx().getSlot().setResponseData(reply.getTextContent());
    }

    /* ===== 框架 final 执行体 ===== */

    @Override
    public final void process() throws Exception {
        AgentConfig cfg = agentConfig();
        AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(cfg);
        MemoryStorageConfig mc = cfg.getSession().getMemory();
        Slot slot = this.getSlot();

        String cid = resolveConversationId();
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
                    BuiltAgent built = buildAgent();
                    agent = built.agent();
                    session.setSkillTrackingHook(built.skillTrackingHook());
                    session.setChatUsageTrackingHook(built.chatUsageTrackingHook());
                    mgr.loadIfExists(session, agent);
                    session.setAgent(agent);
                }
                SkillTrackingHook skillHook = session.getSkillTrackingHook();
                if (skillHook != null) {
                    skillHook.clear();
                    slot.setAttachment(skillHookKey(), skillHook);
                }
                ChatUsageTrackingHook usageHook = session.getChatUsageTrackingHook();
                if (usageHook != null) {
                    usageHook.reset();
                    ctx.setChatUsageTrackingHook(usageHook);
                }
                Throwable processError = null;
                try {
                    Msg userMsg = Msg.builder().textContent(userPrompt()).build();
                    Msg reply = callAgent(agent, userMsg, slot);
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
                slot.removeAttachment(skillHookKey());
            }
        } finally {
            session.getLock().unlock();
        }
    }

    private Msg callAgent(ReActAgent agent, Msg userMsg, Slot slot) {
        if (!FlowEventPublisher.hasListener(slot)) {
            return agent.call(List.of(userMsg)).block();
        }
        return streamAgent(agent, userMsg, slot).block();
    }

    private Mono<Msg> streamAgent(ReActAgent agent, Msg userMsg, Slot slot) {
        AtomicReference<Msg> finalMsg = new AtomicReference<>();
        AtomicReference<Msg> fallbackFinalMsg = new AtomicReference<>();
        StreamOptions options = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.SUMMARY, EventType.AGENT_RESULT)
                .incremental(true)
                .build();

        return agent.stream(List.of(userMsg), options)
                .doOnNext(event -> {
                    if (event.getType() == EventType.AGENT_RESULT) {
                        finalMsg.set(event.getMessage());
                    } else if (event.isLast()) {
                        fallbackFinalMsg.set(event.getMessage());
                    }
                    publishAgentEvent(slot, event);
                })
                .then(Mono.defer(() -> {
                    Msg msg = finalMsg.get();
                    if (msg != null) {
                        return Mono.just(msg);
                    }
                    Msg fallback = fallbackFinalMsg.get();
                    return fallback == null ? Mono.empty() : Mono.just(fallback);
                }));
    }

    private void publishAgentEvent(Slot slot, Event event) {
        String type = toFlowEventType(event.getType());
        if (type == null) {
            return;
        }
        Msg msg = event.getMessage();
        FlowEventPublisher.publish(slot, FlowEvent.builder()
                .type(type)
                .chainId(slot.getChainId())
                .nodeId(getNodeId())
                .requestId(slot.getRequestId())
                .conversationId(slot.getConversationId())
                .text(msg == null ? null : msg.getTextContent())
                .last(event.isLast())
                .data(event)
                .build());
    }

    private String toFlowEventType(EventType type) {
        if (type == EventType.REASONING) {
            return FLOW_EVENT_TYPE_REASONING;
        }
        if (type == EventType.TOOL_RESULT) {
            return FLOW_EVENT_TYPE_TOOL_RESULT;
        }
        if (type == EventType.SUMMARY) {
            return FLOW_EVENT_TYPE_SUMMARY;
        }
        if (type == EventType.AGENT_RESULT) {
            return FLOW_EVENT_TYPE_RESULT;
        }
        return null;
    }

    private record BuiltAgent(ReActAgent agent, SkillTrackingHook skillTrackingHook,
                              ChatUsageTrackingHook chatUsageTrackingHook) {
    }

    private BuiltAgent buildAgent() {
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

        ChatUsageTrackingHook chatUsageTrackingHook = new ChatUsageTrackingHook();
        allHooks.add(chatUsageTrackingHook);

        SkillTrackingHook skillTrackingHook = null;
        SkillBox skillBox = null;
        if (enableSkills()) {
            SkillLoadResult skillLoadResult = SkillBoxFactory.build(toolkit, cfg, skills(), ctx.getWorkspaceDir());
            skillBox = skillLoadResult.skillBox();
            skillTrackingHook = new SkillTrackingHook(skillLoadResult.skillIdToName());
            allHooks.add(skillTrackingHook);
        }

        ReActAgent.Builder builder = ReActAgent.builder()
                .name(getNodeId() == null ? "liteflow-agent" : getNodeId())
                .sysPrompt(effectiveSystemPrompt())
                .model(buildModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(iters)
                .hooks(allHooks);

        if (skillBox != null) {
            builder.skillBox(skillBox);
        }

        return new BuiltAgent(builder.build(), skillTrackingHook, chatUsageTrackingHook);
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
