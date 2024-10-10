package com.yomahub.liteflow.process.impl;

import com.yomahub.liteflow.lifecycle.LifeCycle;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;

/**
 * 生命周期Bean的查找和初始化
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public class LifeCycleBeanProcess implements LiteflowScannerProcessStep {
    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Object bean = ctx.getBean();
        return bean instanceof LifeCycle;
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        Object bean = ctx.getBean();
        LifeCycleHolder.addLifeCycle((LifeCycle) bean);
        return bean;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.LIFE_CYCLE_BEAN;
    }
}
