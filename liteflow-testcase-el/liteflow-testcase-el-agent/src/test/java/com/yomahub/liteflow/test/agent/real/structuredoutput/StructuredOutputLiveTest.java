package com.yomahub.liteflow.test.agent.real.structuredoutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * guide §7 结构化输出 的真实模型验证：
 * Java 类型反序列化、JSON Schema 输出、两种覆写互斥校验。
 */
@TestPropertySource("classpath:/real/structuredoutput/application.properties")
@SpringBootTest(classes = StructuredOutputLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.structuredoutput")
public class StructuredOutputLiveTest extends RealAgentTestBase {

    /** 方式一：responseData 直接是反序列化后的 record 对象。 */
    @Test
    public void typedOutputDeserializesIntoRecord() {
        String prompt = "请评估 LiteFlow 这个规则引擎框架。必须只输出一个 JSON 对象，"
                + "包含三个字段：answer（一句话中文总结）、score（1 到 10 的整数评分）、"
                + "recommend（布尔值，是否推荐）。不要输出 JSON 以外的任何内容。";

        LiteflowResponse response = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            response = flowExecutor.execute2Resp("realTypedOutputChain", prompt);
            if (response.isSuccess()) {
                break;
            }
            // 真实模型/中转端点对结构化输出约束偶发失效（metadata 缺 _structured_output）：
            // 换新会话重试最多 3 次，连续失败才视为功能问题。
        }

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(data instanceof RealStructuredOutputCmp.StructuredReply,
                "responseData must be StructuredReply but was " + className(data));
        RealStructuredOutputCmp.StructuredReply reply =
                (RealStructuredOutputCmp.StructuredReply) data;
        Assertions.assertNotNull(reply.answer());
        Assertions.assertFalse(reply.answer().isBlank());
        Assertions.assertTrue(reply.score() >= 1 && reply.score() <= 10,
                "score must be within 1..10, got: " + reply.score());
        assertHistoryHasResult(response);
    }

    /** 方式二：responseData 是符合 schema 的 JsonNode。 */
    @Test
    public void schemaOutputProducesJsonNode() {
        LiteflowResponse response = flowExecutor.execute2Resp("realSchemaOutputChain",
                "请给出中国南方一个著名旅游城市：city 填城市名，country 填国家名。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(data instanceof JsonNode,
                "responseData must be JsonNode but was " + className(data));
        JsonNode node = (JsonNode) data;
        Assertions.assertEquals("object", node.getNodeType().toString().toLowerCase(),
                "root must be an object node: " + node);
        Assertions.assertFalse(node.path("city").asText("").isBlank(),
                "city must be present: " + node);
        Assertions.assertEquals("中国", node.path("country").asText().strip(),
                "country must be 中国: " + node);
        assertHistoryHasResult(response);
    }

    private void assertHistoryHasResult(LiteflowResponse response) {
        try (var history = AgentConversationService.open(liteflowConfig.getAgent())) {
            var messages = history.messages(
                    response.getConversationId(), 0, 20).items();
            Assertions.assertEquals(2, messages.size());
            Assertions.assertEquals("result", messages.get(1).stage());
            Assertions.assertFalse(messages.get(1).content().isBlank(), "Structured reply must be visible in history");
        }
    }

    /** §7/§18：两种结构化输出覆写同时存在 → 构建期 AgentConfigException。 */
    @Test
    public void conflictingOutputOverridesFailFast() {
        LiteflowResponse response = flowExecutor.execute2Resp("realConflictingOutputChain",
                "任意问题");

        Assertions.assertFalse(response.isSuccess(),
                "conflicting structured output overrides must fail the chain");
        Assertions.assertTrue(cause(response).contains(
                        "structuredOutputType and structuredOutputSchema are mutually exclusive"),
                "unexpected cause: " + cause(response));
    }

    private static String className(Object data) {
        return data == null ? "null" : data.getClass().getName();
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
