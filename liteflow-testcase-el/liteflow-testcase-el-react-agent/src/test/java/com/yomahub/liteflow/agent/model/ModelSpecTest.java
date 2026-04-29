package com.yomahub.liteflow.agent.model;

import io.agentscope.core.model.Model;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelSpecTest {

    /** 仅用于测试的最小 ModelSpec 子类。 */
    static class TestSpec extends ModelSpec<TestSpec> {
        @Override protected Model resolve(AgentConfig cfg) { return null; }
    }

    @Test
    void fluentSettersReturnSelfAndStoreValues() {
        TestSpec spec = new TestSpec()
                .temperature(0.5)
                .topP(0.9)
                .topK(40)
                .maxTokens(1024)
                .seed(42L)
                .stream(true)
                .cacheControl(true);

        assertEquals(0.5, spec.getTemperature());
        assertEquals(0.9, spec.getTopP());
        assertEquals(40, spec.getTopK());
        assertEquals(1024, spec.getMaxTokens());
        assertEquals(42L, spec.getSeed());
        assertEquals(Boolean.TRUE, spec.getStream());
        assertEquals(Boolean.TRUE, spec.getCacheControl());
    }

    @Test
    void unsetValuesReturnNull() {
        TestSpec spec = new TestSpec();
        assertNull(spec.getTemperature());
        assertNull(spec.getTopP());
        assertNull(spec.getTopK());
        assertNull(spec.getMaxTokens());
        assertNull(spec.getSeed());
        assertNull(spec.getStream());
        assertNull(spec.getCacheControl());
    }
}
