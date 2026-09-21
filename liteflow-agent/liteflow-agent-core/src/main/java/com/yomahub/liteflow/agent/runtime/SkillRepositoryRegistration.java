package com.yomahub.liteflow.agent.runtime;

import io.agentscope.core.skill.repository.AgentSkillRepository;

import java.util.Objects;

/** A component-provided skill repository together with its LiteFlow runtime ownership. */
public record SkillRepositoryRegistration(
        AgentSkillRepository repository, boolean owned) {

    public SkillRepositoryRegistration {
        Objects.requireNonNull(repository, "repository");
    }

    public static SkillRepositoryRegistration owned(AgentSkillRepository repository) {
        return new SkillRepositoryRegistration(repository, true);
    }

    public static SkillRepositoryRegistration borrowed(AgentSkillRepository repository) {
        return new SkillRepositoryRegistration(repository, false);
    }
}
