package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 直接测试 {@link WorkspaceFileTools}，不经过 ReActAgent 或 LLM。
 */
public class WorkspaceFileToolsUnitTest {

    @TempDir
    Path workspace;

    private AgentConfig cfg;

    @BeforeEach
    public void setUp() {
        cfg = new AgentConfig();
        cfg.getWorkspace().setRoot(workspace.toString());
        cfg.getWorkspace().setMaxFileBytes(10);
        cfg.getWorkspace().setMaxListSize(2);
    }

    @Test
    public void testReadWriteAndDeleteRoundTrip() throws IOException {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        Assertions.assertEquals("ok", tools.writeFile("a.txt", "hello"));
        Assertions.assertEquals("hello", tools.readFile("a.txt"));

        Assertions.assertEquals("ok", tools.deleteFile("a.txt"));
        Assertions.assertFalse(Files.exists(workspace.resolve("a.txt")));
    }

    @Test
    public void testReadTruncatesAtMaxFileBytes() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        tools.writeFile("long.txt", "0123456789ABCDEF");
        Assertions.assertEquals("0123456789", tools.readFile("long.txt"));
    }

    @Test
    public void testListFilesCapsAtMaxListSize() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        tools.writeFile("a.txt", "1");
        tools.writeFile("b.txt", "2");
        tools.writeFile("c.txt", "3");
        List<String> listed = tools.listFiles(".");
        Assertions.assertEquals(2, listed.size(), "list_files 应按 max-list-size 限制条目数");
    }

    @Test
    public void testRelativeEscapeIsDenied() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        SecurityException ex = Assertions.assertThrows(SecurityException.class,
                () -> tools.readFile("../escape.txt"));
        Assertions.assertTrue(ex.getMessage().contains("path escapes workspace"));
    }

    @Test
    public void testAbsoluteEscapeIsDenied() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        SecurityException ex = Assertions.assertThrows(SecurityException.class,
                () -> tools.readFile("/tmp/escape.txt"));
        Assertions.assertTrue(ex.getMessage().contains("absolute path denied"));
    }

    @Test
    public void testWriteCreatesParentDirectories() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        tools.writeFile("deep/nested/dir/note.txt", "yo");
        Assertions.assertTrue(Files.exists(workspace.resolve("deep/nested/dir/note.txt")));
    }

    @Test
    public void testNullPathIsDenied() {
        WorkspaceFileTools tools = new WorkspaceFileTools(workspace, cfg);
        Assertions.assertThrows(SecurityException.class, () -> tools.readFile(null));
    }
}
