package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import io.agentscope.core.model.Model;

/** ReAct component base whose model boundary is always deterministic and in-process. */
public abstract class OfflineReActAgentComponent extends ReActAgentComponent {

    @Override
    protected final ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        return ScriptedChatModel.builder().reply("deterministic offline reply").build();
    }
}
