package com.yomahub.liteflow.agent.compatibility;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AgentScopeCompatibilityTest {
    @Test
    void acceptsThePinnedCoreArtifactOnTheRealClasspath() {
        assertDoesNotThrow(AgentScopeCompatibility::requireCoreVersion);
    }

    @Test
    void rejectsEvenAnUnverifiedPatchVersionWithAnActionableError() {
        var failure = assertThrows(AgentConfigException.class,
                () -> AgentScopeCompatibility.verify(loader("version=2.0.4\n"), "agentscope-core", "2.0.3"));
        assertTrue(failure.getMessage().contains("requires agentscope-core 2.0.3"));
        assertTrue(failure.getMessage().contains("found 2.0.4"));
        assertTrue(failure.getMessage().contains("dependency override"));
    }

    @Test
    void explainsMissingShadedMetadataAndRejectsMissingVersion() {
        var absent = assertThrows(AgentConfigException.class,
                () -> AgentScopeCompatibility.verify(loader(null), "agentscope-core", "2.0.3"));
        assertTrue(absent.getMessage().contains("preserve META-INF/maven/io.agentscope/agentscope-core/pom.properties"));
        assertThrows(AgentConfigException.class,
                () -> AgentScopeCompatibility.verify(loader("artifactId=agentscope-core\n"), "agentscope-core", "2.0.3"));
    }

    private static ClassLoader loader(String metadata) {
        return new ClassLoader(null) {
            @Override public InputStream getResourceAsStream(String name) {
                return metadata == null ? null : new ByteArrayInputStream(metadata.getBytes(StandardCharsets.ISO_8859_1));
            }
        };
    }
}
