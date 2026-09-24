package com.yomahub.liteflow.agent.jev;

/**
 * Immutable raw Noul answer, before applying the component's probability threshold.
 * The probability is the model's estimate that the answer is yes; Noul has no separate confidence.
 */
public record JevNoulResult(String model, double probability) {
}
