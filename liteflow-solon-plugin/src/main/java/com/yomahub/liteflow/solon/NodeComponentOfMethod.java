package com.yomahub.liteflow.solon;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.Slot;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;

/**
 * @author noear
 * @since 1.11
 */
public class NodeComponentOfMethod extends NodeComponent {
    final BeanWrap beanWrap;
    final Method method;
    final LiteFlowMethodEnum methodEnum;

    public NodeComponentOfMethod(BeanWrap beanWrap, Method method, LiteFlowMethodEnum methodEnum) {
        this.beanWrap = beanWrap;
        this.method = method;
        this.methodEnum = methodEnum;
    }

    @Override
    public void process() throws Exception {
        if(methodEnum != LiteFlowMethodEnum.PROCESS){
            return;
        }

        if (method.getParameterCount() == 0) {
            method.invoke(beanWrap.get());
        } else if (method.getParameterCount() == 1) {
            method.invoke(beanWrap.get(), this);
        } else {
            String methodFullName = beanWrap.clz().getName() + "::" + method.getName();
            throw new RuntimeException("NodeComponent method parameter cannot be more than one: " + methodFullName);
        }
    }


    @Override
    public <T> void beforeProcess(String nodeId, Slot slot) {
        if(methodEnum != LiteFlowMethodEnum.BEFORE_PROCESS){
            return;
        }
    }

    @Override
    public <T> void afterProcess(String nodeId, Slot slot) {
        if(methodEnum != LiteFlowMethodEnum.AFTER_PROCESS){
            return;
        }
    }


    @Override
    public void onError() throws Exception {
        if(methodEnum != LiteFlowMethodEnum.ON_ERROR){
            return;
        }
    }

    @Override
    public void onSuccess() throws Exception {
        if(methodEnum != LiteFlowMethodEnum.ON_SUCCESS){
            return;
        }
    }
}
