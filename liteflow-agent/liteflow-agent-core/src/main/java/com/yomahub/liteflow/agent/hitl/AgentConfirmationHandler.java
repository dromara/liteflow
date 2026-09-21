package com.yomahub.liteflow.agent.hitl;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import reactor.core.publisher.Mono;

import java.util.List;

/** Supplies decisions for one AgentScope permission-confirmation request. */
@FunctionalInterface
public interface AgentConfirmationHandler {

    Mono<List<ConfirmResult>> confirm(
            RequireUserConfirmEvent event, LiteFlowAgentContext context);
}
