package com.yomahub.liteflow.test.agent.feature.conversationid;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 resolveConversationId，根据请求 Map 中的 userId/convId 拼接稳定 cid，
 * 演示 guide §5.2 中"按业务请求对象多轮对话"。
 */
@Component("resolveCidAgent")
public class ResolveCidAgentCmp extends ReActAgentComponent {

    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();

    public static void reset() {
        SEEN_CID.set(null);
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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
    protected String resolveConversationId() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            Object convId = map.get("convId");
            return "user-" + userId + "-conv-" + convId;
        }
        return super.resolveConversationId();
    }

    @Override
    protected String userPrompt() {
        SEEN_CID.set(ctx().getConversationId());
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object p = map.get("prompt");
            if (p != null) {
                return p.toString();
            }
        }
        return reqData == null ? "" : reqData.toString();
    }
}
