package com.yomahub.liteflow.annotation;

import java.lang.annotation.*;

/**
 * LiteFlow组件重试次数
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowRetry {

    @AliasFor("retry")
    int value() default 0;

    @AliasFor("value")
    int retry() default 0;

    Class<? extends Exception>[] forExceptions() default {Exception.class};
}
