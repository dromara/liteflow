package com.yomahub.liteflow.test.agent.feature.a2a;

import com.yomahub.liteflow.agent.a2a.A2aAgentComponent;
import com.yomahub.liteflow.agent.a2a.A2aClientRuntimeFactory;
import com.yomahub.liteflow.agent.a2a.RecordingA2aRuntimeFactory;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component("a2aAgent")
public final class A2aAgentCmp extends A2aAgentComponent {

    private static final AtomicInteger RUNTIME_BUILDS = new AtomicInteger();
    static void reset() {
        RUNTIME_BUILDS.set(0);
        RecordingA2aRuntimeFactory.reset();
    }

    static int runtimeBuilds() { return RUNTIME_BUILDS.get(); }
    static void failNext(Throwable failure) { RecordingA2aRuntimeFactory.failNext(failure); }

    @Override protected String remoteAgentName() { return "offline-remote"; }
    @Override protected AgentCardResolver agentCardResolver() { return name -> null; }
    @Override protected String userPrompt(LiteFlowAgentContext context) {
        Object request = getSlot().getChainReqData(getSlot().getChainId());
        return request == null ? "" : request.toString();
    }

    @Override
    protected A2aClientRuntimeFactory a2aClientRuntimeFactory() {
        RUNTIME_BUILDS.incrementAndGet();
        return RecordingA2aRuntimeFactory.create();
    }
}
