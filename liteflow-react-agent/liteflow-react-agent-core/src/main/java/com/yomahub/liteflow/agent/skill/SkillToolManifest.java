package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.SkillsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SkillToolManifest {

    private static final Logger LOG = LoggerFactory.getLogger(SkillToolManifest.class);

    private final SkillsConfig config;
    private final Map<String, List<Class<?>>> toolClasses = new LinkedHashMap<>();

    public SkillToolManifest(Path skillsRoot, SkillsConfig config) {
        this.config = config;
        scan(skillsRoot);
    }

    public List<Object> instantiateTools(String skillName) {
        List<Class<?>> classes = toolClasses.get(skillName);
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        List<Object> instances = new ArrayList<>(classes.size());
        for (Class<?> clazz : classes) {
            try {
                instances.add(clazz.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                handleProblem("Skill '" + skillName + "' tool class '" + clazz.getName()
                        + "' instantiation failed", e);
            }
        }
        return List.copyOf(instances);
    }

    private void scan(Path skillsRoot) {
        if (!Files.isDirectory(skillsRoot)) {
            handleProblem("Skills root not found: " + skillsRoot, null);
            return;
        }
        try (Stream<Path> dirs = Files.list(skillsRoot)) {
            dirs.filter(Files::isDirectory)
                    .sorted()
                    .forEach(this::loadOne);
        } catch (IOException e) {
            handleProblem("Failed to scan skills dir: " + skillsRoot, e);
        }
    }

    private void loadOne(Path skillDir) {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            handleProblem("Skill file not found: " + skillMd, null);
            return;
        }
        try {
            Map<String, Object> frontmatter = parseFrontmatter(Files.readString(skillMd));
            Object nameObj = frontmatter.get("name");
            if (nameObj == null) {
                return;
            }
            Object toolsObj = frontmatter.get("tools");
            if (toolsObj == null) {
                return;
            }
            String skillName = nameObj.toString().trim();
            List<Class<?>> resolved = new ArrayList<>();
            for (String className : toClassNameList(toolsObj)) {
                try {
                    resolved.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    handleProblem("Skill '" + skillName + "' references unknown tool class '"
                            + className + "'", e);
                }
            }
            if (!resolved.isEmpty()) {
                toolClasses.put(skillName, List.copyOf(resolved));
                LOG.info("Skill '{}' bound to tool classes: {}", skillName,
                        resolved.stream().map(Class::getName).toList());
            }
        } catch (IOException e) {
            handleProblem("Failed to read skill file: " + skillMd, e);
        }
    }

    static Map<String, Object> parseFrontmatter(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (content == null || !content.startsWith("---")) {
            return result;
        }
        int end = content.indexOf("\n---", 3);
        if (end < 0) {
            return result;
        }
        String[] lines = content.substring(3, end).split("\\R");
        String currentListKey = null;
        List<String> currentList = null;
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (currentListKey != null && trimmed.startsWith("-")) {
                currentList.add(unquote(trimmed.substring(1).trim()));
                continue;
            }
            currentListKey = null;
            currentList = null;
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (value.isEmpty()) {
                currentListKey = key;
                currentList = new ArrayList<>();
                result.put(key, currentList);
            } else {
                result.put(key, unquote(value));
            }
        }
        return result;
    }

    private List<String> toClassNameList(Object field) {
        if (field instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Stream.of(field.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private void handleProblem(String message, Exception e) {
        if (config.isStrict()) {
            if (e == null) {
                throw new AgentConfigException(message);
            }
            throw new AgentConfigException(message, e);
        }
        if (e == null) {
            LOG.warn(message);
        } else {
            LOG.warn("{}: {}", message, e.getMessage());
        }
    }
}
