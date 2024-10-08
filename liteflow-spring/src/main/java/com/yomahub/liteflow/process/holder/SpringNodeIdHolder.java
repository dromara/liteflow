package com.yomahub.liteflow.process.holder;

import cn.hutool.core.annotation.AnnotationUtil;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * 全局 nodeId 持有器
 *
 * @author tkc
 * @since 2.12.4
 */
public class SpringNodeIdHolder {
    /**
     * @RefreshScope 注解 bean 的前缀
     */
    private static final String REFRESH_SCOPE_ANN_BEAN_PREFIX = "scopedTarget.";

    /**
     * @RefreshScope 注解完整类路径
     */
    private static final String REFRESH_SCOPE_ANN_CLASS_PATH = "org.springframework.cloud.context.config.annotation.RefreshScope";

    private static final Set<String> NODE_COMPONENT_SET = new HashSet<>();

    public static void add(String nodeId) {
        NODE_COMPONENT_SET.add(nodeId);
    }

    public static Set<String> getNodeIdSet() {
        return NODE_COMPONENT_SET;
    }

    /**
     * 获取真实的 beanName 1. @RefreshScope 注解标注的bean 名称前会多加一个 scopedTarget.
     *
     * @param clazz    clazz
     * @param beanName beanName
     */
    public static String getRealBeanName(Class<?> clazz, String beanName) {
        if (beanName.startsWith(REFRESH_SCOPE_ANN_BEAN_PREFIX)) {
            Annotation[] annotations = AnnotationUtil.getAnnotations(clazz, true);
            for (Annotation annotation : annotations) {
                String name = annotation.annotationType().getName();
                if (REFRESH_SCOPE_ANN_CLASS_PATH.equals(name)) {
                    return beanName.replace(REFRESH_SCOPE_ANN_BEAN_PREFIX, "");
                }
            }
        }
        return beanName;
    }
}
