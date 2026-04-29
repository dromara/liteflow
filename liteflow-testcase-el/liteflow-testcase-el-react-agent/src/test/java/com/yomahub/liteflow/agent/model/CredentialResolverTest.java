package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialResolverTest {

    @Test
    void requireFirstClassReturnsCredentialWhenApiKeySet() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenai().setApiKey("sk-xxx");

        PlatformCredential c =
                CredentialResolver.requireFirstClass(cfg.getOpenai(), "liteflow.agent.openai");
        assertEquals("sk-xxx", c.getApiKey());
    }

    @Test
    void requireFirstClassThrowsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireFirstClass(
                        cfg.getOpenai(), "liteflow.agent.openai"));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"),
                "message should mention the config path");
    }

    @Test
    void requireCompatibleReturnsCredentialFromMap() {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("ds-key");
        cfg.getOpenaiCompatible().put("deepseek", cred);

        PlatformCredential c = CredentialResolver.requireCompatible(
                cfg.getOpenaiCompatible(), "deepseek", "liteflow.agent.openai-compatible");
        assertEquals("ds-key", c.getApiKey());
    }

    @Test
    void requireCompatibleThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        cfg.getOpenaiCompatible(), "deepseek",
                        "liteflow.agent.openai-compatible"));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.deepseek"));
    }

    @Test
    void requireCompatibleThrowsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenaiCompatible().put("deepseek", new PlatformCredential());

        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        cfg.getOpenaiCompatible(), "deepseek",
                        "liteflow.agent.openai-compatible"));
        assertTrue(ex.getMessage().contains("api-key"));
    }
}
