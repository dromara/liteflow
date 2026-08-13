package com.yomahub.liteflow.test.agent.feature.hitl;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/hitl/application.properties")
@SpringBootTest(classes = {HitlChainTest.class, HitlGuardConfiguration.class})
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.hitl")
public class HitlChainTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void resetFixture() {
        HitlAgentCmp.reset();
    }

    @Test
    void approvalResumesWithMetadataOnlyWhileHoldingOneInvocationLease() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "hitlChain", "approve",
                ExecuteOption.of().conversationId("conversation-hitl"));

        assertTrue(response.isSuccess(), () -> String.valueOf(response.getCause()));
        assertEquals("approved", response.getSlot().getResponseData());
        assertEquals(List.of(1, 1, 1), HitlAgentCmp.activeLeaseObservations());
        assertEquals(0, HitlAgentCmp.guard().active());

        List<Msg> continuation = HitlAgentCmp.continuationInput();
        assertEquals(1, continuation.size());
        Msg resume = continuation.get(0);
        assertTrue(resume instanceof UserMessage);
        assertTrue(resume.getContent().isEmpty());
        assertEquals(1, resume.getMetadata().size());
        assertTrue(resume.getMetadata().containsKey(Msg.METADATA_CONFIRM_RESULTS));
        @SuppressWarnings("unchecked")
        List<ConfirmResult> results = (List<ConfirmResult>) resume.getMetadata()
                .get(Msg.METADATA_CONFIRM_RESULTS);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isConfirmed());
        assertFalse(resume.getMetadata().containsKey(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID));
    }
}

@TestConfiguration
class HitlGuardConfiguration {
    @Bean("hitlGuard")
    HitlRecordingGuard hitlGuard() {
        return HitlAgentCmp.guard();
    }
}
