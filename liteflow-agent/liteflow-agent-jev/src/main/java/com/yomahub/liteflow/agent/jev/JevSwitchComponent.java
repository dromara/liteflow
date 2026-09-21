package com.yomahub.liteflow.agent.jev;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.JevConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single Jev Choice evaluation that returns a target ID to LiteFlow's SWITCH.
 * Instances are shared: keep invocation state in local variables or context beans.
 */
public abstract class JevSwitchComponent extends NodeSwitchComponent {

    /** Reserved model option, mapped to SWITCH's DEFAULT rather than a target ID. */
    public static final String NO_MATCH = "__liteflow_no_match__";

    protected abstract Object state();

    protected abstract String instructions();

    /** Target IDs mapped to descriptions. IDs must resolve uniquely in the current .to(...). */
    protected abstract Map<String, String> choices();

    protected double minConfidence() {
        return jevConfig().getMinConfidence();
    }

    /** Called on the flow thread for every valid answer, including uncertain/no-match answers. */
    protected void onDecision(JevChoiceResult result) {
    }

    protected final JevConfig jevConfig() {
        AgentConfig agent = LiteflowConfigGetter.get().getAgent();
        if (agent == null || agent.getJev() == null) {
            throw new IllegalArgumentException("liteflow.agent.jev must be configured before use");
        }
        return agent.getJev();
    }

    @Override
    public final String processSwitch() throws Exception {
        double threshold = minConfidence();
        if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Jev minConfidence must be between 0 and 1");
        }
        Map<String, String> configured = choices();
        if (configured == null || configured.isEmpty() || configured.size() > 254) {
            throw new IllegalArgumentException("Jev choices must contain 1 to 254 targets");
        }
        Map<String, String> criteria = new LinkedHashMap<>(configured);
        List<String> targets = getTargetList();
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            String id = entry.getKey();
            if (id == null || id.isBlank() || id.contains(":") || NO_MATCH.equals(id)
                    || Collections.frequency(targets, id) != 1) {
                throw new IllegalArgumentException("Jev choice must be a unique ID in the current SWITCH.to(): " + id);
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("Jev choice description must not be blank: " + id);
            }
        }
        String question = instructions();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Jev instructions must not be blank");
        }
        criteria.put(NO_MATCH, "None of the other options fits, or the input lacks enough information to choose one.");
        JevChoiceResult result = JevChoiceClient.evaluate(jevConfig(), state(), question, criteria);
        onDecision(result);
        // Slot rejects null; an empty target is the existing SWITCH DEFAULT convention.
        return NO_MATCH.equals(result.choice()) || result.confidence() < threshold ? "" : result.choice();
    }
}
