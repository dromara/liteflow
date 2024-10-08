package com.yomahub.liteflow.process.impl;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;
import com.yomahub.liteflow.process.holder.SpringNodeIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 普通组件查找
 *
 * @author tkc
 * @since 2.12.4
 */
public class NodeCmpBeanProcess implements LiteflowScannerProcessStep {
    private static final Logger LOG = LoggerFactory.getLogger(CmpAroundAspectBeanProcess.class);

    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();

        return NodeComponent.class.isAssignableFrom(clazz);
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();
        String beanName = ctx.getBeanName();
        Object bean = ctx.getBean();

        LOG.info("component[{}] has been found", beanName);
        NodeComponent nodeComponent = (NodeComponent) bean;
        String realBeanName = SpringNodeIdHolder.getRealBeanName(clazz, beanName);
        SpringNodeIdHolder.add(realBeanName);
        return nodeComponent;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.NODE_CMP_BEAN;
    }
}
