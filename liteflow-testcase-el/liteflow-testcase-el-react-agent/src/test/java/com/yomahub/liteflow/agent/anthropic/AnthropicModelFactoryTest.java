package com.yomahub.liteflow.agent.anthropic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnthropicModelFactoryTest {
    @Test void construct_ok() { assertNotNull(AnthropicModelFactory.of("k", "claude-sonnet-4-6")); }
    @Test void custom_base_url_ok() {
        assertNotNull(AnthropicModelFactory.custom("k", "https://anthropic-proxy.example.com", "claude-sonnet-4-6"));
    }
}
