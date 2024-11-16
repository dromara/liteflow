package com.yomahub.liteflow.spi.local;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.spi.ContextAware;

import java.util.Map;

/**
 * 非Spring环境容器实现 其实非Spring没有环境容器，所以这是个空实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class LocalContextAware implements ContextAware {

    @Override
    public <T> T getBean(String name) {
        return null;
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T registerBean(String beanName, Class<T> clazz) {
        return ReflectUtil.newInstanceIfPossible(clazz);
    }

    @Override
    public <T> T registerBean(Class<T> clazz) {
        return registerBean(null, clazz);
    }

    @Override
    public <T> T registerBean(String beanName, Object bean) {
        return (T) bean;
    }

    @Override
    public <T> T registerOrGet(String beanName, Class<T> clazz) {
        return registerBean(beanName, clazz);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return null;
    }

    @Override
    public boolean hasBean(String beanName) {
        return false;
    }

    @Override
    public boolean hasBean(Class<?> clazz) {
        return false;
    }

    @Override
    public Object registerDeclWrapBean(String beanName, DeclWarpBean declWarpBean) {
        return null;
    }

    @Override
    public int priority() {
        return 2;
    }

}
