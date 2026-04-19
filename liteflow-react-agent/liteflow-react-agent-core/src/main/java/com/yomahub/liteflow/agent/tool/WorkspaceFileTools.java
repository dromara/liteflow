package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceFileTools {

    private final Path workspace;
    private final long maxBytes;
    private final int maxList;

    public WorkspaceFileTools(Path workspace, AgentConfig cfg) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.maxBytes = cfg.getWorkspace().getMaxFileBytes();
        this.maxList = cfg.getWorkspace().getMaxListSize();
    }

    @Tool(name = "read_file", description = "Read a text file in the current workspace")
    public String readFile(
            @ToolParam(name = "path", description = "Relative path") String path) {
        Path p = resolveSafe(path);
        try {
            long size = Files.size(p);
            if (size > maxBytes) {
                byte[] buf = new byte[(int) maxBytes];
                try (var in = Files.newInputStream(p)) {
                    int read = in.read(buf);
                    return new String(buf, 0, Math.max(0, read), StandardCharsets.UTF_8);
                }
            }
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("read_file failed: " + e.getMessage(), e);
        }
    }

    @Tool(name = "write_file", description = "Write text to a file in the current workspace (overwrite)")
    public String writeFile(
            @ToolParam(name = "path", description = "Relative path") String path,
            @ToolParam(name = "content", description = "File content") String content) {
        Path p = resolveSafe(path);
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, content, StandardCharsets.UTF_8);
            return "ok";
        } catch (IOException e) {
            throw new RuntimeException("write_file failed: " + e.getMessage(), e);
        }
    }

    @Tool(name = "list_files", description = "List files in a workspace directory")
    public List<String> listFiles(
            @ToolParam(name = "path", required = false, description = "Relative path; defaults to current dir") String path) {
        Path dir = resolveSafe(path == null || path.isEmpty() ? "." : path);
        List<String> out = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                out.add(workspace.relativize(p).toString());
                if (out.size() >= maxList) break;
            }
        } catch (IOException e) {
            throw new RuntimeException("list_files failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Tool(name = "delete_file", description = "Delete a file in the current workspace")
    public String deleteFile(
            @ToolParam(name = "path", description = "Relative path") String path) {
        Path p = resolveSafe(path);
        try {
            Files.deleteIfExists(p);
            return "ok";
        } catch (IOException e) {
            throw new RuntimeException("delete_file failed: " + e.getMessage(), e);
        }
    }

    private Path resolveSafe(String rel) {
        if (rel == null) throw new SecurityException("path is null");
        if (rel.startsWith("/")) throw new SecurityException("absolute path denied: " + rel);
        Path abs = workspace.resolve(rel).toAbsolutePath().normalize();
        if (!abs.startsWith(workspace)) {
            throw new SecurityException("path escapes workspace: " + rel);
        }
        return abs;
    }
}
