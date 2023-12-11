package com.yomahub.liteflow.annotation;

import java.lang.annotation.*;

/**
 * @author Bryan.Zhang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface LFAliasFor {

	String value() default "";

}
