package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.property.agent.HarnessMemoryConfig;
import io.agentscope.harness.agent.memory.MemoryConfig;

/** Applies LiteFlow's flush policy without losing the component's other memory settings. */
final class HarnessMemoryConfigResolver {

    private HarnessMemoryConfigResolver() {
    }

    static MemoryConfig resolve(MemoryConfig component, HarnessMemoryConfig properties) {
        properties.validate();
        MemoryConfig base = component == null ? MemoryConfig.defaults() : component;
        MemoryConfig.FlushTrigger trigger = switch (properties.getFlushMode()) {
            case ALWAYS -> MemoryConfig.FlushTrigger.always();
            case NEVER -> MemoryConfig.FlushTrigger.never();
            case THROTTLED -> MemoryConfig.FlushTrigger.throttled(properties.getFlushMinGap());
        };
        return MemoryConfig.builder()
                .model(base.model())
                .flushPrompt(base.flushPrompt())
                .consolidationPrompt(base.consolidationPrompt())
                .consolidationMaxTokens(base.consolidationMaxTokens())
                .consolidationMinGap(base.consolidationMinGap())
                .dailyFileRetentionDays(base.dailyFileRetentionDays())
                .sessionRetentionDays(base.sessionRetentionDays())
                .flushTrigger(trigger)
                .build();
    }
}
