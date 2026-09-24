package com.yomahub.liteflow.agent.jev;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.property.agent.JevConfig;

/**
 * A single Jev Noul evaluation that returns a boolean to LiteFlow's IF.
 * Instances are shared: keep invocation state in local variables or context beans.
 */
public abstract class JevBooleanComponent extends NodeBooleanComponent {

    protected abstract Object state();

    protected abstract String instructions();

    /** Probability at or above which the Noul answer counts as yes. */
    protected double probabilityThreshold() {
        return jevConfig().getNoulThreshold();
    }

    /** Called on the flow thread for every valid answer, including uncertain ones near the threshold. */
    protected void onDecision(JevNoulResult result) {
    }

    protected final JevConfig jevConfig() {
        return JevTransport.requireConfig();
    }

    @Override
    public final boolean processBoolean() throws Exception {
        double threshold = probabilityThreshold();
        if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Jev noulThreshold must be between 0 and 1");
        }
        String question = instructions();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Jev instructions must not be blank");
        }
        JevNoulResult result = JevNoulClient.evaluate(jevConfig(), state(), question);
        onDecision(result);
        return result.probability() >= threshold;
    }
}
