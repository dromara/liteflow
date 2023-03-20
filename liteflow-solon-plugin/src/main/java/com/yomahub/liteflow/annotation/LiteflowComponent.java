package com.yomahub.liteflow.annotation;

import java.lang.annotation.*;

/**
 * LiteFlow的组件标识注解
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowComponent {

	@AliasFor("id")
	String value() default "";

	@AliasFor("value")
	String id() default "";

	String name() default "";

}
