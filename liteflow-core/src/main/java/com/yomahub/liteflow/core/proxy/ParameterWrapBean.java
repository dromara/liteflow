package com.yomahub.liteflow.core.proxy;

import com.yomahub.liteflow.annotation.LiteflowFact;

/**
 * 声明式的包装类
 * @author Bryan.Zhang
 * @since 2.12.1
 */
public class ParameterWrapBean {

    private Class<?> parameterType;

    private LiteflowFact fact;

    private int index;

    public ParameterWrapBean(Class<?> parameterType, LiteflowFact fact, int index) {
        this.parameterType = parameterType;
        this.fact = fact;
        this.index = index;
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    public void setParameterType(Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    public LiteflowFact getFact() {
        return fact;
    }

    public void setFact(LiteflowFact fact) {
        this.fact = fact;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
