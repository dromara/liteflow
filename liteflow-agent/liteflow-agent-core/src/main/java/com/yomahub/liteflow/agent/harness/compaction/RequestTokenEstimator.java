package com.yomahub.liteflow.agent.harness.compaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolSchema;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Local estimate of the complete input, including system messages, tools and output schemas. */
final class RequestTokenEstimator {
    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        // Build common Jackson serializers before an Agent's execution deadline starts.
        estimate(List.of(Msg.builder().role(io.agentscope.core.message.MsgRole.USER)
                        .content(io.agentscope.core.message.TextBlock.builder().text("warmup").build()).build()),
                List.of(ToolSchema.builder().name("warmup").description("warmup")
                        .parameters(java.util.Map.of("type", "object")).build()), null);
    }

    static void initialize() { }

    static long estimate(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        long tokens = 0;
        if (messages != null) {
            for (Msg message : messages) {
                tokens += 12 + estimateValue(message.getContent()) + estimateValue(message.getName());
            }
        }
        tokens += estimateValue(tools);
        if (options != null && options.getResponseFormat() != null) tokens += estimateValue(options.getResponseFormat());
        return tokens;
    }

    private static long estimateValue(Object value) {
        if (value == null) return 0;
        Counter counter = new Counter();
        try { JSON.writeValue(counter, value); }
        catch (IOException failure) { throw new AgentConfigException("Cannot estimate model request tokens", failure); }
        return (counter.units + 2) / 3;
    }

    /** No intermediate JSON string; non-ASCII text gets a larger allowance than ASCII. */
    private static final class Counter extends Writer {
        long units;
        @Override public void write(char[] buffer, int offset, int length) {
            for (int i = offset; i < offset + length; i++) units += buffer[i] < 128 ? 1 : 6;
        }
        @Override public void flush() { }
        @Override public void close() { }
    }
}
