package com.yomahub.liteflow.spi;

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

    <T> T registerOrGet(String beanName, Class<T> clazz);
}
