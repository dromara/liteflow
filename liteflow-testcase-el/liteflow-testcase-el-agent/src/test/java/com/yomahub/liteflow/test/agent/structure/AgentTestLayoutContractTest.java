package com.yomahub.liteflow.test.agent.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentTestLayoutContractTest {

    private static final Set<String> ALLOWED_ROOTS = Set.of("liteflow-testcase-el", "liteflow-benchmark");

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
