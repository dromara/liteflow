package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.SkillsConfig;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillBoxFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SkillBoxFactory.class);

    private SkillBoxFactory() {
    }

    public static SkillLoadResult build(Toolkit toolkit, AgentConfig agentConfig, List<String> allowedSkills) {
        SkillsConfig skillsConfig = agentConfig.getSkills();
        Path root = Path.of(skillsConfig.getPath()).normalize();
        if (!Files.isDirectory(root)) {
            return handleMissingRoot(root, skillsConfig, toolkit);
        }

        try {
            FileSystemSkillRepository repository = new FileSystemSkillRepository(root);
            List<AgentSkill> allSkills = repository.getAllSkills();
            List<AgentSkill> selected = selectSkills(allSkills, allowedSkills, skillsConfig);
            SkillToolManifest manifest = new SkillToolManifest(root, skillsConfig);
            SkillBox skillBox = new SkillBox(toolkit);
            Map<String, String> skillIdToName = new LinkedHashMap<>();
            List<String> skillNames = new ArrayList<>();

            for (AgentSkill skill : selected) {
                skillIdToName.put(skill.getSkillId(), skill.getName());
                skillNames.add(skill.getName());
                List<Object> skillTools = manifest.instantiateTools(skill.getName());
                if (skillTools.isEmpty()) {
                    skillBox.registerSkill(skill);
                } else {
                    for (Object tool : skillTools) {
                        skillBox.registration().skill(skill).tool(tool).apply();
                    }
                }
            }
            return new SkillLoadResult(
                    skillBox,
                    Collections.unmodifiableMap(new LinkedHashMap<>(skillIdToName)),
                    List.copyOf(skillNames));
        } catch (AgentConfigException e) {
            throw e;
        } catch (Exception e) {
            if (skillsConfig.isStrict()) {
                throw new AgentConfigException("Failed to load skills from: " + root, e);
            }
            LOG.warn("Failed to load skills from {}: {}", root, e.getMessage());
            return new SkillLoadResult(new SkillBox(toolkit), Map.of(), List.of());
        }
    }

    private static SkillLoadResult handleMissingRoot(Path root, SkillsConfig skillsConfig, Toolkit toolkit) {
        String message = "Skills root not found: " + root;
        if (skillsConfig.isStrict()) {
            throw new AgentConfigException(message);
        }
        LOG.warn(message);
        return new SkillLoadResult(new SkillBox(toolkit), Map.of(), List.of());
    }

    private static List<AgentSkill> selectSkills(
            List<AgentSkill> allSkills,
            List<String> allowedSkills,
            SkillsConfig skillsConfig) {
        Map<String, AgentSkill> byName = allSkills.stream()
                .collect(Collectors.toMap(
                        AgentSkill::getName,
                        skill -> skill,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Set<String> allowed = normalizeAllowedSkills(allowedSkills);
        if (allowed.isEmpty()) {
            return byName.values().stream()
                    .sorted(Comparator.comparing(AgentSkill::getName))
                    .toList();
        }

        List<String> missing = allowed.stream()
                .filter(name -> !byName.containsKey(name))
                .toList();
        if (!missing.isEmpty()) {
            String message = "Declared skills not found: " + missing;
            if (skillsConfig.isStrict()) {
                throw new AgentConfigException(message);
            }
            LOG.warn(message);
        }

        List<AgentSkill> selected = new ArrayList<>();
        for (String name : allowed) {
            AgentSkill skill = byName.get(name);
            if (skill != null) {
                selected.add(skill);
            }
        }
        return selected;
    }

    private static Set<String> normalizeAllowedSkills(List<String> allowedSkills) {
        if (allowedSkills == null || allowedSkills.isEmpty()) {
            return Set.of();
        }
        return allowedSkills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
