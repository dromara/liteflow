package com.yomahub.liteflow.context;

import com.yomahub.liteflow.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @description 用于标注上下文bean的别名，以便在脚本或者组件中通过别名来获取上下文对象
 * @since 2.9.7
 * @author Tingliang Wang
 * @createTime 2023/2/6 15:06
 * @update: [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
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
