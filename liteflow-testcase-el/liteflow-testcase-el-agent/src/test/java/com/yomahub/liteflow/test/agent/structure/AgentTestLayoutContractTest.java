package com.yomahub.liteflow.test.agent.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgentTestLayoutContractTest {

    private static final Set<String> ALLOWED_ROOTS = Set.of("liteflow-testcase-el", "liteflow-benchmark");

    @Test
    void agentTestsNeverLoadExternalCredentialsOrSkipForMissingServices() throws Exception {
        Path testcase = repositoryRoot().resolve("liteflow-testcase-el");
        assertNull(getClass().getClassLoader().getResource("env.txt"),
                "Local credentials must never be copied to the test classpath; run clean test after migration");
        List<String> violations = new ArrayList<>();
        Pattern forbidden = Pattern.compile("System\\s*\\.\\s*getenv\\s*\\(|env\\.txt|"
                + "@(?:EnabledIf|Disabled)\\w*|\\b(?:assumeTrue|assumeFalse|assumingThat)\\s*\\(|"
                + "LITEFLOW_(?:AGENT_TEST|TEST_SHARED_STORAGE|TEST_DOCKER)|sk-[A-Za-z0-9]{20,}");
        try (var modules = Files.list(testcase)) {
            for (Path module : modules.filter(p -> p.getFileName().toString()
                    .startsWith("liteflow-testcase-el-agent")).toList()) {
                String pom = Files.readString(module.resolve("pom.xml"));
                if (pom.contains("agent-live") || pom.contains("agent-docker-it")
                        || !pom.contains("<skipTests>false</skipTests>") || excludesTests(pom)) {
                    violations.add(module.getFileName() + "/pom.xml: external-service profile or hidden tests");
                }
                try (var files = Files.walk(module.resolve("src/test"))) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        String name = file.getFileName().toString();
                        if (name.equals("AgentTestLayoutContractTest.java")) continue;
                        if (name.equals("env.txt") || name.startsWith(".env")) {
                            if (!module.getFileName().toString().equals("liteflow-testcase-el-agent")) {
                                violations.add(testcase.relativize(file).toString());
                            }
                            continue;
                        }
                        if (name.endsWith("LiveTest.java")) {
                            violations.add(testcase.relativize(file).toString());
                        } else if (name.endsWith(".java") || name.endsWith(".properties")
                                || name.endsWith(".yml") || name.endsWith(".yaml")) {
                            if (forbidden.matcher(Files.readString(file)).find()) {
                                violations.add(testcase.relativize(file).toString());
                            }
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), violations,
                () -> "Agent tests must use mock boundaries and run without credentials or external services: " + violations);
    }

    private static boolean excludesTests(String pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(pom)));
        var plugins = document.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            if ("maven-surefire-plugin".equals(plugin.getElementsByTagName("artifactId").item(0).getTextContent())
                    && (plugin.getElementsByTagName("excludes").getLength() > 0
                    || plugin.getElementsByTagName("excludedGroups").getLength() > 0
                    || plugin.getElementsByTagName("includes").getLength() > 0)) return true;
        }
        return false;
    }

    @Test
    void allTestsLiveUnderTestcaseElExceptBenchmarks() throws IOException {
        Path root = repositoryRoot();
        List<String> violations = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                if (directory.equals(root)) return FileVisitResult.CONTINUE;
                String name = directory.getFileName().toString();
                if (name.startsWith(".") || name.equals("target") || name.equals("node_modules")
                        || (directory.getParent().equals(root) && ALLOWED_ROOTS.contains(name))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (relative.contains("/src/test/") || relative.startsWith("src/test/")) {
                    violations.add(relative);
                } else if (relative.endsWith(".java") || relative.endsWith(".kt") || relative.endsWith(".groovy")) {
                    String source = Files.readString(file);
                    if (source.matches("(?s).*\\bimport\\s+(?:static\\s+)?(?:org\\.junit\\.|org\\.testng\\.|junit\\.framework\\.).*")) {
                        violations.add(relative);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        violations.sort(Comparator.naturalOrder());
        assertEquals(List.of(), violations,
                () -> "Tests must live under liteflow-testcase-el (benchmark excepted): " + violations);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("liteflow-agent"))
                    && Files.isDirectory(current.resolve("liteflow-testcase-el"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate LiteFlow repository root");
    }
}
