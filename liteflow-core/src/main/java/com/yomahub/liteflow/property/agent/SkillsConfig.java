package com.yomahub.liteflow.property.agent;

/**
 * Agent 技能配置绑定对象，对应配置段 {@code liteflow.agent.skills.*}。
 *
 * <p>启用后，ReAct 类组件会根据 {@code path} 自动创建并托管 AgentScope 2
 * {@code AgentSkillRepository}。{@code classpath:} 前缀表示 classpath 资源目录，
 * 其他值表示文件系统目录；{@code strict} 当前仅为配置兼容保留。
 */
public class SkillsConfig {

    /**
     * 是否启用配置驱动的技能支持。
     *
     * <p>默认关闭；启用后由 LiteFlow 创建并管理对应的技能仓库。
     */
    private boolean enabled = false;

    /**
     * 技能目录路径。
     *
     * <p>默认值为当前工作目录下的 {@code ./skills}。使用 {@code classpath:agent/skills}
     * 这类值可从 classpath 资源目录加载技能。
     */
    private String path = "./skills";

    /**
     * 旧的严格解析配置占位。
     *
     * <p>当前 {@code ReActAgentComponent}、AgentScope 2 repository 与
     * {@code SkillFilter} 均不读取该字段；设置为 {@code false} 不会改变错误处理策略。
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
