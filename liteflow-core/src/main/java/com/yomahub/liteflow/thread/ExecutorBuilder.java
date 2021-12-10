package com.yomahub.liteflow.thread;

import java.util.concurrent.*;

/**
 * 并行多线程执行器构造器接口
 * @author Bryan.Zhang
 * @since 2.6.6
 */
public interface ExecutorBuilder {

    ExecutorService buildExecutor();
}
