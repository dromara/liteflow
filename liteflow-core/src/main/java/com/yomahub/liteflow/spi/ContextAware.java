package com.yomahub.liteflow.spi;

import com.yomahub.liteflow.core.proxy.DeclWarpBean;

import java.util.Map;

/**
 * 环境容器SPI接口
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public interface ContextAware extends SpiPriority {

    <T> T getBean(String name);

    <T> T getBean(Class<T> clazz);

    <T> T registerBean(String beanName, Class<T> clazz);

    <T> T registerBean(Class<T> clazz);

    <T> T registerBean(String beanName, Object bean);

    <T> T registerOrGet(String beanName, Class<T> clazz);

    /**
     * 获取指定类型对应的所有Bean，包括子类
     *
     * @param <T>  Bean类型
     * @param type 类、接口，null表示获取所有bean
     * @return 类型对应的bean，key是bean注册的name，value是Bean
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);

    /**
     * 判断是否存在指定名称的bean
     *
     * @param beanName bean 名称
     */
    boolean hasBean(String beanName);

    /**
     * 判断是否存在指定类型的bean
     *
     * @param clazz 类型
     */
    boolean hasBean(Class<?> clazz);

    Object registerDeclWrapBean(String beanName, DeclWarpBean declWarpBean);

}
