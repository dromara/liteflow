package com.yomahub.liteflow.spi.solon;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.CmpAroundAspect;
import org.noear.solon.Solon;

/**
 * Solon 环境全局组件切面实现
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SolonCmpAroundAspect implements CmpAroundAspect {
    public static ICmpAroundAspect cmpAroundAspect;

    static {
        Solon.context().getBeanAsyn(ICmpAroundAspect.class, bean -> {
            cmpAroundAspect = bean;
        });
    }


    @Override
    public void beforeProcess(String nodeId, Slot slot) {
        if (ObjectUtil.isNotNull(cmpAroundAspect)) {
            cmpAroundAspect.beforeProcess(nodeId, slot);
        }
    }

    @Override
    public void afterProcess(String nodeId, Slot slot) {
        if (ObjectUtil.isNotNull(cmpAroundAspect)) {
            cmpAroundAspect.afterProcess(nodeId, slot);
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
