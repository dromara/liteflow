package com.yomahub.liteflow.agent.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.Msg;

import java.util.Objects;

/** Writes text or structured AgentScope replies without retaining stale Slot data. */
public final class AgentReplyHandler {

    private AgentReplyHandler() {
    }

    public static void handle(Msg reply, AgentOutputSpec output, Slot slot) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(slot, "slot");
        slot.clearResponseData();
        if (reply == null) {
            return;
        }
        try {
            Object response = switch (output.kind()) {
                case TEXT -> reply.getTextContent();
                case JAVA_TYPE -> reply.getStructuredData(output.javaType());
                case JSON_SCHEMA -> reply.getStructuredData(JsonNode.class);
            };
            if (response != null) {
                slot.setResponseData(response);
            }
        } catch (RuntimeException failure) {
            throw new AgentInvocationException(
                    AgentInvocationErrorType.STRUCTURED_OUTPUT,
                    "Failed to extract " + output.kind() + " agent reply",
                    failure);
        }
    }
}
