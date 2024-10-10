/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.spring;

import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.process.LiteflowScannerProcessStep;
import com.yomahub.liteflow.process.LiteflowScannerProcessStepFactory;
import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.holder.SpringNodeIdHolder;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.util.LOGOPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Optional;

/**
 * 组件扫描类，只要是NodeComponent的实现类，都可以被这个扫描器扫到
 *
 * @author Bryan.Zhang
 */
public class ComponentScanner implements BeanPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);


    private final LiteflowScannerProcessStepFactory liteflowScannerProcessStepFactory = new LiteflowScannerProcessStepFactory();

    public ComponentScanner() {
        LOGOPrinter.print();
    }

    public ComponentScanner(LiteflowConfig liteflowConfig) {
        if (liteflowConfig.getPrintBanner()) {
            // 打印liteflow的LOGO
            LOGOPrinter.print();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = LiteFlowProxyUtil.getUserClass(bean.getClass());

        LiteflowScannerProcessStepContext ctx = LiteflowScannerProcessStepContext.of(bean, beanName, clazz);
        // 遍历所有finder，找到匹配的处理器
        Optional<LiteflowScannerProcessStep> finderOpt = liteflowScannerProcessStepFactory.getSteps()
                .stream()
                .filter(t -> t.filter(ctx))
                .findFirst();
        // 如果找到命中规则的处理器，就使用处理器对 bean 进行处理
        if (finderOpt.isPresent()) {
            return finderOpt.get().postProcessAfterInitialization(ctx);
        }
        return bean;
    }

    /**
     * 用于清除 spring 上下文扫描到的组件实体
     */
    public static void cleanCache() {
        SpringNodeIdHolder.getNodeIdSet().clear();
    }
}
