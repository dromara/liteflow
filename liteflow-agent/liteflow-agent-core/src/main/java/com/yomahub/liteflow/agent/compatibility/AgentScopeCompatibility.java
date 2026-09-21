package com.yomahub.liteflow.agent.compatibility;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.ReActAgent;

import java.io.IOException;
import java.util.Properties;

/** Internal compatibility check for the AgentScope version tested by this LiteFlow release. */
public final class AgentScopeCompatibility {
    public static final String CORE_VERSION = "2.0.3";
    private static volatile boolean coreVerified;

    private AgentScopeCompatibility() {
    }

    public static void requireCoreVersion() {
        if (coreVerified) {
            return;
        }
        synchronized (AgentScopeCompatibility.class) {
            if (!coreVerified) {
                verify(ReActAgent.class.getClassLoader(), "agentscope-core", CORE_VERSION);
                coreVerified = true;
            }
        }
    }

    static void verify(ClassLoader loader, String artifactId, String requiredVersion) {
        String resource = "META-INF/maven/io.agentscope/" + artifactId + "/pom.properties";
        Properties metadata = new Properties();
        try (var input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new AgentConfigException("Cannot verify " + artifactId + ": preserve " + resource
                        + " when packaging or shading the application");
            }
            metadata.load(input);
        } catch (IOException failure) {
            throw new AgentConfigException("Cannot read version metadata for " + artifactId, failure);
        }
        String actualVersion = metadata.getProperty("version");
        if (!requiredVersion.equals(actualVersion)) {
            throw new AgentConfigException("This LiteFlow Agent release requires " + artifactId + " "
                    + requiredVersion + ", but found " + actualVersion
                    + "; remove the dependency override and align all AgentScope artifacts with LiteFlow's BOM");
        }
    }
}
