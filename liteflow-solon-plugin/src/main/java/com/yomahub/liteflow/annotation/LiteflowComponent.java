package com.yomahub.liteflow.annotation;

import cn.hutool.core.annotation.MirrorFor;

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

	@MirrorFor(attribute = "id")
	String value() default "";

	@MirrorFor(attribute = "value")
	String id() default "";

	String name() default "";

}
