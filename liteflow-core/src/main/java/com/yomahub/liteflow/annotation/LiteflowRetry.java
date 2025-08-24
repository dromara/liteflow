package com.yomahub.liteflow.annotation;

import cn.hutool.core.annotation.MirrorFor;

import java.lang.annotation.*;

/**
 * <p>LiteFlow组件重试次数</p>
 * <p>这个注解已经被废弃，请参考EL中的retry关键字</p>
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Deprecated
public @interface LiteflowRetry {

	@MirrorFor(attribute = "retry")
	int value() default 0;

	@MirrorFor(attribute = "value")
	int retry() default 0;

	Class<? extends Exception>[] forExceptions() default { Exception.class };

}
