package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.SkillsConfig;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.agentscope.core.skill.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 把技能 frontmatter 中声明的 {@code tools} 解析为可注册的工具实例。
 *
 * <p>{@code tools} 字段直接取自 agentscope 已经用 SnakeYAML 解析好的
 * {@link AgentSkill#getMetadataValue(String)}，因此无需再次读盘或自行解析 YAML，
 * 也天然支持 {@code tools: [a, b]} 这类行内数组写法。解析范围严格限定在传入的技能上，
 * 不会因为目录中其它（未被选中的）技能配置错误而牵连本次构建。
 */
final class SkillToolResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SkillToolResolver.class);

    static final String TOOLS_METADATA_KEY = "tools";

    private final SkillsConfig config;

    SkillToolResolver(SkillsConfig config) {
        this.config = config;
    }

    /**
     * 解析并实例化指定技能声明的工具。技能未声明 {@code tools} 时返回空列表。
     *
     * <p>工具类优先从框架容器（Spring/Solon）按类型取已注册的 bean，使其依赖注入生效；
     * 无容器、未注册或容器访问异常时，降级为反射实例化（依赖注入不可用）。
     */
    List<Object> instantiateTools(AgentSkill skill) {
        List<Class<?>> classes = resolveToolClasses(skill);
        if (classes.isEmpty()) {
            return List.of();
        }
        ContextAware contextAware = ContextAwareHolder.loadContextAware();
        List<Object> instances = new ArrayList<>(classes.size());
        for (Class<?> clazz : classes) {
            try {
                instances.add(resolveToolInstance(contextAware, skill, clazz));
            } catch (ReflectiveOperationException e) {
                handleProblem("Skill '" + skill.getName() + "' tool class '" + clazz.getName()
                        + "' instantiation failed", e);
            }
        }
        return List.copyOf(instances);
    }

    private Object resolveToolInstance(ContextAware contextAware, AgentSkill skill, Class<?> clazz)
            throws ReflectiveOperationException {
        try {
            if (contextAware.hasBean(clazz)) {
                return contextAware.getBean(clazz);
            }
        } catch (Exception ex) {
            // 容器未就绪（如 classpath 含 spring 但 ApplicationContext 尚未初始化）等异常：降级反射实例化
            LOG.warn("Skill '{}' resolving tool '{}' from container failed ({}); "
                    + "falling back to reflective instantiation",
                    skill.getName(), clazz.getName(), ex.toString());
        }
        Object instance = clazz.getDeclaredConstructor().newInstance();
        LOG.info("Skill '{}' tool '{}' not found in container; fell back to reflective "
                + "instantiation, dependency injection unavailable", skill.getName(), clazz.getName());
        return instance;
    }

    private List<Class<?>> resolveToolClasses(AgentSkill skill) {
        Object toolsObj = skill.getMetadataValue(TOOLS_METADATA_KEY);
        if (toolsObj == null) {
            return List.of();
        }
        List<Class<?>> resolved = new ArrayList<>();
        for (String className : toClassNameList(toolsObj)) {
            try {
                resolved.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                handleProblem("Skill '" + skill.getName() + "' references unknown tool class '"
                        + className + "'", e);
            }
        }
        if (!resolved.isEmpty()) {
            LOG.info("Skill '{}' bound to tool classes: {}", skill.getName(),
                    resolved.stream().map(Class::getName).toList());
        }
        return resolved;
    }

    private static List<String> toClassNameList(Object field) {
        if (field instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Stream.of(field.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void handleProblem(String message, Exception e) {
        if (config.isStrict()) {
            if (e == null) {
                throw new AgentConfigException(message);
            }
            throw new AgentConfigException(message, e);
        }
        if (e == null) {
            LOG.warn(message);
        } else {
            LOG.warn("{}: {}", message, e.getMessage());
        }
    }
}
