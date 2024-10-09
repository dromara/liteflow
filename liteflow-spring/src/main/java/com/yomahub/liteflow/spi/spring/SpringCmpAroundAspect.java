package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.process.holder.SpringCmpAroundAspectHolder;
import com.yomahub.liteflow.spi.CmpAroundAspect;

/**
 * Spring环境全局组件切面实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SpringCmpAroundAspect implements CmpAroundAspect {

    @Override
    public void beforeProcess(NodeComponent cmp) {
        ICmpAroundAspect instance = SpringCmpAroundAspectHolder.getInstance();
        if (ObjectUtil.isNotNull(instance)) {
            instance.beforeProcess(cmp);
        }
    }

    @Override
    public void afterProcess(NodeComponent cmp) {
        ICmpAroundAspect instance = SpringCmpAroundAspectHolder.getInstance();
        if (ObjectUtil.isNotNull(instance)) {
            instance.afterProcess(cmp);
        }
    }

    @Override
    public void onSuccess(NodeComponent cmp) {
        ICmpAroundAspect instance = SpringCmpAroundAspectHolder.getInstance();
        if (ObjectUtil.isNotNull(instance)) {
            instance.onSuccess(cmp);
        }
    }

    @Override
    public void onError(NodeComponent cmp, Exception e) {
        ICmpAroundAspect instance = SpringCmpAroundAspectHolder.getInstance();
        if (ObjectUtil.isNotNull(instance)) {
            instance.onError(cmp, e);
        }
    }

    @Override
    public int priority() {
        return 1;
    }

}
