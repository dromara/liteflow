package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAICompatibleSpecTest {

    @Test
    void explicitCredentialsWorkWithoutCompatibleConfigurationAndBuildExactlyOnce() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);
        spec.apiKey("explicit-key").baseUrl("https://explicit.example/v1");

        spec.resolve(new AgentConfig());

        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("explicit-key", spec.apiKey),
                () -> assertEquals("https://explicit.example/v1", spec.baseUrl));
    }

    @Test
    void explicitCredentialsOverrideCompatibleConfiguration() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);
        spec.apiKey("explicit-key").baseUrl("https://explicit.example/v1");

        spec.resolve(compatibleConfig(
                "local", "configured-key", "https://configured.example/v1"));

        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("explicit-key", spec.apiKey),
                () -> assertEquals("https://explicit.example/v1", spec.baseUrl));
    }

    @Test
    void compatibleConfigurationSuppliesCredentials() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);

        spec.resolve(compatibleConfig(
                "local", "configured-key", "https://configured.example/v1"));

        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("configured-key", spec.apiKey),
                () -> assertEquals("https://configured.example/v1", spec.baseUrl));
    }

    @Test
    void blankExplicitBaseUrlFallsBackToConfiguredCompatibleBaseUrl() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);
        spec.apiKey("explicit-key").baseUrl("   ");

        spec.resolve(compatibleConfig(
                "local", "configured-key", "https://configured.example/v1"));

        assertEquals("https://configured.example/v1", spec.baseUrl);
    }

    @Test
    void configuredBaseUrlOverridesTemporaryPresetDefault() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec(
                "deepseek", "https://temporary-preset.example/v1");

        spec.resolve(compatibleConfig(
                "deepseek", "configured-key", "https://configured.example/v1"));

        assertEquals("https://configured.example/v1", spec.baseUrl);
    }

    @Test
    void temporaryThreeArgumentPresetPathRemainsSourceCompatibleForTaskFour() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec(
                "deepseek", "https://temporary-preset.example/v1");

        spec.resolve(compatibleConfig("deepseek", "configured-key", null));

        assertEquals("https://temporary-preset.example/v1", spec.baseUrl);
    }

    @Test
    void genericCustomRequiresAnEffectiveBaseUrlAndDoesNotFallBackToOpenAI() {
        OpenAISpec spec = OpenAICompatible.custom("local", "custom-model")
                .apiKey("explicit-key");

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(new AgentConfig()));

        assertEquals(
                "Missing base URL: please configure "
                        + "liteflow.agent.openai-compatible.local.base-url",
                failure.getMessage());
    }

    @Test
    void blankConfiguredBaseUrlFailsBeforeBuildWithCompatibleConfigPath() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(compatibleConfig("local", "configured-key", "   ")));

        assertEquals(
                "Missing base URL: please configure "
                        + "liteflow.agent.openai-compatible.local.base-url",
                failure.getMessage());
        assertEquals(0, spec.buildCount);
    }

    @Test
    void explicitlyBlankApiKeyFailsBeforeBuildWithExactCompatibleConfigPath() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);
        spec.apiKey("   ").baseUrl("https://explicit.example/v1");

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(compatibleConfig(
                        "local", "configured-key", "https://configured.example/v1")));

        assertEquals(
                "Missing API key: please configure "
                        + "liteflow.agent.openai-compatible.local.api-key",
                failure.getMessage());
        assertEquals(0, spec.buildCount);
    }

    @Test
    void missingCompatibleCredentialFailsBeforeBuildWithExactConfigPath() {
        CapturingCompatibleSpec spec = new CapturingCompatibleSpec("local", null);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(new AgentConfig()));

        assertEquals(
                "Missing platform credential: please configure "
                        + "liteflow.agent.openai-compatible.local.api-key",
                failure.getMessage());
        assertEquals(0, spec.buildCount);
    }

    private static AgentConfig compatibleConfig(
            String configKey, String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        config.getSessionStore().setJsonRoot("target/agent-state");
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        Map<String, PlatformCredential> compatible = new LinkedHashMap<>();
        compatible.put(configKey, credential);
        config.setOpenaiCompatible(compatible);
        return config;
    }

    private static final class CapturingCompatibleSpec extends OpenAICompatibleSpec {
        private int buildCount;
        private String apiKey;
        private String baseUrl;

        private CapturingCompatibleSpec(String configKey, String defaultBaseUrl) {
            super(configKey, "custom-model", defaultBaseUrl);
        }

        @Override
        protected Model buildModel(String apiKey, String baseUrl) {
            buildCount++;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            return null;
        }
    }
}
