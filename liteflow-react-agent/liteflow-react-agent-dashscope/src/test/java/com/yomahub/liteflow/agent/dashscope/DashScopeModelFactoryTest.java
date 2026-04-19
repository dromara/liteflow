package com.yomahub.liteflow.agent.dashscope;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DashScopeModelFactoryTest {
    @Test void construct_ok() { assertNotNull(DashScopeModelFactory.of("k", "qwen3-max")); }
}