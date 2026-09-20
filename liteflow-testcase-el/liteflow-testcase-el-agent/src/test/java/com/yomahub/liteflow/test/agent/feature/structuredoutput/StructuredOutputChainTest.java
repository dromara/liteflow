package com.yomahub.liteflow.test.agent.feature.structuredoutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/structuredoutput/application.properties")
@SpringBootTest(classes = StructuredOutputChainTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.structuredoutput")
public class StructuredOutputChainTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    void pojoAndJsonSchemaRepliesAreStoredAsStructuredObjects() {
        LiteflowResponse pojo = execute("structuredPojoChain", "pojo");
        LiteflowResponse schema = execute("structuredSchemaChain", "schema");

        assertTrue(pojo.isSuccess());
        StructuredReply pojoReply = pojo.getSlot().getResponseData();
        assertEquals("typed", pojoReply.answer());

        assertTrue(schema.isSuccess());
        JsonNode schemaReply = schema.getSlot().getResponseData();
        assertEquals("schema", schemaReply.path("answer").asText());
    }

    @Test
    void emptyReplyClearsPreviouslySeededSlotValue() {
        LiteflowResponse response = execute("structuredEmptyChain", "empty");

        assertTrue(response.isSuccess());
        assertNull(response.getSlot().getResponseData(),
                "empty agent completion must not leave stale response data");
    }

    private LiteflowResponse execute(String chain, String prompt) {
        return flowExecutor.execute2Resp(
                chain, prompt, ExecuteOption.of().conversationId("conversation-" + chain));
    }

    public record StructuredReply(String answer) {
    }
}

abstract class OfflineStructuredComponent extends HarnessAgentComponent {

    @Override
    protected final ModelSpec<?> model() {
        throw new AssertionError("offline buildModel override must be used");
    }

    @Override
    protected final String systemPrompt() {
        return "Return the requested structured fixture.";
    }

    @Override
    protected final String userPrompt(LiteFlowAgentContext context) {
        return "fixture";
    }
}

@Component("structuredPojoAgent")
final class StructuredPojoAgentCmp extends OfflineStructuredComponent {
    @Override protected Model buildModel() {
        return ScriptedChatModel.nativeStructured()
                .reply("{\"answer\":\"typed\"}").build();
    }
    @Override protected Class<?> structuredOutputType() {
        return StructuredOutputChainTest.StructuredReply.class;
    }
}

@Component("structuredSchemaAgent")
final class StructuredSchemaAgentCmp extends OfflineStructuredComponent {
    private static final JsonNode SCHEMA = new ObjectMapper().createObjectNode()
            .put("type", "object")
            .set("properties", new ObjectMapper().createObjectNode()
                    .set("answer", new ObjectMapper().createObjectNode().put("type", "string")));
    @Override protected Model buildModel() {
        return ScriptedChatModel.nativeStructured()
                .reply("{\"answer\":\"schema\"}").build();
    }
    @Override protected JsonNode structuredOutputSchema() { return SCHEMA; }
}

@Component("seedStructuredResponse")
final class SeedStructuredResponseCmp extends NodeComponent {
    @Override public void process() { getSlot().setResponseData("stale"); }
}

@Component("structuredEmptyAgent")
final class StructuredEmptyAgentCmp extends OfflineStructuredComponent {
    @Override protected Model buildModel() {
        return ScriptedChatModel.builder().reply("unused").build();
    }
    @Override protected Mono<Msg> invokeRuntime(
            HarnessAgentRuntime runtime, java.util.List<Msg> input, AgentOutputSpec output,
            RuntimeContext runtimeContext, LiteFlowAgentContext liteflowContext) {
        return Mono.empty();
    }
}
