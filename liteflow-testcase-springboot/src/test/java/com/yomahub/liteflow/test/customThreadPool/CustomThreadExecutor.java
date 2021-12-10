package com.yomahub.liteflow.test.customThreadPool;

import com.yomahub.liteflow.thread.ExecutorBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomThreadExecutor implements ExecutorBuilder {
    @Override
    public ExecutorService buildExecutor() {
        return Executors.newCachedThreadPool();
    }
}
