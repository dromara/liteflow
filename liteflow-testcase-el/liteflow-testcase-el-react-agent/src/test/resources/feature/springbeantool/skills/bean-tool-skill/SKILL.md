---
name: bean-tool-skill
description: Skill that binds a Spring-managed Java tool for DI verification tests
tools: com.yomahub.liteflow.test.agent.feature.springbeantool.SpringBeanEchoTool
---

# Bean Tool Skill

This skill provides a tool that is also a Spring bean, verifying container-based resolution
by SkillToolResolver instead of reflective fallback instantiation.
