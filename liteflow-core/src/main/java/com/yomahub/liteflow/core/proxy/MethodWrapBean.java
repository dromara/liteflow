package com.yomahub.liteflow.core.proxy;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;

import java.lang.reflect.Method;

/**
 * LiteflowMethod的包装类
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class MethodWrapBean {

    private Method method;

    private LiteflowMethod liteflowMethod;

    private LiteflowRetry liteflowRetry;

    public MethodWrapBean(Method method, LiteflowMethod liteflowMethod, LiteflowRetry liteflowRetry) {
        this.method = method;
        this.liteflowMethod = liteflowMethod;
        this.liteflowRetry = liteflowRetry;
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
}
