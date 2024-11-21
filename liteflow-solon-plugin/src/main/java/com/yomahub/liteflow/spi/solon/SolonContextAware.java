package com.yomahub.liteflow.spi.solon;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.spi.ContextAware;
import org.noear.solon.Solon;
import org.noear.solon.core.BeanWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于代码形式的 Solon 上下文工具类
 *
 * @author Bryan.Zhang
 */
public class SolonContextAware implements ContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(SolonContextAware.class);

    @Override
    public <T> T getBean(String name) {
        try {
            return Solon.context().getBean(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        try {
            return Solon.context().getBean(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T getBean(String beanName, Class<T> clazz) {
        try {
            return Solon.context().getBean(beanName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T registerBean(String beanName, Class<T> c) {
        BeanWrap beanWrap = Solon.context().getWrap(beanName);
        if (beanWrap == null) {
            beanWrap = new BeanWrap(Solon.context(), c, null, beanName);
            Solon.context().putWrap(beanName, beanWrap);
            Solon.context().putWrap(c, beanWrap);
        }

        return beanWrap.get();
    }

    @Override
    public <T> T registerBean(Class<T> c) {
        return registerBean(c.getName(), c);
    }

    @Override
    public <T> T registerBean(String beanName, Object bean) {
        BeanWrap beanWrap = Solon.context().getWrap(beanName);
        if (beanWrap == null) {
            beanWrap = Solon.context().wrap(beanName, bean);
            Solon.context().putWrap(beanName, beanWrap);
        }

        return beanWrap.get();
    }

    @Override
    public Object registerDeclWrapBean(String beanName, DeclWarpBean declWarpBean) {
        BeanWrap beanWrap = Solon.context().getWrap(beanName);
        if (beanWrap == null) {
            NodeComponent nodeComponent = LiteFlowProxyUtil.proxy2NodeComponent(declWarpBean);
            beanWrap = Solon.context().wrap(beanName, nodeComponent);
            Solon.context().putWrap(beanName, beanWrap);
        }

        LOG.info("proxy component[{}] has been found", beanName);
        return beanWrap.get();
    }

    @Override
    public <T> T registerOrGet(String beanName, Class<T> clazz) {
        T t = getBean(beanName, clazz);
        if (ObjectUtil.isNull(t)) {
            t = registerBean(beanName, clazz);
        }
        return t;
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        List<BeanWrap> wrapsOfType = Solon.context().getWrapsOfType(type);
        return CollUtil.toMap(wrapsOfType, new HashMap<String, T>(), BeanWrap::name, BeanWrap::get);
    }

    @Override
    public boolean hasBean(String beanName) {
        return Solon.context().hasWrap(beanName);
    }

    @Override
    public boolean hasBean(Class<?> clazz) {
        return CollUtil.size(getBeansOfType(clazz)) > 0;
    }

    @Override
    public int priority() {
        return 1;
    }
}
