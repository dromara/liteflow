package com.yomahub.liteflow.process.impl;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.annotation.ScriptBean;
import com.yomahub.liteflow.script.proxy.ScriptBeanProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本 bean 组件查找
 *
 * @author tkc
 * @since 2.12.4
 */
public class ScriptBeanProcess implements LiteflowScannerProcessStep {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptBeanProcess.class);

    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();

        ScriptBean outPut = AnnoUtil.getAnnotation(clazz, ScriptBean.class);
        ctx.setOutPut(outPut);

        return ObjectUtil.isNotNull(outPut);
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();
        String beanName = ctx.getBeanName();
        Object bean = ctx.getBean();
        ScriptBean scriptBean = (ScriptBean) ctx.getOutPut();

        LOG.info("script bean[{}] has been found", beanName);
        ScriptBeanProxy proxy = new ScriptBeanProxy(bean, clazz, scriptBean);
        ScriptBeanManager.addScriptBean(scriptBean.value(), proxy.getProxyScriptBean());
        return bean;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.SCRIPT_BEAN;
    }
}
