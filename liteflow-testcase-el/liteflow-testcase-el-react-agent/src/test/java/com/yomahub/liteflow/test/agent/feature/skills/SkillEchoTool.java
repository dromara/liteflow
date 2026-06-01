package com.yomahub.liteflow.test.agent.feature.skills;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * skill-skill 的 SKILL.md frontmatter 中声明的 Java 工具类。
 * 按模块隔离约定冗余在本包内。
 */
public class SkillEchoTool {

    public static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

    public SkillEchoTool() {
        CONSTRUCT_COUNT.incrementAndGet();
    }

    public static void reset() {
        CONSTRUCT_COUNT.set(0);
    }

    @Tool(name = "skill_echo", description = "Echo text from a skill-bound Java tool")
    public String echo(@ToolParam(name = "text", description = "Text to echo") String text) {
        return text;
    }
}
