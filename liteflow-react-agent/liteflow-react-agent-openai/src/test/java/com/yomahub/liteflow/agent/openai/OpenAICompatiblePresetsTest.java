package com.yomahub.liteflow.agent.openai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAICompatiblePresetsTest {
    @Test void deepseek_ok() { assertNotNull(OpenAICompatiblePresets.deepseek("k", "deepseek-chat")); }
    @Test void kimi_ok()    { assertNotNull(OpenAICompatiblePresets.kimi("k", "moonshot-v1-8k")); }
    @Test void glm_ok()     { assertNotNull(OpenAICompatiblePresets.glm("k", "glm-4")); }
    @Test void minimax_ok() { assertNotNull(OpenAICompatiblePresets.minimax("k", "abab6.5s-chat")); }
}