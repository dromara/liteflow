package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;

import java.util.List;

/**
 * Abstract LiteFlow component that wraps an agentscope ReActAgent.
 * <p>
 * Subclasses must provide {@link #buildModel}, {@link #systemPrompt}, and
 * {@link #userPrompt}.  Optional overrides allow custom tools, hooks, and
 * lifecycle callbacks.
 * <p>
 * The {@link #process()} method is {@code final} so that the framework can
 * guarantee proper session management and agent lifecycle.
 */
public abstract class ReActAgentComponent extends NodeComponent {

    /* ===== Framework-provided final accessor ===== */

    /**
     * Returns the agent section of the current LiteflowConfig.
     *
     * @throws AgentConfigException if liteflow.agent has not been configured
     */
    protected final AgentConfig agentConfig() {
        AgentConfig c = LiteflowConfigGetter.get().getAgent();
        if (c == null) {
            throw new AgentConfigException(
                    "LiteflowConfig.agent is null; configure liteflow.agent.* or setAgent() before use");
        }
        return c;
    }

    /* ===== Must implement ===== */

    /**
     * Build the {@link Model} instance used by the ReActAgent.
     * Subclasses typically choose a specific provider (OpenAI, Anthropic, etc.)
     * based on configuration from {@link AgentConfig}.
     */
    protected abstract Model buildModel(ReActAgentContext ctx);

    /**
     * Return the system prompt for the agent. Called once when the agent is built.
     */
    protected abstract String systemPrompt(ReActAgentContext ctx);

    /**
     * Return the user prompt for this execution. Called on every {@link #process()}.
     */
    protected abstract String userPrompt(ReActAgentContext ctx);

    /* ===== Optional overrides ===== */

    /**
     * Provide additional tool objects to register with the agent's {@link Toolkit}.
     * Each object's methods annotated with {@code @Tool} will be discovered automatically.
     * Returns an empty list by default.
     */
    protected List<Object> tools(ReActAgentContext ctx) { return List.of(); }

    /**
     * Derive the session id from the current slot. Defaults to the slot's requestId.
     */
    protected String resolveSessionId(Slot slot) { return slot.getRequestId(); }

    /**
     * Maximum ReAct iterations. A value of -1 (default) means "use the global default
     * from {@link com.yomahub.liteflow.property.agent.DefaultsConfig}".
     */
    protected int maxIterations() { return -1; }

    /**
     * Whether to register the built-in {@link WorkspaceFileTools}. Default true.
     */
    protected boolean enableShellTool() { return true; }

    /**
     * Whether to register the built-in {@link ManagedShellCommandTool}. Default true.
     */
    protected boolean enableWorkspaceFileTools() { return true; }

    /**
     * Provide hooks for the agent. Returns an empty list by default.
     */
    protected List<Hook> hooks(ReActAgentContext ctx) { return List.of(); }

    /**
     * Called after the agent replies. The default implementation writes
     * {@code reply.getTextContent()} into the slot's response data.
     */
    protected void handleReply(Msg reply, ReActAgentContext ctx) {
        ctx.getSlot().setResponseData(reply == null ? null : reply.getTextContent());
    }

    /* ===== Framework final execution body ===== */

    /**
     * Executes the ReActAgent within a managed session.
     * <ol>
     *   <li>Acquires (or creates) an {@link AgentSession} keyed by session id</li>
     *   <li>Builds a {@link ReActAgent} on first use, then reuses it</li>
     *   <li>Calls the agent with the user prompt and handles the reply</li>
     * </ol>
     * This method is {@code final} to ensure correct session locking.
     */
    @Override
    public final void process() throws Exception {
        AgentSessionManager mgr = AgentSessionManagerHolder.getOrCreate(agentConfig());
        Slot slot = this.getSlot();
        String sid = resolveSessionId(slot);
        AgentSession session = mgr.acquire(sid);
        session.getLock().lock();
        try {
            ReActAgentContext ctx = new ReActAgentContext(slot, session.getSessionId(), session.getWorkspaceDir());
            ReActAgent agent = (ReActAgent) session.getAgent();
            if (agent == null) {
                agent = buildAgent(ctx);
                session.setAgent(agent);
            }
            Msg userMsg = Msg.builder().textContent(userPrompt(ctx)).build();
            Msg reply = agent.call(List.of(userMsg)).block();
            handleReply(reply, ctx);
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

        return ReActAgent.builder()
                .name(getNodeId() == null ? "liteflow-agent" : getNodeId())
                .sysPrompt(systemPrompt(ctx))
                .model(buildModel(ctx))
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(iters)
                .hooks(hooks(ctx))
                .build();
    }

    /** Holds singleton AgentSessionManager; lazily created on first process() */
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
