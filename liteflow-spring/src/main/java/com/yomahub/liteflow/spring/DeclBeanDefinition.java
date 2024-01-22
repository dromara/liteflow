package com.yomahub.liteflow.spring;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.spi.holder.DeclComponentParserHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;

import java.lang.reflect.Method;
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

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;

        String[] beanDefinitionNames = defaultListableBeanFactory.getBeanDefinitionNames();

        Arrays.stream(beanDefinitionNames).filter(beanName -> {
            BeanDefinition beanDefinition = defaultListableBeanFactory.getMergedBeanDefinition(beanName);
            Class<?> rawClass = getRawClassFromBeanDefinition(beanDefinition);
            if (rawClass == null){
                return false;
            }else{
                return Arrays.stream(rawClass.getMethods()).anyMatch(method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class) != null);
            }
        }).forEach(beanName -> {
            BeanDefinition beanDefinition = defaultListableBeanFactory.getMergedBeanDefinition(beanName);
            Class<?> rawClass = getRawClassFromBeanDefinition(beanDefinition);
            List<DeclWarpBean> declWarpBeanList = DeclComponentParserHolder.loadDeclComponentParser().parseDeclBean(rawClass);

            declWarpBeanList.forEach(declWarpBean -> {
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
            });
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    private Class<?> getRawClassFromBeanDefinition(BeanDefinition beanDefinition){
        try{
            Method method = ReflectUtil.getMethodByName(DeclBeanDefinition.class, "getResolvableType");
            if (method != null){
                Object resolvableType = ReflectUtil.invoke(beanDefinition, method);
                return ReflectUtil.invoke(resolvableType, "getRawClass");
            }else{
                return ReflectUtil.invoke(beanDefinition, "getTargetType");
            }
        }catch (Exception e){
            LOG.error("An error occurred while obtaining the rowClass.",e);
            return null;
        }
    }
}
