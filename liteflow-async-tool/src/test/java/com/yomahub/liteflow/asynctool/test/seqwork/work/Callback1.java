package com.yomahub.liteflow.asynctool.test.seqwork.work;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.asynctool.callback.ICallback;
import com.yomahub.liteflow.asynctool.worker.WorkResult;

public class Callback1 implements ICallback<String, String> {
    @Override
    public void result(boolean success, String param, WorkResult<String> workResult) {
        System.out.println(StrUtil.format("开始执行{},结果为{},参数为{},workResult为{}", this.getClass().getSimpleName(), success, param, workResult.getResult()));
    }
}
