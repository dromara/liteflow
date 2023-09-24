package com.yomahub.liteflow.annotation;

import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 降级组件
 * @author DaleLee
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FallbackCmp {
    
    /**
     * 节点类型
     * @return NodeTypeEnum
     */
    NodeTypeEnum type() default NodeTypeEnum.COMMON;
}
