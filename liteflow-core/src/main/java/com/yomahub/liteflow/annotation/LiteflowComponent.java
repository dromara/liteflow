package com.yomahub.liteflow.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * LiteFlow的组件标识注解
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface LiteflowComponent {

    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = Component.class, attribute = "value")
    String id() default "";

    String name() default "";
}
