package com.yomahub.liteflow.agent.harness.skill;

import com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystem;
import com.yomahub.liteflow.agent.harness.filesystem.LocalExecutionFilesystem;
import com.yomahub.liteflow.agent.harness.storage.StoredWorkspaceFilesystem;
import com.yomahub.liteflow.property.agent.ShellConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.remote.store.InMemoryStore;
import io.agentscope.harness.agent.skill.runtime.HarnessSkillEntry;
import io.agentscope.harness.agent.skill.runtime.SkillCatalog;
import io.agentscope.harness.agent.skill.runtime.SkillRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SessionSkillWorkspaceTest {
    @TempDir Path root;
    private final RuntimeContext first = RuntimeContext.builder().userId("user").sessionId("first").build();
    private final RuntimeContext second = RuntimeContext.builder().userId("user").sessionId("second").build();

    private AbstractFilesystem storage(boolean remote) {
        return remote ? new StoredWorkspaceFilesystem(new InMemoryStore(),
                rc -> List.of(rc.getUserId(), rc.getSessionId()), root)
                : new GuardedLocalFilesystem(root.resolve("records"));
    }

    private SkillCatalog catalog(Map<String, String> resources) {
        AgentSkill skill = new AgentSkill(Map.of("name", "report", "description", "report skill"),
                "Create a report.", resources, "classpath-skills", null);
        return SkillCatalog.of(List.of(new HarnessSkillEntry(skill, null, "/old/host/cache")));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void completeSkillIsReadableAndExecutableThroughTheSameSessionPath(boolean remote) throws Exception {
        AbstractFilesystem records = storage(remote);
        LocalExecutionFilesystem files = new LocalExecutionFilesystem(records, root, new ShellConfig(), "app");
        var middleware = new SessionSkillWorkspaceMiddleware(() -> files);
        SkillCatalog original = catalog(Map.of("scripts/report.sh", "printf report-ok", "assets/raw.bin", "base64:AP8="));
        first.put(SkillCatalog.class, original);
        String prompt = new SkillRuntime().renderPrompt(original, SkillFilter.all());
        String rewritten = middleware.onSystemPrompt(null, first, prompt).block();
        String path = ".skills-cache/classpath-skills/report";
        assertNotNull(rewritten);
        assertTrue(rewritten.contains(path));
        assertFalse(rewritten.contains("/old/host/cache"));
        assertEquals(path, new SkillRuntime().currentCatalog(first).all().iterator().next().filesRoot());
        assertTrue(files.read(first, path + "/SKILL.md", 0, 0).fileData().content().contains("Create a report."));
        assertArrayEquals(new byte[]{0, (byte)255}, files.downloadFiles(first, List.of(path + "/assets/raw.bin")).get(0).content());
        assertTrue(Files.isRegularFile(files.executionDirectory(first).resolve(path + "/SKILL.md")));
        assertFalse(files.exists(second, path + "/SKILL.md"));
        var output = files.execute(first, "sh " + path + "/scripts/report.sh", null);
        assertEquals(0, output.exitCode(), output.output());
        assertEquals("report-ok", output.output());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void revokedSkillsDisappearAfterRuntimeRecreationWithoutDeletingAnotherSession(boolean remote) {
        AbstractFilesystem files = storage(remote);
        for (RuntimeContext context : List.of(first, second)) {
            context.put(SkillCatalog.class, catalog(Map.of("scripts/report.sh", "printf ok")));
            new SessionSkillWorkspaceMiddleware(() -> files).onSystemPrompt(null, context, "").block();
        }
        files.write(first, "business.txt", "keep");
        first.put(SkillCatalog.class, SkillCatalog.empty());
        new SessionSkillWorkspaceMiddleware(() -> files).onSystemPrompt(null, first, "").block();
        assertFalse(files.exists(first, ".skills-cache/classpath-skills/report/SKILL.md"));
        assertTrue(files.exists(second, ".skills-cache/classpath-skills/report/SKILL.md"));
        assertEquals("keep", files.read(first, "business.txt", 0, 0).fileData().content());
    }

    @Test void invalidResourcePathsFailBeforeAnyWorkspaceMutation() {
        AbstractFilesystem files = storage(true);
        var middleware = new SessionSkillWorkspaceMiddleware(() -> files);
        first.put(SkillCatalog.class, catalog(Map.of("../escape", "bad")));
        assertThrows(IllegalArgumentException.class, () -> middleware.onSystemPrompt(null, first, "").block());
        assertFalse(files.exists(first, ".skills-cache/classpath-skills/report/SKILL.md"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void skillResourcesLargerThanTheFormerLimitAreMaterialized(boolean remote) {
        AbstractFilesystem files = storage(remote);
        var middleware = new SessionSkillWorkspaceMiddleware(() -> files);
        String content = "x".repeat(11 * 1024 * 1024);
        first.put(SkillCatalog.class, catalog(Map.of("large.txt", content)));
        middleware.onSystemPrompt(null, first, "").block();
        assertEquals(content, new String(files.downloadFiles(first,
                List.of(".skills-cache/classpath-skills/report/large.txt")).get(0).content(),
                java.nio.charset.StandardCharsets.UTF_8));
    }
}
