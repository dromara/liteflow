package com.yomahub.liteflow.core.proxy;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;

import java.lang.reflect.Method;
import java.util.List;

/**
 * LiteflowMethod的包装类
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class MethodWrapBean {

    private Method method;

    private LiteflowMethod liteflowMethod;

    private LiteflowRetry liteflowRetry;

    private List<ParameterWrapBean> parameterWrapBeanList;

    public MethodWrapBean(Method method, LiteflowMethod liteflowMethod, LiteflowRetry liteflowRetry, List<ParameterWrapBean> parameterWrapBeanList) {
        this.method = method;
        this.liteflowMethod = liteflowMethod;
        this.liteflowRetry = liteflowRetry;
        this.parameterWrapBeanList = parameterWrapBeanList;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public LiteflowMethod getLiteflowMethod() {
        return liteflowMethod;
    }

    public void setLiteflowMethod(LiteflowMethod liteflowMethod) {
        this.liteflowMethod = liteflowMethod;
    }

    public LiteflowRetry getLiteflowRetry() {
        return liteflowRetry;
    }

    public void setLiteflowRetry(LiteflowRetry liteflowRetry) {
        this.liteflowRetry = liteflowRetry;
    }

    public List<ParameterWrapBean> getParameterWrapBeanList() {
        return parameterWrapBeanList;
    }

    public void setParameterWrapBeanList(List<ParameterWrapBean> parameterWrapBeanList) {
        this.parameterWrapBeanList = parameterWrapBeanList;
    }
}
