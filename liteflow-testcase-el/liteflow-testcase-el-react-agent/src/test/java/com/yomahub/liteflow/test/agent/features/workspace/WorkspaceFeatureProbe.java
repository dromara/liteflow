package com.yomahub.liteflow.test.agent.features.workspace;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * workspace 功能包的测试观测点。
 */
public final class WorkspaceFeatureProbe {
    public static final AtomicReference<String> TRUNCATED_READ = new AtomicReference<>();
    public static final AtomicReference<List<String>> LIST_RESULT = new AtomicReference<>(List.of());
    public static final AtomicReference<String> RELATIVE_ESCAPE_DENIED = new AtomicReference<>();
    public static final AtomicReference<String> ABSOLUTE_ESCAPE_DENIED = new AtomicReference<>();

    private WorkspaceFeatureProbe() {
    }

    public static void reset() {
        TRUNCATED_READ.set(null);
        LIST_RESULT.set(List.of());
        RELATIVE_ESCAPE_DENIED.set(null);
        ABSOLUTE_ESCAPE_DENIED.set(null);
    }
}
