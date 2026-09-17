package com.yomahub.liteflow.agent.model.catalog;

/** Null means the source does not report that limit. All reported values are positive. */
public record ModelLimits(Integer context, Integer input, Integer output) {
    public ModelLimits {
        if ((context != null && context <= 0) || (input != null && input <= 0)
                || (output != null && output <= 0)) {
            throw new IllegalArgumentException("Model limits must be positive when reported");
        }
    }

    public boolean hasInputCapacity() {
        return context != null || input != null;
    }
}
