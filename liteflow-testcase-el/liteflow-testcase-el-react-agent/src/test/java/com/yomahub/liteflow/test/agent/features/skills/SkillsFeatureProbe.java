package com.yomahub.liteflow.test.agent.features.skills;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * skills 功能包的测试观测点。
 */
public final class SkillsFeatureProbe {
    public static final AtomicReference<List<String>> USED_SKILLS = new AtomicReference<>(List.of());
    public static final AtomicReference<List<String>> TOOL_NAMES = new AtomicReference<>(List.of());

    private SkillsFeatureProbe() {
    }

    public static void reset() {
        USED_SKILLS.set(List.of());
        TOOL_NAMES.set(new CopyOnWriteArrayList<>());
    }
}
