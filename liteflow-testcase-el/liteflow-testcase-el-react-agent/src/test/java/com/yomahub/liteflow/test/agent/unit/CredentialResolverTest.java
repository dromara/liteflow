package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 覆盖 guide §4.5 中两种凭据解析策略的错误提示。
 */
public class CredentialResolverTest {

    @Test
    public void testRequireFirstClassWithNullThrows() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> CredentialResolver.requireFirstClass(null, "liteflow.agent.openai"));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"));
    }

    @Test
    public void testRequireFirstClassWithBlankApiKeyThrows() {
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("");
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> CredentialResolver.requireFirstClass(cred, "liteflow.agent.anthropic"));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.anthropic.api-key"));
    }

    @Test
    public void testRequireFirstClassReturnsCredentialOnValidApiKey() {
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("real-key");
        PlatformCredential out = CredentialResolver.requireFirstClass(cred, "liteflow.agent.openai");
        Assertions.assertSame(cred, out);
    }

    @Test
    public void testRequireCompatibleWithMissingMapEntryThrows() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        Map.of(), "deepseek", "liteflow.agent.openai-compatible"));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.deepseek.api-key"));
    }

    @Test
    public void testRequireCompatibleWithBlankApiKeyThrows() {
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("   ");
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(
                        Map.of("kimi", cred), "kimi", "liteflow.agent.openai-compatible"));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.kimi.api-key"));
    }

    @Test
    public void testRequireCompatibleReturnsCredentialOnValidApiKey() {
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("real-key");
        cred.setBaseUrl("https://api.example.com/v1");
        PlatformCredential out = CredentialResolver.requireCompatible(
                Map.of("glm", cred), "glm", "liteflow.agent.openai-compatible");
        Assertions.assertSame(cred, out);
    }

    @Test
    public void testRequireCompatibleWithNullMapThrows() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> CredentialResolver.requireCompatible(null, "minimax", "liteflow.agent.openai-compatible"));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.minimax.api-key"));
    }
}
