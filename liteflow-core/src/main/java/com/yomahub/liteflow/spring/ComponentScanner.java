/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.spring;

import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.util.LOGOPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.*;

/**
 * 组件扫描类，只要是NodeComponent的实现类，都可以被这个扫描器扫到
 * @author Bryan.Zhang
 */
public class ComponentScanner implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);

    public static Map<String, NodeComponent> nodeComponentMap = new HashMap<>();

    public static ICmpAroundAspect cmpAroundAspect;

    static {
        // 打印liteflow的LOGO
        LOGOPrinter.print();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        // 组件的扫描发现，扫到之后缓存到类属性map中
        if (NodeComponent.class.isAssignableFrom(clazz)) {
            LOG.info("component[{}] has been found", beanName);
            NodeComponent nodeComponent = (NodeComponent) bean;
            nodeComponentMap.put(beanName, nodeComponent);
        }

        // 组件Aop的实现类加载
        if (ICmpAroundAspect.class.isAssignableFrom(clazz)) {
            LOG.info("component aspect implement[{}] has been found", beanName);
            cmpAroundAspect = (ICmpAroundAspect) bean;
        }

        return bean;
    }

    public static void cleanCache() {
        nodeComponentMap.clear();
    }
}
