package com.yomahub.liteflow.annotation;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowMethod {

    LiteFlowMethodEnum value();

    // 节点ID，用于区分节点
    // 默认为空 则按照Spring模式下BeanName为准。
    String nodeId() default "";

    /**
     * cmp定义
     *
     */
    Class<? extends NodeComponent> cmpClass() default NodeComponent.class;
}
