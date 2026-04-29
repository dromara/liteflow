package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

/**
 * 布尔节点：判定本次提问是否是计算题（用于 IF EL 路由演示）。
 */
@Component("isMath")
public class IsMathCmp extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        Object req = this.getRequestData();
        if (req == null) return false;
        String s = req.toString();
        return s.matches(".*\\d.*[+\\-*/].*\\d.*");
    }
}
