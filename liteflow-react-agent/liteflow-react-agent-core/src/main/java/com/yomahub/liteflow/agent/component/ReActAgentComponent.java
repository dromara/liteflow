package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.hook.ReActLoggingHook;
import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.agent.session.NanoIdSessionIdGenerator;
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

/**
 * 封装 agentscope ReActAgent 的 LiteFlow 抽象组件。
 * <p>
 * 子类必须提供 {@link #model}、{@link #systemPrompt} 和
 * {@link #userPrompt}。可选覆写方法用于自定义工具、钩子和生命周期回调。
 * <p>
 * {@link #process()} 方法被声明为 {@code final}，由框架统一保证 session
 * 管理和 agent 生命周期的正确性。
 */
public abstract class ReActAgentComponent extends NodeComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ReActAgentComponent.class);

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

    /* ===== 必须实现 ===== */

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

    /**
     * 返回 agent 的系统提示词。构建 agent 时只调用一次。
     */
    protected abstract String systemPrompt(ReActAgentContext ctx);

    /**
     * 返回本次执行的用户提示词。每次 {@link #process()} 都会调用。
     */
    protected abstract String userPrompt(ReActAgentContext ctx);

    /* ===== 可选覆写 ===== */

    /**
     * 提供要注册到 agent {@link Toolkit} 中的额外工具对象。
     * 对象中标注了 {@code @Tool} 的方法会被自动发现。
     * 默认返回空列表。
     */
    protected List<Object> tools(ReActAgentContext ctx) { return List.of(); }

    /**
     * 从当前 slot 推导 session id。默认生成 {@code YYYYMMDD + NanoId(18)} 格式。
     */
    protected String resolveSessionId(Slot slot) {
        return NanoIdSessionIdGenerator.generate();
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
    protected List<Hook> hooks(ReActAgentContext ctx) { return List.of(); }

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
    protected void handleReply(Msg reply, ReActAgentContext ctx) {
        ctx.getSlot().setResponseData(reply == null ? null : reply.getTextContent());
    }

    /* ===== 框架 final 执行体 ===== */

    /**
     * 在受管 session 中执行 ReActAgent。
     * <ol>
     *   <li>根据 session id 获取（或创建）{@link AgentSession}</li>
     *   <li>首次使用时构建 {@link ReActAgent}，之后复用</li>
     *   <li>使用用户提示词调用 agent，并处理回复</li>
     * </ol>
     * 该方法被声明为 {@code final}，用于保证 session 加锁逻辑正确执行。
     */
    @Override
    public final void process() throws Exception {
        AgentConfig cfg = agentConfig();
        AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(cfg);
        MemoryStorageConfig mc = cfg.getSession().getMemory();
        Slot slot = this.getSlot();
        String sid = resolveSessionId(slot);
        AgentSession session = mgr.acquire(sid);
        session.getLock().lock();
        try {
            ReActAgentContext ctx = new ReActAgentContext(slot, session.getSessionId(), session.getWorkspaceDir());
            ReActAgent agent = (ReActAgent) session.getAgent();
            if (agent == null) {
                agent = buildAgent(ctx);
                // 懒加载：同一个 sessionId 在当前 JVM 中首次出现时才恢复一次。
                mgr.loadIfExists(session, agent);
                session.setAgent(agent);
            }
            Throwable processError = null;
            try {
                Msg userMsg = Msg.builder().textContent(userPrompt(ctx)).build();
                Msg reply = agent.call(List.of(userMsg)).block();
                handleReply(reply, ctx);
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
                            LOG.warn("session memory save failed for sid={}", session.getSessionId(), persistEx);
                        }
                    }
                }
            }
        } finally {
            session.getLock().unlock();
        }
    }

    private ReActAgent buildAgent(ReActAgentContext ctx) {
        AgentConfig cfg = agentConfig();
        int iters = maxIterations() > 0 ? maxIterations() : cfg.getDefaults().getMaxIterations();

        Toolkit toolkit = new Toolkit();
        tools(ctx).forEach(toolkit::registerTool);
        if (enableWorkspaceFileTools()) {
            toolkit.registerTool(new WorkspaceFileTools(ctx.getWorkspaceDir(), cfg));
        }
        if (enableShellTool() && cfg.getShell().getMode() != ShellMode.DISABLED) {
            toolkit.registerTool(new ManagedShellCommandTool(ctx.getWorkspaceDir(), cfg));
        }

        List<Hook> allHooks = new ArrayList<>(hooks(ctx));
        if (enableReActLogging()) {
            allHooks.add(new ReActLoggingHook(ctx.getSessionId()));
        }

        return ReActAgent.builder()
                .name(getNodeId() == null ? "liteflow-agent" : getNodeId())
                .sysPrompt(systemPrompt(ctx))
                .model(buildModel(ctx))
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
