package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Trusted-local file tools routed by the current invocation's runtime session. */
public class WorkspaceFileTools {

    private final GuardedWorkspacePathResolver resolver;
    private final int maxList;

    public WorkspaceFileTools(GuardedWorkspacePathResolver resolver, AgentConfig config) {
        this.resolver = java.util.Objects.requireNonNull(resolver, "resolver");
        this.maxList = config.getWorkspace().getMaxListSize();
    }

    public WorkspaceFileTools(Path root, AgentConfig config) {
        this(new GuardedWorkspacePathResolver(
                root,
                config.getWorkspace().getMaxFileBytes(),
                config.getWorkspace().isAutoCreate()), config);
    }

    @Tool(name = "read_file", description = "Read a text file in the current workspace",
            readOnly = true)
    public String readFile(
            RuntimeContext runtimeContext,
            @ToolParam(name = "path", description = "Relative path") String path) {
        Path target = resolve(runtimeContext, path);
        try {
            return readLimited(target, resolver.maxFileBytes());
        } catch (IOException failure) {
            throw new RuntimeException("read_file failed: " + failure.getMessage(), failure);
        }
    }

    @Tool(name = "write_file",
            description = "Write text to a file in the current workspace (overwrite)",
            concurrencySafe = false)
    public String writeFile(
            RuntimeContext runtimeContext,
            @ToolParam(name = "path", description = "Relative path") String path,
            @ToolParam(name = "content", description = "File content") String content) {
        resolver.validateWriteContent(content);
        LiteFlowAgentContext context = requireContext(runtimeContext);
        Path target = resolver.resolve(context.getRuntimeSessionId(), path);
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            target = resolver.resolve(context.getRuntimeSessionId(), path);
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return "ok";
        } catch (IOException failure) {
            throw new RuntimeException("write_file failed: " + failure.getMessage(), failure);
        }
    }

    @Tool(name = "list_files", description = "List files in a workspace directory",
            readOnly = true)
    public List<String> listFiles(
            RuntimeContext runtimeContext,
            @ToolParam(name = "path", required = false,
                    description = "Relative path; defaults to current dir") String path) {
        LiteFlowAgentContext context = requireContext(runtimeContext);
        Path session = resolver.sessionRoot(context.getRuntimeSessionId());
        Path directory = resolver.resolve(
                context.getRuntimeSessionId(), path == null || path.isEmpty() ? "." : path);
        List<String> result = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                result.add(session.relativize(entry).toString());
                if (result.size() >= maxList) {
                    break;
                }
            }
        } catch (IOException failure) {
            throw new RuntimeException("list_files failed: " + failure.getMessage(), failure);
        }
        return result;
    }

    @Tool(name = "delete_file", description = "Delete a file in the current workspace",
            concurrencySafe = false)
    public String deleteFile(
            RuntimeContext runtimeContext,
            @ToolParam(name = "path", description = "Relative path") String path) {
        Path target = resolve(runtimeContext, path);
        try {
            Files.deleteIfExists(target);
            return "ok";
        } catch (IOException failure) {
            throw new RuntimeException("delete_file failed: " + failure.getMessage(), failure);
        }
    }

    private Path resolve(RuntimeContext runtimeContext, String path) {
        LiteFlowAgentContext context = requireContext(runtimeContext);
        return resolver.resolve(context.getRuntimeSessionId(), path);
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for workspace tools");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException(
                    "LiteFlowAgentContext is required for workspace tools");
        }
        return context;
    }

    private static String readLimited(Path path, long maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long remaining = maxBytes;
        try (InputStream input = Files.newInputStream(path)) {
            while (remaining > 0) {
                int request = (int) Math.min(buffer.length, remaining);
                int read = input.read(buffer, 0, request);
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
