package com.yomahub.liteflow.agent.jev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable raw decision, before applying the component's confidence threshold. */
public record JevChoiceResult(String model, String choice, double confidence,
                              Map<String, Double> probabilities) {
    public JevChoiceResult {
        probabilities = Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }
}
