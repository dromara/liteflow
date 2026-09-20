package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelSpecTest {

    @Test
    void noConfiguredOptionsLeaveProviderDefaultsUntouched() {
        assertNull(new TestSpec().merge(null));
    }

    @Test
    void explicitlySuppliedEmptyNativeOptionsRemainPresent() {
        GenerateOptions nativeOptions = GenerateOptions.builder().build();

        GenerateOptions merged = new TestSpec()
                .generateOptions(nativeOptions)
                .merge(null);

        assertSame(nativeOptions, merged);
    }

    @Test
    void commonFluentOptionsCoverEveryProviderNeutralFieldAndPreserveSelfType() {
        ExecutionConfig executionConfig = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(12))
                .maxAttempts(2)
                .build();
        TestSpec spec = new TestSpec();

        TestSpec chained = spec
                .apiKey("spec-key")
                .baseUrl("https://spec.example")
                .temperature(0.0)
                .topP(0.0)
                .topK(0)
                .maxTokens(0)
                .maxCompletionTokens(0)
                .seed(0L)
                .stream(false)
                .cacheControl(false)
                .parallelToolCalls(false)
                .executionConfig(executionConfig)
                .additionalHeader("X-Common", "header")
                .additionalBodyParam("commonBody", 7)
                .additionalQueryParam("commonQuery", "query");

        assertSame(spec, chained);
        assertEquals("spec-key", spec.getApiKey());
        assertEquals("https://spec.example", spec.getBaseUrl());

        GenerateOptions merged = spec.merge(null);
        assertEquals(0.0, merged.getTemperature());
        assertEquals(0.0, merged.getTopP());
        assertEquals(0, merged.getTopK());
        assertEquals(0, merged.getMaxTokens());
        assertEquals(0, merged.getMaxCompletionTokens());
        assertEquals(0L, merged.getSeed());
        assertFalse(merged.getStream());
        assertFalse(merged.getCacheControl());
        assertFalse(merged.getParallelToolCalls());
        assertSame(executionConfig, merged.getExecutionConfig());
        assertEquals(Map.of("X-Common", "header"), merged.getAdditionalHeaders());
        assertEquals(Map.of("commonBody", 7), merged.getAdditionalBodyParams());
        assertEquals(Map.of("commonQuery", "query"), merged.getAdditionalQueryParams());
    }

    @Test
    void nativeOverridesProviderAndProviderOverridesCommonWithoutDroppingOtherValues() {
        ExecutionConfig commonExecution = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(10))
                .maxAttempts(2)
                .initialBackoff(Duration.ofSeconds(1))
                .build();
        ExecutionConfig providerExecution = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(20))
                .maxBackoff(Duration.ofSeconds(8))
                .build();
        ExecutionConfig nativeExecution = ExecutionConfig.builder()
                .maxAttempts(4)
                .backoffMultiplier(3.0)
                .build();
        GenerateOptions providerOptions = GenerateOptions.builder()
                .temperature(0.5)
                .maxTokens(200)
                .frequencyPenalty(0.3)
                .presencePenalty(0.4)
                .executionConfig(providerExecution)
                .additionalHeader("shared", "provider")
                .additionalHeader("provider", "header")
                .additionalBodyParam("shared", "provider")
                .additionalBodyParam("provider", 2)
                .additionalQueryParam("shared", "provider")
                .additionalQueryParam("provider", "query")
                .build();
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .temperature(0.9)
                .maxCompletionTokens(900)
                .executionConfig(nativeExecution)
                .additionalHeader("shared", "native")
                .additionalHeader("native", "header")
                .additionalBodyParam("shared", "native")
                .additionalBodyParam("native", 3)
                .additionalQueryParam("shared", "native")
                .additionalQueryParam("native", "query")
                .build();
        TestSpec spec = new TestSpec()
                .temperature(0.1)
                .topP(0.2)
                .maxTokens(100)
                .maxCompletionTokens(101)
                .executionConfig(commonExecution)
                .additionalHeader("shared", "common")
                .additionalHeader("common", "header")
                .additionalBodyParam("shared", "common")
                .additionalBodyParam("common", 1)
                .additionalQueryParam("shared", "common")
                .additionalQueryParam("common", "query")
                .generateOptions(nativeOptions);

        GenerateOptions merged = spec.merge(providerOptions);

        assertEquals(0.9, merged.getTemperature());
        assertEquals(0.2, merged.getTopP());
        assertEquals(200, merged.getMaxTokens());
        assertEquals(900, merged.getMaxCompletionTokens());
        assertEquals(0.3, merged.getFrequencyPenalty());
        assertEquals(0.4, merged.getPresencePenalty());
        assertEquals(Duration.ofSeconds(20), merged.getExecutionConfig().getTimeout());
        assertEquals(4, merged.getExecutionConfig().getMaxAttempts());
        assertEquals(Duration.ofSeconds(1), merged.getExecutionConfig().getInitialBackoff());
        assertEquals(Duration.ofSeconds(8), merged.getExecutionConfig().getMaxBackoff());
        assertEquals(3.0, merged.getExecutionConfig().getBackoffMultiplier());
        assertEquals(Map.of(
                "shared", "native",
                "common", "header",
                "provider", "header",
                "native", "header"), merged.getAdditionalHeaders());
        assertEquals(Map.of(
                "shared", "native",
                "common", 1,
                "provider", 2,
                "native", 3), merged.getAdditionalBodyParams());
        assertEquals(Map.of(
                "shared", "native",
                "common", "query",
                "provider", "query",
                "native", "query"), merged.getAdditionalQueryParams());

        assertEquals(Map.of("shared", "provider", "provider", "header"),
                providerOptions.getAdditionalHeaders());
        assertEquals(Map.of("shared", "native", "native", "header"),
                nativeOptions.getAdditionalHeaders());
    }

    @Test
    void commonMapsAreDefensivelyCopiedAndMergedMapsAreImmutable() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("header", "configured");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("body", 1);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("query", "configured");
        TestSpec spec = new TestSpec()
                .additionalHeaders(headers)
                .additionalBodyParams(body)
                .additionalQueryParams(query);

        headers.put("late", "header");
        body.put("late", 2);
        query.put("late", "query");

        GenerateOptions merged = spec.merge(null);
        assertEquals(Map.of("header", "configured"), merged.getAdditionalHeaders());
        assertEquals(Map.of("body", 1), merged.getAdditionalBodyParams());
        assertEquals(Map.of("query", "configured"), merged.getAdditionalQueryParams());
        assertEquals(Map.of("header", "configured", "late", "header"), headers);
        assertEquals(Map.of("body", 1, "late", 2), body);
        assertEquals(Map.of("query", "configured", "late", "query"), query);
        assertThrows(UnsupportedOperationException.class,
                () -> merged.getAdditionalHeaders().put("mutated", "value"));
        assertThrows(UnsupportedOperationException.class,
                () -> merged.getAdditionalBodyParams().put("mutated", 3));
        assertThrows(UnsupportedOperationException.class,
                () -> merged.getAdditionalQueryParams().put("mutated", "value"));
    }

    private static final class TestSpec extends ModelSpec<TestSpec> {

        GenerateOptions merge(GenerateOptions providerOptions) {
            return mergeGenerateOptions(providerOptions);
        }

        @Override
        public Model resolve(AgentConfig cfg) {
            return null;
        }
    }
}
