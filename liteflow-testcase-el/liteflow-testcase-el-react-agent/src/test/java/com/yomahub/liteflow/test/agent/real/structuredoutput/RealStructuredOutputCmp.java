package com.yomahub.liteflow.test.agent.real.structuredoutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.springframework.stereotype.Component;

/**
 * guide §7 结构化输出 的真实模型组件。
 */
final class RealStructuredOutputCmp {

    private RealStructuredOutputCmp() {
    }

    /** structuredOutputType 反序列化目标。 */
    public record StructuredReply(String answer, int score, boolean recommend) {
    }

    abstract static class AbstractStructuredAgent extends ReActAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是结构化输出助手，严格按照用户要求的 JSON 结构作答，不要输出多余内容。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }
    }

    /** 方式一：Java 类型。 */
    @Component("realTypedOutputAgent")
    static class TypedOutputAgentCmp extends AbstractStructuredAgent {

        @Override
        protected Class<?> structuredOutputType() {
            return StructuredReply.class;
        }
    }

    /** 方式二：JSON Schema。 */
    @Component("realSchemaOutputAgent")
    static class SchemaOutputAgentCmp extends AbstractStructuredAgent {

        static final JsonNode CITY_SCHEMA = buildSchema();

        private static JsonNode buildSchema() {
            ObjectNode schema = JsonNodeFactory.instance.objectNode();
            schema.put("type", "object");
            ObjectNode properties = schema.putObject("properties");
            properties.putObject("city").put("type", "string");
            properties.putObject("country").put("type", "string");
            schema.putArray("required").add("city").add("country");
            return schema;
        }

        @Override
        protected JsonNode structuredOutputSchema() {
            return CITY_SCHEMA;
        }
    }

    /** §7/§18：两种方式同时覆写 → 构建期 AgentConfigException。 */
    @Component("realConflictingOutputAgent")
    static class ConflictingOutputAgentCmp extends AbstractStructuredAgent {

        @Override
        protected Class<?> structuredOutputType() {
            return StructuredReply.class;
        }

        @Override
        protected JsonNode structuredOutputSchema() {
            return SchemaOutputAgentCmp.CITY_SCHEMA;
        }
    }
}
