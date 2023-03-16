package com.yomahub.liteflow.context;

import com.yomahub.liteflow.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 用于标注上下文bean的别名，以便在脚本或者组件中通过别名来获取上下文对象
 * @since 2.9.7
 * @author Tingliang Wang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextBean {
    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

}
