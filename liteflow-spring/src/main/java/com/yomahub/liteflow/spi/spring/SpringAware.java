package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.spi.ContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 基于代码形式的spring上下文工具类
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
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)applicationContext.getAutowireCapableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(c.getName());
        beanFactory.setAllowBeanDefinitionOverriding(true);
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
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();
        defaultListableBeanFactory.registerSingleton(beanName,bean);
        return (T) configurableApplicationContext.getBean(beanName);
    }

    @Override
    public <T> T registerOrGet(String beanName, Class<T> clazz) {
        if (ObjectUtil.isNull(applicationContext)){
            return null;
        }
        try{
            return getBean(beanName, clazz);
        }catch (Exception e){
            return registerBean(beanName, clazz);
        }
    }

    @Override
    public boolean hasBean(String beanName){
        return applicationContext.containsBean(beanName);
    }

    @Override
    public int priority() {
        return 1;
    }
}
