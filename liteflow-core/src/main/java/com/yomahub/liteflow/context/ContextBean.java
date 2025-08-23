package com.yomahub.liteflow.context;

import cn.hutool.core.annotation.MirrorFor;

import java.lang.annotation.*;

/**
 * 用于标注上下文bean的别名，以便在脚本或者组件中通过别名来获取上下文对象
 *
 * @since 2.9.7
 * @author Tingliang Wang
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ContextBean {

	@MirrorFor(attribute = "name")
	String value() default "";

	@MirrorFor(attribute = "value")
	String name() default "";

}
