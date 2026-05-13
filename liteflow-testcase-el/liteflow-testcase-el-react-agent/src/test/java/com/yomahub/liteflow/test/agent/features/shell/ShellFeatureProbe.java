package com.yomahub.liteflow.test.agent.features.shell;

import java.util.concurrent.atomic.AtomicReference;

/**
 * shell 功能包的测试观测点。
 */
public final class ShellFeatureProbe {
    public static final AtomicReference<String> WORKSPACE = new AtomicReference<>();
    public static final AtomicReference<String> PWD_OUTPUT = new AtomicReference<>();
    public static final AtomicReference<String> DENIED_OUTPUT = new AtomicReference<>();

    private ShellFeatureProbe() {
    }

    public static void reset() {
        WORKSPACE.set(null);
        PWD_OUTPUT.set(null);
        DENIED_OUTPUT.set(null);
    }
}
