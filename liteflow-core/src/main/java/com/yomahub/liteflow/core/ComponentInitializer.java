package com.yomahub.liteflow.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.entity.executor.NodeExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import org.springframework.core.annotation.AnnotationUtils;

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

        //先取传进来的name值(配置文件中配置的)，再看有没有配置LiteflowComponent标注
        nodeComponent.setName(desc);
        if (nodeComponent.getType().equals(NodeTypeEnum.COMMON) && StrUtil.isBlank(nodeComponent.getName())){
            //判断NodeComponent是否是标识了@LiteflowComponent的标注
            //如果标注了，那么要从中取到name字段
            LiteflowComponent liteflowComponent = nodeComponent.getClass().getAnnotation(LiteflowComponent.class);
            if (ObjectUtil.isNotNull(liteflowComponent)) {
                String name = liteflowComponent.name();
                if (StrUtil.isNotBlank(name)) {
                    nodeComponent.setName(name);
                }
            }
        }

        //先从组件上取@RetryCount标注，如果没有，则看全局配置，全局配置如果不配置的话，默认是0
        //默认retryForExceptions为Exception.class
        LiteflowRetry liteflowRetryAnnotation = AnnotationUtils.getAnnotation(nodeComponent.getClass(), LiteflowRetry.class);
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
