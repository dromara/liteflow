package com.yomahub.liteflow.agent.skill;

import io.agentscope.core.skill.SkillBox;

import java.util.List;
import java.util.Map;

public record SkillLoadResult(
        SkillBox skillBox,
        Map<String, String> skillIdToName,
        List<String> skillNames) {
}
