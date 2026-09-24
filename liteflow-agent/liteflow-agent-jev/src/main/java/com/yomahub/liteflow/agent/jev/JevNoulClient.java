package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.property.agent.JevConfig;

import java.util.Map;

/** Jev Noul protocol over TypeSafe's System One or OpenRouter's Decisions API. */
final class JevNoulClient {
    private static final String QUESTION_ID = "judge";

    private JevNoulClient() {
    }

    static JevNoulResult evaluate(JevConfig config, Object state, String instructions) throws InterruptedException {
        JsonNode root = JevTransport.post(config, state, QUESTION_ID,
                Map.of("type", "noul", "instructions", instructions));
        JsonNode answer = JevTransport.answer(root, QUESTION_ID);
        String model = JevTransport.text(root.path("model"), "model");
        if (!"noul".equals(JevTransport.text(answer.path("type"), "type"))) {
            throw new JevInvocationException("Jev answer type must be noul");
        }
        return new JevNoulResult(model, JevTransport.probability(answer.path("noul")));
    }
}
