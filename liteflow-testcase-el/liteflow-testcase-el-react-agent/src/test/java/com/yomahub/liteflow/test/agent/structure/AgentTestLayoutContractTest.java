package com.yomahub.liteflow.test.agent.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentTestLayoutContractTest {

    private static final List<String> PROHIBITED_ROOTS = List.of(
            "liteflow-react-agent/liteflow-react-agent-core/src/test",
            "liteflow-react-agent/liteflow-react-agent-openai/src/test",
            "liteflow-react-agent/liteflow-react-agent-anthropic/src/test",
            "liteflow-react-agent/liteflow-react-agent-gemini/src/test",
            "liteflow-react-agent/liteflow-react-agent-dashscope/src/test",
            "liteflow-react-agent/liteflow-react-agent-harness/src/test",
            "liteflow-react-agent/liteflow-react-agent-a2a/src/test");

    private static final List<String> PROHIBITED_FILES = List.of(
            "liteflow-core/src/test/java/com/yomahub/liteflow/property/agent/AgentConfigV2Test.java",
            "liteflow-spring-boot-starter/src/test/java/com/yomahub/liteflow/springboot/AgentPropertyBindingTest.java",
            "liteflow-spring-boot4-starter/src/test/java/com/yomahub/liteflow/springboot4/AgentPropertyBindingTest.java",
            "liteflow-solon-plugin/src/test/java/com/yomahub/liteflow/spi/solon/SolonCmpAroundAspectTest.java");

    @Test
    void agentScope2TestsLiveOnlyUnderLiteflowTestcaseEl() throws IOException {
        Path root = repositoryRoot();
        List<String> violations = new ArrayList<>();
        for (String relativeRoot : PROHIBITED_ROOTS) {
            Path prohibited = root.resolve(relativeRoot);
            if (!Files.exists(prohibited)) {
                continue;
            }
            try (var paths = Files.walk(prohibited)) {
                paths.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .map(Path::toString)
                        .forEach(violations::add);
            }
        }
        for (String relativeFile : PROHIBITED_FILES) {
            if (Files.isRegularFile(root.resolve(relativeFile))) {
                violations.add(relativeFile);
            }
        }
        violations.sort(Comparator.naturalOrder());
        assertEquals(List.of(), violations,
                () -> "AgentScope 2 tests must live under liteflow-testcase-el: " + violations);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("liteflow-react-agent"))
                    && Files.isDirectory(current.resolve("liteflow-testcase-el"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate LiteFlow repository root");
    }
}
