package com.yomahub.liteflow.test.agent.features.skills.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * skills 功能包的准备节点。
 */
@Component("skillsPrepare")
public class SkillsPrepareCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setChainReqData(getSlot().getChainId(), getRequestData());
    }
}
