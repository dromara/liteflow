package com.yomahub.liteflow.test.agent.features.skills.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * skills 功能包的记录节点。
 */
@Component("skillsRecord")
public class SkillsRecordCmp extends NodeComponent {
    @Override
    public void process() {
        getSlot().setOutput(getNodeId(), getSlot().getResponseData());
    }
}
