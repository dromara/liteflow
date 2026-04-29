package com.yomahub.liteflow.agent.gemini;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeminiModelFactoryTest {
    @Test void plain_ok() { assertNotNull(GeminiModelFactory.of("k", "gemini-3-flash-preview")); }
}