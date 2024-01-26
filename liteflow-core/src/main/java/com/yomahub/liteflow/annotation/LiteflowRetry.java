package com.yomahub.liteflow.annotation;

import java.lang.annotation.*;

/**
 * LiteFlow组件重试次数
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Deprecated
public @interface LiteflowRetry {

	@LFAliasFor("retry")
	int value() default 0;

	@LFAliasFor("value")
	int retry() default 0;

	Class<? extends Exception>[] forExceptions() default { Exception.class };

}
