package com.yomahub.liteflow.test.agent.feature.conversationid;

import com.yomahub.liteflow.test.agent.support.OfflineAgentComponent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 resolveConversationId，根据请求 Map 中的 userId/convId 拼接稳定 cid，
 * 演示 guide §5.2 中"按业务请求对象多轮对话"。
 */
@Component("resolveCidAgent")
public class ResolveCidAgentCmp extends OfflineAgentComponent {

    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();

    public static void reset() {
        SEEN_CID.set(null);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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
    protected String resolveConversationId(com.yomahub.liteflow.slot.Slot slot) {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            Object convId = map.get("convId");
            return "user-" + userId + "-conv-" + convId;
        }
        return super.resolveConversationId(slot);
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        SEEN_CID.set(context.getConversationId());
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
