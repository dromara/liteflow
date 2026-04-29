package com.yomahub.liteflow.agent.anthropic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnthropicModelFactoryTest {
    @Test void construct_ok() { assertNotNull(AnthropicModelFactory.of("k", "claude-sonnet-4-6")); }
}