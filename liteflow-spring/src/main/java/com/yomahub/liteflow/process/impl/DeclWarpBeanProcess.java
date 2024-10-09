package com.yomahub.liteflow.process.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;
import com.yomahub.liteflow.process.holder.SpringNodeIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 声明式组件查找
 *
 * @author tkc
 * @since 2.12.4
 */
public class DeclWarpBeanProcess implements LiteflowScannerProcessStep {
    private static final Logger LOG = LoggerFactory.getLogger(DeclWarpBeanProcess.class);

    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Object bean = ctx.getBean();
        return bean instanceof DeclWarpBean;
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();
        String beanName = ctx.getBeanName();
        Object bean = ctx.getBean();
        NodeComponent nodeComponent = LiteFlowProxyUtil.proxy2NodeComponent((DeclWarpBean) bean);
        String nodeId = StrUtil.isEmpty(nodeComponent.getNodeId()) ? SpringNodeIdHolder.getRealBeanName(clazz, beanName) : nodeComponent.getNodeId();
        SpringNodeIdHolder.add(nodeId);
        LOG.info("proxy component[{}] has been found", beanName);
        return nodeComponent;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.DECL_WARP_BEAN;
    }


}
