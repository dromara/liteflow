package com.yomahub.liteflow.asynctool.test.seqwork.work;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.asynctool.callback.IWorker;
import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;

import java.util.Map;

public class SeqWork3 implements IWorker<String, String> {
    @Override
    public String action(String object, Map<String, WorkerWrapper> allWrappers) {
        System.out.println(StrUtil.format("开始执行{},参数为{},当前线程ID为{}", this.getClass().getSimpleName(), object, Thread.currentThread().getId()));
        try{
            Thread.sleep(1000);
        }catch (Exception ignored){}
        return "result3";
    }
}
