package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆写 resolveConversationId，根据请求 Map 中的 userId 拼接稳定 cid，
 * 用于演示 guide §5.2 中"按业务请求对象多轮对话"的样例。
 */
@Component("resolveCidAgent")
public class ResolveCidAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<String> SEEN_CID = new AtomicReference<>();

    public static void reset() {
        SEEN_CID.set(null);
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
            if (p != null) return p.toString();
        }
        return reqData == null ? "" : reqData.toString();
    }
}
