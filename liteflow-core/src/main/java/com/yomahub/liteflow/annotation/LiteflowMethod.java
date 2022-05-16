package com.yomahub.liteflow.annotation;

import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LiteflowMethod {

    LiteFlowMethodEnum value();
}
