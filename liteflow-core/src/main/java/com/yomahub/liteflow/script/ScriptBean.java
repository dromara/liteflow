package com.yomahub.liteflow.script;

import java.lang.annotation.*;

/**
 * 用于标注在Script中可使用的java bean
 * @author Bryan.Zhang
 * @since 2.9.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ScriptBean {

    String value() default "";
}
