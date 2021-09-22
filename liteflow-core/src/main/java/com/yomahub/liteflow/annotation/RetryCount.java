package com.yomahub.liteflow.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

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
public @interface RetryCount {

    @AliasFor(value = "retry")
    int value() default 0;

    @AliasFor(value = "value")
    int retry() default 0;

    Class<? extends Exception>[] forExceptions() default {Exception.class};
}
