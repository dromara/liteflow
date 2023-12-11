package com.yomahub.liteflow.spring;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 声明式类的元信息注册器
 * 目的是把声明式的类(尤其是方法级声明的类)拆分出来成为一个或多个定义
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class DeclBeanDefinition implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;
        Map<String, BeanDefinition> beanDefinitionHolderMap = (Map<String, BeanDefinition>)ReflectUtil.getFieldValue(defaultListableBeanFactory, "mergedBeanDefinitions");

        beanDefinitionHolderMap.entrySet().stream().filter(entry -> {
            Class<?> rawClass = entry.getValue().getResolvableType().getRawClass();
            return Arrays.stream(rawClass.getMethods()).anyMatch(method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class) != null);
        }).forEach(entry -> {
            Class<?> rawClass = entry.getValue().getResolvableType().getRawClass();
            List<DeclWarpBean> declWarpBeanList = DeclComponentParserHolder.loadDeclComponentParser().parseDeclBean(rawClass);

            declWarpBeanList.forEach(declWarpBean -> {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(DeclWarpBean.class);
                beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
                mutablePropertyValues.add("nodeId", declWarpBean.getNodeId());
                mutablePropertyValues.add("nodeName", declWarpBean.getNodeName());
                mutablePropertyValues.add("nodeType", declWarpBean.getNodeType());
                mutablePropertyValues.add("rawClazz", declWarpBean.getRawClazz());
                mutablePropertyValues.add("methodWrapBeanList", declWarpBean.getMethodWrapBeanList());
                mutablePropertyValues.add("rawBean", entry.getValue());
                beanDefinition.setPropertyValues(mutablePropertyValues);
                defaultListableBeanFactory.setAllowBeanDefinitionOverriding(true);
                defaultListableBeanFactory.registerBeanDefinition(declWarpBean.getNodeId(), beanDefinition);
            });

        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
