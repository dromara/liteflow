package com.yomahub.liteflow.agent.harness.skill;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.util.MarkdownSkillParser;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.middleware.HarnessRuntimeMiddleware;
import io.agentscope.harness.agent.skill.runtime.HarnessSkillEntry;
import io.agentscope.harness.agent.skill.runtime.SkillCatalog;
import io.agentscope.harness.agent.skill.runtime.SkillRuntime;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/** Gives every execution backend the same session-relative, complete skill files. */
public final class SessionSkillWorkspaceMiddleware implements HarnessRuntimeMiddleware {
    private static final String ROOT = ".skills-cache/";
    private final Supplier<AbstractFilesystem> filesystem;
    private final boolean shellEnabled;
    private final SkillRuntime renderer = new SkillRuntime();

    public SessionSkillWorkspaceMiddleware(Supplier<AbstractFilesystem> filesystem) {
        this(filesystem, true);
    }

    public SessionSkillWorkspaceMiddleware(Supplier<AbstractFilesystem> filesystem, boolean shellEnabled) {
        this.shellEnabled = shellEnabled;
        this.filesystem = Objects.requireNonNull(filesystem);
    }

    @Override public int order() { return Integer.MIN_VALUE; }

    @Override public Mono<String> onSystemPrompt(Agent agent, RuntimeContext context, String prompt) {
        return Mono.fromCallable(() -> {
            SkillCatalog original = renderer.currentCatalog(context);
            AbstractFilesystem files = filesystem.get();
            Map<String, byte[]> uploads = new LinkedHashMap<>();
            List<HarnessSkillEntry> entries = new ArrayList<>();
            for (HarnessSkillEntry entry : original.all()) {
                String name = segment(entry.skill().getName());
                String source = segment(entry.skill().getSource());
                String root = ROOT + source + "/" + name;
                put(uploads, root + "/SKILL.md", MarkdownSkillParser.generate(
                        entry.skill().getMetadata(), entry.skill().getSkillContent()).getBytes(StandardCharsets.UTF_8));
                for (var resource : entry.skill().getResources().entrySet()) {
                    String text = resource.getValue();
                    byte[] bytes = text.startsWith("base64:")
                            ? Base64.getDecoder().decode(text.substring(7)) : text.getBytes(StandardCharsets.UTF_8);
                    put(uploads, root + "/" + relative(resource.getKey()), bytes);
                }
                if (entry.lazyResources() != null) {
                    for (String path : entry.lazyResources().list()) {
                        if (path.equals("SKILL.md")) continue;
                        String target = root + "/" + relative(path);
                        if (!uploads.containsKey(target)) {
                            put(uploads, target, entry.lazyResources().readBinary(path).orElseThrow(
                                    () -> new IllegalStateException("Cannot read skill resource: " + path)));
                        }
                    }
                }
                entries.add(new HarnessSkillEntry(entry.skill(), entry.lazyResources(), shellEnabled ? root : null));
            }
            // This reserved subtree is owned by the skill runtime, including older snapshot layouts.
            // Sweep only the current session, so filtering cannot leave executable stale resources.
            List<String> previousFiles = new ArrayList<>();
            List<String> previousDirectories = new ArrayList<>();
            collectCache(files, context, ROOT.substring(0, ROOT.length() - 1), previousFiles, previousDirectories);
            for (String old : previousFiles) {
                if (!uploads.containsKey(old) && !files.delete(context, old).isSuccess())
                    throw new IllegalStateException("Cannot remove stale skill resource: " + old);
            }
            previousDirectories.sort(Comparator.comparingInt(String::length).reversed());
            for (String directory : previousDirectories) {
                if (uploads.keySet().stream().noneMatch(path -> path.startsWith(directory + "/"))
                        && files.exists(context, directory) && !files.delete(context, directory).isSuccess())
                    throw new IllegalStateException("Cannot remove stale skill directory: " + directory);
            }
            if (!uploads.isEmpty()) {
                var result = files.uploadFiles(context, uploads.entrySet().stream().toList());
                if (result.size() != uploads.size() || result.stream().anyMatch(r -> !r.isSuccess()))
                    throw new IllegalStateException("Cannot materialize the session skill workspace");
            }
            SkillCatalog catalog = SkillCatalog.of(entries);
            context.put(SkillCatalog.class, catalog);
            String before = renderer.renderPrompt(original, SkillFilter.all());
            String after = renderer.renderPrompt(catalog, SkillFilter.all());
            return before == null || before.isEmpty() ? prompt : prompt.replace(before, after);
        });
    }

    private static void collectCache(AbstractFilesystem files, RuntimeContext context, String directory,
                                     List<String> paths, List<String> directories) {
        if (!files.exists(context, directory)) return;
        var listing = files.ls(context, directory);
        if (!listing.isSuccess()) throw new IllegalStateException("Cannot inspect skill cache: " + directory);
        for (var entry : listing.entries()) {
            String raw = entry.path().replace('\\', '/').replaceAll("/+$", "");
            String path = directory + "/" + segment(raw.substring(raw.lastIndexOf('/') + 1));
            if (entry.isDirectory()) {
                collectCache(files, context, path, paths, directories);
                directories.add(path);
            } else {
                paths.add(path);
            }
        }
    }

    private void put(Map<String, byte[]> uploads, String path, byte[] bytes) {
        uploads.put(path, bytes);
    }

    private static String segment(String raw) {
        if (raw == null || raw.isBlank()) return "_global";
        String value = relative(raw);
        if (value.contains("/")) throw new IllegalArgumentException("Invalid skill name or source: " + raw);
        return value;
    }

    private static String relative(String raw) {
        String value = raw.replace('\\', '/');
        if (value.isBlank() || value.startsWith("/") || value.matches("^[A-Za-z]:.*")
                || Arrays.asList(value.split("/")).contains(".."))
            throw new IllegalArgumentException("Invalid skill resource path: " + raw);
        String normalized = Path.of(value).normalize().toString().replace('\\', '/');
        if (normalized.isBlank() || normalized.equals(".")) throw new IllegalArgumentException("Invalid skill resource path");
        return normalized;
    }
}
