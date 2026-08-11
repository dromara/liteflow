package com.yomahub.liteflow.property.agent;

/**
 * Agent 技能配置绑定对象，对应配置段 {@code liteflow.agent.skills.*}。
 *
 * <p>{@code ReActAgentComponent} 不会自动读取此对象。应用可在覆写
 * {@code skillRepositories()} 时自行读取 {@code enabled} 与 {@code path}，
 * 创建 AgentScope 2 {@code AgentSkillRepository}；{@code strict} 当前仅为配置兼容保留。
 */
public class SkillsConfig {

    /**
     * 应用侧是否启用配置驱动的技能支持。
     *
     * <p>默认关闭；只有组件覆写主动读取该字段时才产生行为。
     */
    private boolean enabled = false;

    /**
     * 技能目录路径。
     *
     * <p>默认值为当前工作目录下的 {@code ./skills}。只有应用把该值传给
     * {@code FileSystemSkillRepository} 等 repository 时才会读取该目录。
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
