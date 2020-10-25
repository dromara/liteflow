package com.yomahub.liteflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringAware implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringAware.class);
    private static ApplicationContext applicationContext = null;

    public SpringAware() {
    }

    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static <T> T registerBean(Class<T> c) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)applicationContext.getAutowireCapableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(c.getName());
        beanFactory.registerBeanDefinition(c.getName(), beanDefinition);
        return getBean(c.getName());
    }

    public static <T> T registerOrGet(Class<T> clazz){
        T t = null;
        try{
            t = SpringAware.getBean(clazz);
        }catch (NoSuchBeanDefinitionException e){
            if(t == null){
                t = SpringAware.registerBean(clazz);
            }
        }
        return t;
    }
}
