package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.WorkspaceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceFileToolsTest {

    @TempDir Path tmp;

    private WorkspaceFileTools newTool(Path ws, long maxBytes, int maxList) {
        AgentConfig cfg = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setMaxFileBytes(maxBytes);
        w.setMaxListSize(maxList);
        cfg.setWorkspace(w);
        return new WorkspaceFileTools(ws, cfg);
    }

    @Test
    void write_then_read_round_trip() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        t.writeFile("a.txt", "hello");
        assertEquals("hello", t.readFile("a.txt"));
    }

    @Test
    void path_traversal_rejected() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertThrows(SecurityException.class, () -> t.readFile("../escape"));
        assertThrows(SecurityException.class, () -> t.writeFile("../../evil", "x"));
        assertThrows(SecurityException.class, () -> t.deleteFile("../../evil"));
    }

    @Test
    void absolute_path_rejected() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertThrows(SecurityException.class, () -> t.readFile("/etc/passwd"));
    }

    @Test
    void read_truncates_oversize_file() throws IOException {
        WorkspaceFileTools t = newTool(tmp, 4, 10);
        Files.writeString(tmp.resolve("big.txt"), "1234567890");
        String out = t.readFile("big.txt");
        assertTrue(out.length() <= 4, "exceeds maxFileBytes, must truncate");
    }

    @Test
    void list_limits_entries() throws IOException {
        WorkspaceFileTools t = newTool(tmp, 1024, 3);
        for (int i = 0; i < 5; i++) Files.writeString(tmp.resolve("f" + i + ".txt"), "x");
        java.util.List<String> list = t.listFiles(".");
        assertTrue(list.size() <= 3, "exceeds maxListSize, must truncate");
    }

    @Test
    void delete_nonexistent_is_ok() {
        WorkspaceFileTools t = newTool(tmp, 1024, 10);
        assertDoesNotThrow(() -> t.deleteFile("nope.txt"));
    }
}
