package com.yomahub.liteflow.process.impl;

import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;
import com.yomahub.liteflow.process.holder.SpringCmpAroundAspectHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 切面查找
 *
 * @author tkc
 * @since 2.12.4
 */
public class CmpAroundAspectBeanProcess implements LiteflowScannerProcessStep {
    private static final Logger LOG = LoggerFactory.getLogger(CmpAroundAspectBeanProcess.class);

    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();

        return ICmpAroundAspect.class.isAssignableFrom(clazz);
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        String beanName = ctx.getBeanName();
        Object bean = ctx.getBean();

        LOG.info("component aspect implement[{}] has been found", beanName);
        SpringCmpAroundAspectHolder.init((ICmpAroundAspect) bean);
        return bean;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.CMP_AROUND_ASPECT_BEAN;
    }
}
