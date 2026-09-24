package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.property.agent.JevConfig;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Jev Choice protocol over TypeSafe's System One or OpenRouter's Decisions API. */
final class JevChoiceClient {
    private static final String QUESTION_ID = "route";

    private JevChoiceClient() {
    }

    static JevChoiceResult evaluate(JevConfig config, Object state, String instructions,
                                    Map<String, String> criteria) throws InterruptedException {
        JsonNode root = JevTransport.post(config, state, QUESTION_ID, Map.of(
                "type", "choice", "instructions", instructions, "criteria", criteria));
        return parse(root, criteria.keySet());
    }

    static URI endpoint(JevConfig config) {
        return JevTransport.endpoint(config);
    }

    private static JevChoiceResult parse(JsonNode root, Set<String> expectedOptions) {
        JsonNode answer = JevTransport.answer(root, QUESTION_ID);
        String model = JevTransport.text(root.path("model"), "model");
        if (!"choice".equals(JevTransport.text(answer.path("type"), "type"))) {
            throw new JevInvocationException("Jev answer type must be choice");
        }
        String choice = JevTransport.text(answer.path("choice"), "choice");
        if (!expectedOptions.contains(choice)) {
            throw new JevInvocationException("Jev returned a choice outside the supplied options");
        }
        double confidence = JevTransport.probability(answer.path("confidence"));
        JsonNode values = answer.path("probabilities");
        if (!values.isObject() || values.size() != expectedOptions.size()) {
            throw new JevInvocationException("Jev probabilities must cover exactly the supplied options");
        }
        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (String option : expectedOptions) {
            probabilities.put(option, JevTransport.probability(values.path(option)));
        }
        double sum = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        double max = probabilities.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (Math.abs(sum - 1) > 0.001 || probabilities.get(choice) + 0.000001 < max) {
            throw new JevInvocationException("Jev probability distribution is inconsistent with the selected choice");
        }
        return new JevChoiceResult(model, choice, confidence, probabilities);
    }
}
