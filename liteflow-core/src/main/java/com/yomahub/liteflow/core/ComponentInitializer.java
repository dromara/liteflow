package com.yomahub.liteflow.core;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.holder.LiteflowComponentSupportHolder;

/**
 * 组件初始化器
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ComponentInitializer {

    private static ComponentInitializer instance;

    public static ComponentInitializer loadInstance(){
        if (ObjectUtil.isNull(instance)){
            instance = new ComponentInitializer();
        }
        return instance;
    }

    public NodeComponent initComponent(NodeComponent nodeComponent, NodeTypeEnum type, String desc, String nodeId){
        nodeComponent.setNodeId(nodeId);
        nodeComponent.setSelf(nodeComponent);
        nodeComponent.setType(type);

        //设置MonitorBus，如果没有就不注入
        MonitorBus monitorBus = ContextAwareHolder.loadContextAware().getBean(MonitorBus.class);
        if(ObjectUtil.isNotNull(monitorBus)){
            nodeComponent.setMonitorBus(monitorBus);
        }

        //先取传进来的name值(配置文件中配置的)，再看有没有配置@LiteflowComponent标注
        //@LiteflowComponent标注只在spring体系下生效，这里用了spi机制取到相应环境下的实现类
        nodeComponent.setName(desc);
        if (ListUtil.toList(NodeTypeEnum.COMMON, NodeTypeEnum.SWITCH).contains(type)
                && StrUtil.isBlank(nodeComponent.getName())){
            String name = LiteflowComponentSupportHolder.loadLiteflowComponentSupport().getCmpName(nodeComponent);
            nodeComponent.setName(name);
        }

        //先从组件上取@RetryCount标注，如果没有，则看全局配置，全局配置如果不配置的话，默认是0
        //默认retryForExceptions为Exception.class
        LiteflowRetry liteflowRetryAnnotation = AnnoUtil.getAnnotation(nodeComponent.getClass(), LiteflowRetry.class);
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        if (ObjectUtil.isNotNull(liteflowRetryAnnotation)) {
            nodeComponent.setRetryCount(liteflowRetryAnnotation.retry());
            nodeComponent.setRetryForExceptions(liteflowRetryAnnotation.forExceptions());
        } else {
            nodeComponent.setRetryCount(liteflowConfig.getRetryCount());
        }
        nodeComponent.setNodeExecutorClass(buildNodeExecutorClass(liteflowConfig));

        return nodeComponent;
    }

    private Class<? extends NodeExecutor> buildNodeExecutorClass(LiteflowConfig liteflowConfig) {
        Class<?> nodeExecutorClass;
        try {
            nodeExecutorClass = Class.forName(liteflowConfig.getNodeExecutorClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
        return (Class<? extends NodeExecutor>) nodeExecutorClass;
    }
}
