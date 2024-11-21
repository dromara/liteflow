package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.spi.ContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * 基于代码形式的spring上下文工具类
 *
 * @author Bryan.Zhang
 */
public class SpringAware implements ApplicationContextAware, ContextAware {

    private static ApplicationContext applicationContext = null;

    public SpringAware() {
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public <T> T getBean(String name) {
        T t = (T) applicationContext.getBean(name);
        return t;
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return applicationContext.getBeansOfType(type);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        T t = applicationContext.getBean(clazz);
        return t;
    }

    private <T> T getBean(String beanName, Class<T> clazz) {
        T t = applicationContext.getBean(beanName, clazz);
        return t;
    }

    @Override
    public <T> T registerBean(String beanName, Class<T> c) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
                .getAutowireCapableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(c.getName());
        beanFactory.setAllowBeanDefinitionOverriding(true);
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        return getBean(beanName);
    }

    @Override
    public Object registerDeclWrapBean(String beanName, DeclWarpBean declWarpBean) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
                .getAutowireCapableBeanFactory();
        beanFactory.setAllowBeanDefinitionOverriding(true);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DeclWarpBean.class);
        beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.add("nodeId", declWarpBean.getNodeId());
        mutablePropertyValues.add("nodeName", declWarpBean.getNodeName());
        mutablePropertyValues.add("nodeType", declWarpBean.getNodeType());
        mutablePropertyValues.add("rawClazz", declWarpBean.getRawClazz());
        mutablePropertyValues.add("methodWrapBeanList", declWarpBean.getMethodWrapBeanList());
        mutablePropertyValues.add("rawBean", declWarpBean.getRawBean());
        beanDefinition.setPropertyValues(mutablePropertyValues);

        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        return getBean(beanName);
    }

    @Override
    public <T> T registerBean(Class<T> c) {
        return registerBean(c.getName(), c);
    }

    @Override
    public <T> T registerBean(String beanName, Object bean) {
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext
                .getAutowireCapableBeanFactory();
        defaultListableBeanFactory.registerSingleton(beanName, bean);
        return (T) configurableApplicationContext.getBean(beanName);
    }

    @Override
    public <T> T registerOrGet(String beanName, Class<T> clazz) {
        if (ObjectUtil.isNull(applicationContext)) {
            return null;
        }
        try {
            return getBean(beanName, clazz);
        } catch (Exception e) {
            return registerBean(beanName, clazz);
        }
    }

    @Override
    public boolean hasBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    @Override
    public boolean hasBean(Class<?> clazz) {
        return CollUtil.size(getBeansOfType(clazz)) > 0;
    }

    @Override
    public int priority() {
        return 1;
    }

}
