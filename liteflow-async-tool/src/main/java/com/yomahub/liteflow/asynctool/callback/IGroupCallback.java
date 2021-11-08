package com.yomahub.liteflow.asynctool.callback;

import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;

import java.util.List;

/**
 * 代码来自于asyncTool,请参考：https://gitee.com/jd-platform-opensource/asyncTool
 * 如果是异步执行整组的话，可以用这个组回调。不推荐使用
 * @author wuweifeng wrote on 2019-11-19.
 */
public interface IGroupCallback {
    /**
     * 成功后，可以从wrapper里去getWorkResult
     */
    void success(List<WorkerWrapper> workerWrappers);
    /**
     * 失败了，也可以从wrapper里去getWorkResult
     */
    void failure(List<WorkerWrapper> workerWrappers, Exception e);
}
