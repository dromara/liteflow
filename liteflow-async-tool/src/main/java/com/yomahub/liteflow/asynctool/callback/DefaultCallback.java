package com.yomahub.liteflow.asynctool.callback;

import com.yomahub.liteflow.asynctool.worker.WorkResult;

/**
 * 代码来自于asyncTool,请参考：https://gitee.com/jd-platform-opensource/asyncTool
 * 默认回调类，如果不设置的话，会默认给这个回调
 * @author wuweifeng wrote on 2019-11-19.
 */
public class DefaultCallback<T, V> implements ICallback<T, V> {
    @Override
    public void begin() {
        
    }

    @Override
    public void result(boolean success, T param, WorkResult<V> workResult) {

    }

}
