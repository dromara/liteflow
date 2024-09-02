package com.yomahub.liteflow.solon;

import com.yomahub.liteflow.core.NodeIteratorComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.exception.LiteFlowException;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * @author noear
 */
public class NodeIteratorComponentOfMethod extends NodeIteratorComponent {
    private final BeanWrap beanWrap;

    private final Method method;

    private final LiteFlowMethodEnum methodEnum;

    public NodeIteratorComponentOfMethod(BeanWrap beanWrap, Method method, LiteFlowMethodEnum methodEnum) {
        this.beanWrap = beanWrap;
        this.method = method;
        this.methodEnum = methodEnum;

        if (method.getParameterCount() > 1) {
            String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
            throw new LiteFlowException("NodeIfComponent method parameter cannot be more than one: " + methodFullName);
        }

        if (method.getReturnType() != Iterator.class) {
            String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
            throw new LiteFlowException("NodeIfComponent method returnType can only be iterator: " + methodFullName);
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
    public Iterator<?> processIterator() throws Exception {
        return (Iterator<?>) exec();
    }
}
