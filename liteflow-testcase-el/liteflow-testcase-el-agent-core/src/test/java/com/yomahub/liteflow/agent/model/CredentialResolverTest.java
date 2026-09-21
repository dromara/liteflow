package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialResolverTest {

    private static final String FIRST_CLASS_PATH = "liteflow.agent.openai";
    private static final String COMPATIBLE_PATH = "liteflow.agent.openai-compatible";

    @Test
    void explicitFirstClassCredentialOverridesConfiguredValuesWithoutMutation() {
        PlatformCredential configured = credential("configured-key", "https://configured.example");
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("tenant", "configured");
        configured.setExtra(extra);

        CredentialResolver.ResolvedCredential resolved = CredentialResolver.resolveFirstClass(
                configured,
                FIRST_CLASS_PATH,
                "explicit-key",
                "https://explicit.example");

        assertEquals("explicit-key", resolved.apiKey());
        assertEquals("https://explicit.example", resolved.baseUrl());
        assertEquals("configured-key", configured.getApiKey());
        assertEquals("https://configured.example", configured.getBaseUrl());
        assertSame(extra, configured.getExtra());
        assertEquals(Map.of("tenant", "configured"), configured.getExtra());
    }

    @Test
    void firstClassCredentialFallsBackToConfiguredValuesAndAllowsNoBaseUrl() {
        CredentialResolver.ResolvedCredential configured = CredentialResolver.resolveFirstClass(
                credential("configured-key", "https://configured.example"),
                FIRST_CLASS_PATH,
                null,
                "   ");
        CredentialResolver.ResolvedCredential noBaseUrl = CredentialResolver.resolveFirstClass(
                credential("configured-key", "  "),
                FIRST_CLASS_PATH,
                null,
                null);

        assertEquals("configured-key", configured.apiKey());
        assertEquals("https://configured.example", configured.baseUrl());
        assertEquals("configured-key", noBaseUrl.apiKey());
        assertNull(noBaseUrl.baseUrl());
    }

    @Test
    void explicitApiKeyCanAuthenticateWithoutFirstClassConfiguration() {
        CredentialResolver.ResolvedCredential resolved = CredentialResolver.resolveFirstClass(
                null,
                FIRST_CLASS_PATH,
                "explicit-key",
                null);

        assertEquals("explicit-key", resolved.apiKey());
        assertNull(resolved.baseUrl());
    }

    @Test
    void blankExplicitApiKeyDoesNotFallBackToConfiguredSecret() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveFirstClass(
                        credential("configured-key", "https://configured.example"),
                        FIRST_CLASS_PATH,
                        "   ",
                        null));

        assertEquals("Missing API key: please configure liteflow.agent.openai.api-key",
                failure.getMessage());
    }

    @Test
    void missingOrBlankConfiguredFirstClassApiKeyReportsTheExactPath() {
        AgentConfigException missing = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveFirstClass(
                        null, FIRST_CLASS_PATH, null, null));
        AgentConfigException blank = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveFirstClass(
                        credential("", "https://configured.example"),
                        FIRST_CLASS_PATH,
                        null,
                        null));

        assertEquals("Missing API key: please configure liteflow.agent.openai.api-key",
                missing.getMessage());
        assertEquals("Missing API key: please configure liteflow.agent.openai.api-key",
                blank.getMessage());
    }

    @Test
    void compatibleCredentialUsesTheSamePrecedenceWithoutMutatingTheMapOrEntry() {
        PlatformCredential entry = credential("configured-key", "https://configured.example");
        Map<String, PlatformCredential> configured = new LinkedHashMap<>();
        configured.put("deepseek", entry);

        CredentialResolver.ResolvedCredential resolved = CredentialResolver.resolveCompatible(
                configured,
                "deepseek",
                COMPATIBLE_PATH,
                "explicit-key",
                "https://explicit.example");

        assertEquals("explicit-key", resolved.apiKey());
        assertEquals("https://explicit.example", resolved.baseUrl());
        assertSame(entry, configured.get("deepseek"));
        assertEquals("configured-key", entry.getApiKey());
        assertEquals("https://configured.example", entry.getBaseUrl());
        assertEquals(1, configured.size());
    }

    @Test
    void compatibleCredentialFallsBackToConfiguredValues() {
        CredentialResolver.ResolvedCredential resolved = CredentialResolver.resolveCompatible(
                Map.of("deepseek", credential(
                        "configured-key", "https://configured.example")),
                "deepseek",
                COMPATIBLE_PATH,
                null,
                null);

        assertEquals("configured-key", resolved.apiKey());
        assertEquals("https://configured.example", resolved.baseUrl());
    }

    @Test
    void explicitApiKeyCanAuthenticateWithoutCompatibleEntry() {
        CredentialResolver.ResolvedCredential resolved = CredentialResolver.resolveCompatible(
                Map.of(),
                "deepseek",
                COMPATIBLE_PATH,
                "explicit-key",
                "https://explicit.example");

        assertEquals("explicit-key", resolved.apiKey());
        assertEquals("https://explicit.example", resolved.baseUrl());
    }

    @Test
    void missingCompatibleEntryIsDistinctFromMissingCompatibleApiKey() {
        AgentConfigException missingEntry = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveCompatible(
                        null,
                        "deepseek",
                        COMPATIBLE_PATH,
                        null,
                        null));
        AgentConfigException missingKey = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveCompatible(
                        Map.of("deepseek", credential(" ", null)),
                        "deepseek",
                        COMPATIBLE_PATH,
                        null,
                        null));

        assertEquals("Missing platform credential: please configure "
                        + "liteflow.agent.openai-compatible.deepseek.api-key",
                missingEntry.getMessage());
        assertEquals("Missing API key: please configure "
                        + "liteflow.agent.openai-compatible.deepseek.api-key",
                missingKey.getMessage());
    }

    @Test
    void explicitBaseUrlAloneCannotAuthenticateAMissingCompatibleEntry() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveCompatible(
                        Map.of(),
                        "deepseek",
                        COMPATIBLE_PATH,
                        null,
                        "https://explicit.example"));

        assertEquals("Missing platform credential: please configure "
                        + "liteflow.agent.openai-compatible.deepseek.api-key",
                failure.getMessage());
    }

    @Test
    void blankExplicitCompatibleApiKeyDoesNotFallBackToConfiguredSecret() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> CredentialResolver.resolveCompatible(
                        Map.of("deepseek", credential("configured-key", null)),
                        "deepseek",
                        COMPATIBLE_PATH,
                        "",
                        null));

        assertEquals("Missing API key: please configure "
                        + "liteflow.agent.openai-compatible.deepseek.api-key",
                failure.getMessage());
    }

    private static PlatformCredential credential(String apiKey, String baseUrl) {
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        return credential;
    }
}
