package com.yomahub.liteflow.script.annotation;

import com.yomahub.liteflow.annotation.LFAliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注在Script中可使用的java 方法
 *
 * @author tangkc
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ScriptMethod {

	@LFAliasFor("name")
	String value() default "";

}
