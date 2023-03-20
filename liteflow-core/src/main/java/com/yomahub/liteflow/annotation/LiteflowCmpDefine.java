package com.yomahub.liteflow.annotation;

import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.lang.annotation.*;

/**
 * @author Bryan.Zhang
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowCmpDefine {

	NodeTypeEnum value() default NodeTypeEnum.COMMON;

}
