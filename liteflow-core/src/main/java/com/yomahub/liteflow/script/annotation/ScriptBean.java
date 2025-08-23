package com.yomahub.liteflow.script.annotation;

import cn.hutool.core.annotation.MirrorFor;

import java.lang.annotation.*;

/**
 * 用于标注在Script中可使用的java bean
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ScriptBean {

	@MirrorFor(attribute = "name")
	String value() default "";

	@MirrorFor(attribute = "value")
	String name() default "";

	String[] includeMethodName() default {};

	String[] excludeMethodName() default {};

}
