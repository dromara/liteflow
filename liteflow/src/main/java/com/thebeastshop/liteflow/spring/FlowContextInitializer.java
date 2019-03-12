package com.thebeastshop.liteflow.spring;

import com.thebeastshop.liteflow.core.FlowExecutor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.Iterator;

public class FlowContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final static String LITEFLOW_PROPERTY = "liteFlow.ruleSource";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();

        Iterator<PropertySource<?>> it = environment.getPropertySources().iterator();
        while (it.hasNext()){
            PropertySource propertySource = it.next();
            if(propertySource.containsProperty(LITEFLOW_PROPERTY)){
                //注册scaner
                DefaultListableBeanFactory factory = (DefaultListableBeanFactory)context.getBeanFactory();
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ComponentScaner.class);
                factory.registerBeanDefinition("componentScaner", beanDefinitionBuilder.getRawBeanDefinition());
                //注册flowExecutor
                String rulePath = (String)propertySource.getProperty(LITEFLOW_PROPERTY);
                beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(FlowExecutor.class);
                beanDefinitionBuilder.addPropertyValue("rulePath",Arrays.asList(rulePath.split(",")));
                factory.registerBeanDefinition("flowExecutor", beanDefinitionBuilder.getRawBeanDefinition());
            }
        }
    }
}
