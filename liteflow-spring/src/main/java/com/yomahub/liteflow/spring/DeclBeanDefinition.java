package com.yomahub.liteflow.spring;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 声明式类的元信息注册器
 * 目的是把声明式的类(尤其是方法级声明的类)拆分出来成为一个或多个定义
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class DeclBeanDefinition implements BeanDefinitionRegistryPostProcessor {

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;

        String[] beanDefinitionNames = defaultListableBeanFactory.getBeanDefinitionNames();

        Arrays.stream(beanDefinitionNames)
                .map(beanName -> combineAsTriple(beanName, defaultListableBeanFactory))
                .filter(this::hasLiteflowMethodAnnotation)
                .forEach(triple -> splitAndRegisterNewBeanDefinition(triple, defaultListableBeanFactory));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    /**
     * 根据 BeanName 封装一个 triple
     *
     * @param beanName                   bean 名称
     * @param defaultListableBeanFactory bean 工厂
     * @return Triple, left 是 beanName, middle 是 beanName 在工厂中对应的 beanDefinition, right 是 rawClass
     */
    private Triple<String, BeanDefinition, Class<?>> combineAsTriple(String beanName, DefaultListableBeanFactory defaultListableBeanFactory) {
        BeanDefinition beanDefinition = defaultListableBeanFactory.getMergedBeanDefinition(beanName);
        Class<?> rawClass = getRawClassFromBeanDefinition(beanDefinition);
        return Triple.of(beanName, beanDefinition, rawClass);
    }

    /**
     * 判断 triple 中的 rawClass 是否有 LiteflowMethod 注解修饰的方法
     *
     * @param triple beanName、beanDefinition、rawClass 的封装对象
     * @return true 表明 triple 中的 rawClass 有 LiteflowMethod 注解修饰的方法; false 没有
     */
    private boolean hasLiteflowMethodAnnotation(Triple<String, BeanDefinition, Class<?>> triple) {
        Class<?> rawClass = triple.getRight();
        return rawClass != null &&
                Arrays.stream(rawClass.getMethods())
                        .anyMatch(method -> AnnotationUtil.hasAnnotation(method, LiteflowMethod.class));
    }

    /**
     * 根据 triple 中的信息, 对声明式组件的 beanDefinition 进行拆分
     * 拆分成新的 beanDefinition 并注册进 Spring 工厂
     *
     * @param triple                     声明式组件原始 bean 的封装
     * @param defaultListableBeanFactory bean 工厂
     */
    private void splitAndRegisterNewBeanDefinition(Triple<String, BeanDefinition, Class<?>> triple, DefaultListableBeanFactory defaultListableBeanFactory) {
        BeanDefinition beanDefinition = triple.getMiddle();
        Class<?> rawClass = triple.getRight();
        DeclComponentParserHolder.loadDeclComponentParser()
                .parseDeclBean(rawClass)
                .forEach(declWarpBean -> registerNewBeanDefinition(declWarpBean, beanDefinition, defaultListableBeanFactory));
    }

    private void registerNewBeanDefinition(DeclWarpBean declWarpBean, BeanDefinition beanDefinition, DefaultListableBeanFactory defaultListableBeanFactory) {
        GenericBeanDefinition newBeanDefinition = new GenericBeanDefinition();
        newBeanDefinition.setBeanClass(DeclWarpBean.class);
        newBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.add("nodeId", declWarpBean.getNodeId());
        mutablePropertyValues.add("nodeName", declWarpBean.getNodeName());
        mutablePropertyValues.add("nodeType", declWarpBean.getNodeType());
        mutablePropertyValues.add("rawClazz", declWarpBean.getRawClazz());
        mutablePropertyValues.add("methodWrapBeanList", declWarpBean.getMethodWrapBeanList());
        mutablePropertyValues.add("rawBean", beanDefinition);
        newBeanDefinition.setPropertyValues(mutablePropertyValues);
        defaultListableBeanFactory.setAllowBeanDefinitionOverriding(true);
        defaultListableBeanFactory.registerBeanDefinition(declWarpBean.getNodeId(), newBeanDefinition);
    }

    private Class<?> getRawClassFromBeanDefinition(BeanDefinition beanDefinition) {
        try {
            Class<?> res = null;
            Method method = ReflectUtil.getMethodByName(DeclBeanDefinition.class, "getResolvableType");
            if (method != null) {
                Object resolvableType = ReflectUtil.invoke(beanDefinition, method);
                res = ReflectUtil.invoke(resolvableType, "getRawClass");
            }

            if (res != null)
                return res;

            res = ReflectUtil.invoke(beanDefinition, "getTargetType");
            if (res != null)
                return res;

            String beanClassName = beanDefinition.getBeanClassName();
            if (StrUtil.isNotBlank(beanClassName)) {
                try {
                    res = Class.forName(beanClassName);
                }
                catch (ClassNotFoundException ignored) {}
            }

            return res;
        }
        catch (Exception e) {
            LOG.error("An error occurred while obtaining the rawClass.", e);
            return null;
        }
    }
}
