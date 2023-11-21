package com.yomahub.liteflow.spring;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.spring.vo.MethodDeclWarpVo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class MethodDeclBeanDefinition implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory)registry;
        Map<String, BeanDefinition> beanDefinitionHolderMap = (Map<String, BeanDefinition>)ReflectUtil.getFieldValue(defaultListableBeanFactory, "mergedBeanDefinitions");

        beanDefinitionHolderMap.entrySet().stream().filter(entry -> {
            Class<?> rawClass = entry.getValue().getResolvableType().getRawClass();
            LiteflowCmpDefine liteflowCmpDefine = AnnotationUtil.getAnnotation(rawClass, LiteflowCmpDefine.class);
            //必须是方法级别声明式，才进行动态BeanDefinition的注册
            if (liteflowCmpDefine == null){
                return Arrays.stream(rawClass.getMethods()).anyMatch(method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class) != null);
            }
            return false;
        }).map(new Function<Map.Entry<String, BeanDefinition>, MethodDeclWarpVo>() {
            @Override
            public MethodDeclWarpVo apply(Map.Entry<String, BeanDefinition> entry) {
                return null;
            }
        });

        System.out.println(defaultListableBeanFactory);

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("bb");
    }
}
