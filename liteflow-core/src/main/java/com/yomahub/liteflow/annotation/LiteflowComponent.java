package com.yomahub.liteflow.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * LiteFlow的组件标识注解
 *
 * @author Bryan.Zhang
 * @since 2.5.11
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface LiteflowComponent {

    @AliasFor(annotation = Component.class)
    String value() default "";

    @AliasFor("value")
    String id();

    String name() default "";
}
