package com.yomahub.liteflow.agent.a2a.server;

import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.AssistantMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2aProtocolEventAdapterTest {

    private final A2aProtocolEventAdapter adapter = new A2aProtocolEventAdapter();

    @Test
    void mapsTypedTextDeltaAndFinalResultToTheCoarseA2aWireEvents() {
        AssistantMessage result = new AssistantMessage("complete");

        StepVerifier.create(adapter.adapt(Flux.just(
                        new TextBlockDeltaEvent("reply-1", "block-1", "hel"),
                        new AgentResultEvent(result))))
                .assertNext(event -> {
                    assertEquals("REASONING", event.getType().name());
                    assertEquals("hel", event.getMessage().getTextContent());
                    assertFalse(event.isLast());
                })
                .assertNext(event -> {
                    assertEquals("AGENT_RESULT", event.getType().name());
                    assertSame(result, event.getMessage());
                    assertTrue(event.isLast());
                })
                .verifyComplete();
    }

    @Test
    void preservesTypedStreamErrors() {
        IllegalStateException upstream = new IllegalStateException("model failed");

        StepVerifier.create(adapter.adapt(Flux.error(upstream)))
                .expectErrorMatches(failure -> failure == upstream)
                .verify();
    }

    @Test
    void rejectsInlineConfirmationInsteadOfApprovingIt() {
        RequireUserConfirmEvent confirmation =
                new RequireUserConfirmEvent("reply-2", List.of());

        StepVerifier.create(adapter.adapt(Flux.just(confirmation)))
                .expectErrorMatches(failure -> failure instanceof UnsupportedOperationException
                        && failure.getMessage().contains("inline confirmation"))
                .verify();
    }
}
