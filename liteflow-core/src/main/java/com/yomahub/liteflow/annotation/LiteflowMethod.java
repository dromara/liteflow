package com.yomahub.liteflow.annotation;

import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.lang.annotation.*;

/**
 * @author Bryan.Zhang
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowMethod {

	LiteFlowMethodEnum value();

	/**
	 * 节点ID，用于区分节点 默认为空 则按照Spring模式下BeanName为准。
	 * @return nodeId
	 */
	String nodeId() default "";

	/**
	 * 节点Name
	 * @return nodeName
	 */
	String nodeName() default "";

	/**
	 * CMP类型定义
	 * @return AnnotationNodeTypeEnum
	 */
	NodeTypeEnum nodeType() default NodeTypeEnum.COMMON;

}
