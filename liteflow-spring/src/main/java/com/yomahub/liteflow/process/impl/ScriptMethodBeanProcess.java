package com.yomahub.liteflow.process.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.annotation.ScriptMethod;
import com.yomahub.liteflow.script.proxy.ScriptMethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 脚本方法组件查找
 *
 * @author tkc
 * @since 2.12.4
 */
public class ScriptMethodBeanProcess implements LiteflowScannerProcessStep {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptMethodBeanProcess.class);

    @Override
    public boolean filter(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();

        List<Method> outPut = Arrays.stream(clazz.getMethods()).filter(method -> {
            ScriptMethod scriptMethod = AnnoUtil.getAnnotation(method, ScriptMethod.class);
            return ObjectUtil.isNotNull(scriptMethod) && StrUtil.isNotEmpty(scriptMethod.value());
        }).collect(Collectors.toList());

        ctx.setOutPut(outPut);

        return CollUtil.isNotEmpty(outPut);
    }

    @Override
    public Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx) {
        Class clazz = ctx.getClazz();
        String beanName = ctx.getBeanName();
        Object bean = ctx.getBean();
        List<Method> scriptMethods = (List<Method>) ctx.getOutPut();

        LOG.info("script method[{}] has been found", beanName);

        Map<String, List<Method>> scriptMethodsGroupByValue = CollStreamUtil.groupBy(scriptMethods, method -> {
            ScriptMethod scriptMethod = AnnoUtil.getAnnotation(method, ScriptMethod.class);
            return scriptMethod.value();
        }, Collectors.toList());

        for (Map.Entry<String, List<Method>> entry : scriptMethodsGroupByValue.entrySet()) {
            String key = entry.getKey();
            List<Method> methods = entry.getValue();
            ScriptMethodProxy proxy = new ScriptMethodProxy(bean, clazz, methods);

            ScriptBeanManager.addScriptBean(key, proxy.getProxyScriptMethod());

        }
        return bean;
    }

    @Override
    public LiteflowScannerProcessStepEnum type() {
        return LiteflowScannerProcessStepEnum.SCRIPT_METHOD_BEAN;
    }
}
