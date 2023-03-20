package com.yomahub.liteflow.script.annotation;

import com.yomahub.liteflow.annotation.AliasFor;

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

	@AliasFor("name")
	String value() default "";

	@AliasFor("value")
	String name() default "";

	String[] includeMethodName() default {};

	String[] excludeMethodName() default {};

}
