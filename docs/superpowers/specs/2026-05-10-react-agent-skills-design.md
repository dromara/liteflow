# liteflow-react-agent Skills Support Design

## Goal

Add first-class skills support to `liteflow-react-agent` by reusing the agent-scope `SkillBox` model and following the skill design pattern from `/Users/bryan31/AiProject/beast-react-agent-service`.

The feature lets LiteFlow ReAct agents load skills from a configured filesystem repository, optionally restrict each agent component to a declared allow-list of skills, and bind skill-specific Java tools through `SKILL.md` frontmatter.

## Non-Goals

- Do not add request-time `dependentSkills` support.
- Do not make the selected skills vary per invocation request.
- Do not enable agent-scope `SkillBox.codeExecution()` by default.
- Do not replace the existing `tools()`, `WorkspaceFileTools`, or `ManagedShellCommandTool` extension points.
- Do not change existing behavior unless `liteflow.agent.skills.enabled=true`.

## Current Context

`liteflow-react-agent` currently builds a `Toolkit` inside `ReActAgentComponent#buildAgent()` and registers:

1. component-level custom tools from `tools()`;
2. built-in workspace file tools when `enableWorkspaceFileTools()` is true;
3. built-in shell tool when `enableShellTool()` is true and shell mode is not disabled.

The built `ReActAgent` currently receives `.toolkit(toolkit)` but no `.skillBox(...)`.

`beast-react-agent-service` uses this pattern:

1. configure a skills path;
2. create `FileSystemSkillRepository` from that path;
3. load `AgentSkill` instances;
4. register them into `SkillBox`;
5. scan `SKILL.md` YAML frontmatter for a custom `tools` field;
6. when a skill has bound tools, call `skillBox.registration().skill(skill).tool(tool).apply()`;
7. pass the `SkillBox` to `ReActAgent.builder().skillBox(skillBox)`.

This design adapts that pattern to LiteFlow's configuration and component inheritance style.

## Configuration Model

Add `SkillsConfig` under `liteflow-core/src/main/java/com/yomahub/liteflow/property/agent/` and add it to `AgentConfig`.

Proposed properties:

```properties
liteflow.agent.skills.enabled=false
liteflow.agent.skills.path=./skills
liteflow.agent.skills.strict=true
```

### Property Semantics

- `enabled`：global skills switch. Default `false` to keep current users unaffected when no skills directory exists.
- `path`：filesystem root that contains skill directories, each with `SKILL.md`, for example `skills/contract-review/SKILL.md`.
- `strict`：when true, if a component declares a skill name that does not exist in the repository, agent construction fails with `AgentConfigException`. Default `true`.

## Component API

Add these extension points to `ReActAgentComponent`:

```java
protected List<String> skills() { return List.of(); }

protected boolean enableSkills() {
    return agentConfig().getSkills().isEnabled();
}
```

### `skills()` Rules

- Empty list means the component may use all skills in the configured repository.
- Non-empty list means the component may only use the named skills.
- Names are matched against `AgentSkill#getName()`.
- If a declared skill is not found and strict mode is enabled, throw `AgentConfigException`.
- The method is a component capability declaration and should not depend on request data.

No `dependentSkills()` API will be added.

## New Skill Package

Create package:

```text
liteflow-react-agent/liteflow-react-agent-core/src/main/java/com/yomahub/liteflow/agent/skill/
```

### `SkillToolManifest`

Responsibilities:

- scan every direct child directory under `liteflow.agent.skills.path`;
- read `<skill-dir>/SKILL.md`;
- parse YAML frontmatter;
- extract `name` and optional `tools`;
- build `skillName -> List<Class<?>>` mapping;
- instantiate tool objects for a skill when requested.

Expected frontmatter format:

```yaml
---
name: contract-review
description: Review contracts and identify legal or commercial risks
tools:
  - com.example.agent.tool.ContractClauseSearchTool
  - com.example.agent.tool.ContractRiskScoringTool
---
```

`tools` may be a YAML list or a comma-separated string. Unknown tool classes should throw `AgentConfigException` in strict mode and log a warning / skip in non-strict mode.

### `SkillBoxFactory`

Responsibilities:

- create `FileSystemSkillRepository` from `SkillsConfig.path`;
- load all skills with `repository.getAllSkills()`;
- apply the component `skills()` allow-list;
- validate missing declared skills;
- build `skillId -> skillName` mapping for tracking;
- register each selected skill into `SkillBox`;
- bind skill-specific Java tools through `SkillToolManifest`.

Registration rules:

```java
if (skillTools.isEmpty()) {
    skillBox.registerSkill(skill);
} else {
    for (Object tool : skillTools) {
        skillBox.registration().skill(skill).tool(tool).apply();
    }
}
```

### `SkillLoadResult`

A small value object returned by `SkillBoxFactory`, containing:

- `SkillBox skillBox`;
- `Map<String, String> skillIdToName`;
- `List<String> skillNames`.

This avoids re-scanning inside hooks and keeps `ReActAgentComponent` simple.

## ReActAgentComponent Integration

Update `ReActAgentComponent#buildAgent()`:

1. create `Toolkit` exactly as today;
2. register existing custom and built-in tools exactly as today;
3. if `enableSkills()` is true:
   - build a `SkillLoadResult` with `SkillBoxFactory`;
   - add `SkillTrackingHook` using the `skillIdToName` mapping;
   - pass `.skillBox(result.skillBox())` to `ReActAgent.builder()`;
4. if skills are disabled, do not pass a `SkillBox` or pass no-op state so behavior stays the same.

The existing session cache key stays `(conversationId, agentKey)` because `skills()` is a stable component capability declaration.

## Skill Tracking

Add `SkillTrackingHook` to `com.yomahub.liteflow.agent.skill`.

Behavior:

- listen for `PostActingEvent`;
- if tool name is `load_skill_through_path`, read `skillId` from tool input;
- map `skillId` to skill name using `skillIdToName`;
- record a de-duplicated ordered set of used skill names.

Expose used skills through `ReActAgentComponent` with:

```java
protected List<String> usedSkills()
```

The default `handleReply()` should continue to write only reply text to response data. Users who need skills metadata can override `handleReply()` and read `usedSkills()`.

## Tool and Code Execution Policy

Do not enable `skillBox.codeExecution()` in this feature.

Reasoning:

- `liteflow-react-agent` already owns workspace file access through `WorkspaceFileTools`.
- It already owns shell execution through `ManagedShellCommandTool`, including command mode, timeout, and output truncation.
- Enabling agent-scope `ShellCommandTool` would introduce a second shell permission model.

Skill-specific Java tools declared in `SKILL.md` are supported because they are explicit, classpath-bound, and follow the beast project pattern.

A future feature can add:

```properties
liteflow.agent.skills.code-execution.enabled=true
```

with a separate security and workspace design.

## Error Handling

- `enabled=true` and missing skills directory:
  - strict mode true：throw `AgentConfigException`.
  - strict mode false：log warning and build agent without skills.
- component declares missing skill:
  - strict mode true：throw `AgentConfigException`.
  - strict mode false：log warning and skip it.
- skill frontmatter has invalid tool class:
  - strict mode true：throw `AgentConfigException`.
  - strict mode false：log warning and skip that tool.
- skill repository read failure:
  - strict mode true：throw `AgentConfigException`.
  - strict mode false：log warning and build without failed skills.

## Tests

Add tests in `liteflow-testcase-el/liteflow-testcase-el-react-agent`.

### Test Fixtures

Add test skills under:

```text
liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/resources/agent/skills/
```

Example:

```text
agent/skills/demo/SKILL.md
agent/skills/research/SKILL.md
agent/skills/tool-skill/SKILL.md
```

Add a test tool class under:

```text
liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/com/yomahub/liteflow/test/agent/tool/SkillEchoTool.java
```

### Test Cases

1. **Skills disabled keeps existing behavior**
   - `liteflow.agent.skills.enabled=false`.
   - Existing tool registration assertions continue to pass.

2. **Empty `skills()` loads all repository skills**
   - Component returns `List.of()`.
   - Model probe sees the skill loading tool schema from `SkillBox`.
   - SkillBox contains all test skill ids.

3. **Non-empty `skills()` filters repository skills**
   - Component returns `List.of("demo")`.
   - SkillBox contains only `demo`.
   - Other repository skills are not active / not registered.

4. **Missing declared skill fails in strict mode**
   - Component returns `List.of("missing-skill")`.
   - Execution response fails with an `AgentConfigException` cause or message.

5. **SKILL.md frontmatter tools are bound**
   - `tool-skill/SKILL.md` declares `SkillEchoTool`.
   - Registered tool schemas include the custom skill tool.

6. **Used skill tracking is available to component**
   - Unit-level hook test records a synthetic `load_skill_through_path` action, or integration test uses a stub model that triggers the skill loading path if practical.
   - Assert `usedSkills()` returns skill names, not opaque ids.

## Documentation Notes

Document these points in the module guide or JavaDoc:

- Enable with `liteflow.agent.skills.enabled=true`.
- Put skills under `liteflow.agent.skills.path`.
- Use `ReActAgentComponent#skills()` to declare an allow-list.
- Empty `skills()` means all configured skills.
- `skills()` should be stable per component and should not depend on request data because agent sessions are cached by `(conversationId, agentKey)`.
- Skill-specific Java tools can be declared with `tools` in `SKILL.md` frontmatter.
- Skill code execution is intentionally not enabled in this version.

## Acceptance Criteria

- Existing `liteflow-react-agent` tests pass with skills disabled.
- With skills enabled, `ReActAgent` is built with a populated `SkillBox`.
- Component-level `skills()` allow-list limits available skills.
- Missing declared skills fail fast by default.
- Skill-specific tools from `SKILL.md` frontmatter are registered.
- No `dependentSkills()` API exists.
- No second shell/code-execution model is introduced.
