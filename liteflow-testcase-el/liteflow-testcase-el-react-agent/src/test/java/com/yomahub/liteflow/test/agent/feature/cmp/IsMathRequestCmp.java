package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * IF 路由用的布尔节点：根据请求参数中的 "type" 字段判定走哪个分支。
 */
@Component("isMathRequest")
public class IsMathRequestCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object t = map.get("type");
            return t != null && "math".equals(t.toString());
        }
        return false;
    }
}
