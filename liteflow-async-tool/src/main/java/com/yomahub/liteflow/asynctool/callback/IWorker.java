package com.yomahub.liteflow.asynctool.callback;

import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * 代码来自于asyncTool,请参考：https://gitee.com/jd-platform-opensource/asyncTool
 * 每个最小执行单元需要实现该接口
 * @author wuweifeng wrote on 2019-11-19.
 */
@FunctionalInterface
public interface IWorker<T, V> {
    /**
     * 在这里做耗时操作，如rpc请求、IO等
     *
     * @param object      object
     * @param allWrappers 任务包装
     */
    V action(T object, Map<String, WorkerWrapper> allWrappers);

    /**
     * 超时、异常时，返回的默认值
     *
     * @return 默认值
     */
    default V defaultValue() {
        return null;
    }
}
