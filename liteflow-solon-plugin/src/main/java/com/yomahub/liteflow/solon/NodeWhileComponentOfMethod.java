package com.yomahub.liteflow.solon;

import com.yomahub.liteflow.core.NodeWhileComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;

/**
 * @author noear
 * @since 1.11
 */
public class NodeWhileComponentOfMethod extends NodeWhileComponent {
    final BeanWrap beanWrap;
    final Method method;
    final LiteFlowMethodEnum methodEnum;

    public NodeWhileComponentOfMethod(BeanWrap beanWrap, Method method, LiteFlowMethodEnum methodEnum) {
        this.beanWrap = beanWrap;
        this.method = method;
        this.methodEnum = methodEnum;

        if (method.getParameterCount() > 1) {
            String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
            throw new RuntimeException("NodeWhileComponent method parameter cannot be more than one: " + methodFullName);
        }

        if (method.getReturnType() != Boolean.class) {
            String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
            throw new RuntimeException("NodeWhileComponent method returnType can only be boolean: " + methodFullName);
        }
    }

    private Object exec() throws Exception {
        if (method.getParameterCount() == 0) {
            return method.invoke(beanWrap.get());
        } else {
            return method.invoke(beanWrap.get(), this);
        }
    }

    @Override
    public boolean processWhile() throws Exception {
        return (boolean) exec();
    }
}
