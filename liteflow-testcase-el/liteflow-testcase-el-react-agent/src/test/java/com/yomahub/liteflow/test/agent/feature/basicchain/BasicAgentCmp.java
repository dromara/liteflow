package com.yomahub.liteflow.test.agent.feature.basicchain;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基础链路 Agent。同时捕获 ctx()、systemPrompt/userPrompt/handleReply 调用次数，
 * 让 {@code BasicChainTest} 一次链路调用即可断言多个基础行为。
 */
@Component("basicAgent")
public class BasicAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<Path> SEEN_WORKSPACE = new AtomicReference<>();
    public static final AtomicReference<String> LAST_REPLY = new AtomicReference<>();
    public static final AtomicInteger SYSTEM_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger USER_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger HANDLE_REPLY_COUNT = new AtomicInteger();

    public static void reset() {
        PROBE.set(new AgentProbe());
        SEEN_CONVERSATION_ID.set(null);
        SEEN_AGENT_KEY.set(null);
        SEEN_WORKSPACE.set(null);
        LAST_REPLY.set(null);
        SYSTEM_PROMPT_COUNT.set(0);
        USER_PROMPT_COUNT.set(0);
        HANDLE_REPLY_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        SYSTEM_PROMPT_COUNT.incrementAndGet();
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt() {
        USER_PROMPT_COUNT.incrementAndGet();
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        SEEN_WORKSPACE.set(ctx().getWorkspaceDir());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }

    @Override
    protected void handleReply(Msg reply) {
        HANDLE_REPLY_COUNT.incrementAndGet();
        LAST_REPLY.set(reply == null ? null : reply.getTextContent());
        super.handleReply(reply);
    }
}
