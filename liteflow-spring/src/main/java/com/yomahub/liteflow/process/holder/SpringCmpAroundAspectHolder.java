package com.yomahub.liteflow.process.holder;

import com.yomahub.liteflow.aop.ICmpAroundAspect;

/**
 * 全局组件拦截器持有器
 *
 * @author tkc
 * @since 2.12.4
 */
public class SpringCmpAroundAspectHolder {
    private static ICmpAroundAspect CMP_AROUND_ASPECT;

    public static void init(ICmpAroundAspect cmpAroundAspect) {
        CMP_AROUND_ASPECT = cmpAroundAspect;
    }

    public static ICmpAroundAspect getInstance() {
        return CMP_AROUND_ASPECT;
    }

    public static void clean() {
        CMP_AROUND_ASPECT = null;
    }
}
