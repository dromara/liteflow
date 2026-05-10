/**
 * LiteFlow ReAct Agent 核心模块。
 *
 * <p>Skills support is configuration-driven. Set
 * {@code liteflow.agent.skills.enabled=true} and point
 * {@code liteflow.agent.skills.path} at a filesystem skills repository. A
 * component may override {@code skills()} to restrict which skill names are
 * available to that agent. Skill-specific Java tools can be declared in
 * {@code SKILL.md} frontmatter with a {@code tools} field.
 */
package com.yomahub.liteflow.agent;
