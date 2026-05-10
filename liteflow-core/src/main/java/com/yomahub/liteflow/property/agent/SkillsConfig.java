package com.yomahub.liteflow.property.agent;

/**
 * Agent 技能配置，对应配置段 {@code liteflow.agent.skills.*}。
 *
 * <p>用于控制 ReAct Agent 是否启用配置驱动的技能目录，以及技能配置解析时
 * 是否采用严格模式。
 */
public class SkillsConfig {

    /**
     * 是否启用配置驱动的技能支持。
     *
     * <p>默认关闭，保持现有 agent 行为不变。
     */
    private boolean enabled = false;

    /**
     * 技能目录路径。
     *
     * <p>默认读取当前工作目录下的 {@code ./skills}，后续技能加载逻辑会基于该路径
     * 查找技能配置文件。
     */
    private String path = "./skills";

    /**
     * 是否使用严格解析模式。
     *
     * <p>默认开启，后续技能解析遇到非法配置时可据此决定是否快速失败。
     */
    private boolean strict = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
