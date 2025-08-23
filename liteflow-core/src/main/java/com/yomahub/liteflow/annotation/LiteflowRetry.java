package com.yomahub.liteflow.annotation;

import cn.hutool.core.annotation.MirrorFor;

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
/**
 * This class has been deprecated due to its only component retry function. Please use the retry method in the EL expression.
 * @Deprecated
 * @see # retry(int retryTimes)   e.g. THEN( a, b.retry(3) ); WHEN( a, b ).retry(3);
 */
public @interface LiteflowRetry {

	@MirrorFor(attribute = "retry")
	int value() default 0;

	@MirrorFor(attribute = "value")
	int retry() default 0;

	Class<? extends Exception>[] forExceptions() default { Exception.class };

}
