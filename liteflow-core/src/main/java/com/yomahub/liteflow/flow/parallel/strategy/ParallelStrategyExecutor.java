package com.yomahub.liteflow.flow.parallel.strategy;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.exception.WhenExecuteException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.FinallyCondition;
import com.yomahub.liteflow.flow.element.condition.PreCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.CompletableFutureTimeout;
import com.yomahub.liteflow.flow.parallel.ParallelSupplier;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 并发策略执行器抽象类
 *
 * @author luo yi
 * @since 2.11.0
 */
public abstract class ParallelStrategyExecutor {

    protected final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

    /**
     * 封装 CompletableFuture 对象
     * @param executable
     * @param parallelExecutor
     * @param whenCondition
     * @param currChainName
     * @param slotIndex
     * @return
     */
    protected CompletableFuture<WhenFutureObj> wrappedFutureObj(Executable executable, ExecutorService parallelExecutor,
                                                                WhenCondition whenCondition, String currChainName, Integer slotIndex) {
        // 套入 CompletableFutureTimeout 方法进行超时判断，如果超时则用 WhenFutureObj.timeOut 返回超时的对象
        // 第 2 个参数是主要的本体 CompletableFuture，传入了 ParallelSupplier 和线程池对象
        return CompletableFutureTimeout.completeOnTimeout(
                WhenFutureObj.timeOut(executable.getId()),
                CompletableFuture.supplyAsync(new ParallelSupplier(executable, currChainName, slotIndex), parallelExecutor),
                whenCondition.getMaxWaitTime(),
                whenCondition.getMaxWaitTimeUnit());
    }

    /**
     * 设置 WhenCondition 参数
     * @param whenCondition
     */
    protected void setWhenConditionParams(WhenCondition whenCondition) {
        // 获得 liteflow 的参数
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        if (ObjectUtil.isNull(whenCondition.getMaxWaitTime())) {
            if (ObjectUtil.isNotNull(liteflowConfig.getWhenMaxWaitSeconds())) {
                // 获取全局异步线程最长等待秒数
                whenCondition.setMaxWaitTime(liteflowConfig.getWhenMaxWaitSeconds());
                whenCondition.setMaxWaitTimeUnit(TimeUnit.SECONDS);
            } else {
                // 获取全局异步线程最⻓的等待时间
                whenCondition.setMaxWaitTime(liteflowConfig.getWhenMaxWaitTime());
            }
        }

        if (ObjectUtil.isNull(whenCondition.getMaxWaitTimeUnit())) {
            // 获取全局异步线程最⻓的等待时间单位
            whenCondition.setMaxWaitTimeUnit(liteflowConfig.getWhenMaxWaitTimeUnit());
        }
    }

    /**
     * 获取所有任务 CompletableFuture 集合
     * @param whenCondition
     * @param slotIndex
     * @return
     */
    protected List<CompletableFuture<WhenFutureObj>> getWhenAllTaskList(WhenCondition whenCondition, Integer slotIndex) {

        String currChainName = whenCondition.getCurrChainId();

        // 此方法其实只会初始化一次 Executor，不会每次都会初始化。Executor是唯一的
        ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildWhenExecutor(whenCondition.getThreadExecutorClass());

        // 设置 whenCondition 参数
        setWhenConditionParams(whenCondition);

        // 这里主要是做了封装 CompletableFuture 对象，用 lumbda 表达式做了很多事情，这句代码要仔细理清
        // 1.先进行过滤，前置和后置组件过滤掉，因为在 EL Chain 处理的时候已经提出来了
        // 2.过滤 isAccess 为 false 的情况，因为不过滤这个的话，如果加上了 any，那么 isAccess 为 false 那就是最快的了
        // 3.根据 condition.getNodeList() 的集合进行流处理，用 map 进行把 executable 对象转换成 List<CompletableFuture<WhenFutureObj>>
        List<CompletableFuture<WhenFutureObj>> completableFutureList = whenCondition.getExecutableList()
                .stream()
                .filter(executable -> !(executable instanceof PreCondition) && !(executable instanceof FinallyCondition))
                .filter(executable -> {
                    try {
                        return executable.isAccess(slotIndex);
                    } catch (Exception e) {
                        LOG.error("there was an error when executing the when component isAccess", e);
                        return false;
                    }
                })
                .map(executable -> wrappedFutureObj(executable, parallelExecutor, whenCondition, currChainName, slotIndex))
                .collect(Collectors.toList());

        return completableFutureList;
    }

    /**
     * 任务结果处理
     * @param whenCondition 并行组件对象
     * @param slotIndex 当前 slot 的 index
     * @param whenAllTaskList 并行组件中所有任务列表
     * @param specifyTask 指定预先完成的任务，详见 {@link ParallelStrategyEnum}
     * @throws Exception
     */
    protected void handleTaskResult(WhenCondition whenCondition, Integer slotIndex, List<CompletableFuture<WhenFutureObj>> whenAllTaskList,
                                    CompletableFuture<?> specifyTask) throws Exception {

        Slot slot = DataBus.getSlot(slotIndex);

        // 定义是否中断参数
        // 这里为什么要定义成数组呢，因为后面 lambda 要用到，根据 final 不能修改引用的原则，这里用了数组对象
        final boolean[] interrupted = { false };

        try {
            // 进行执行，这句执行完后有三种可能，所有任务执行完成、任一任务执行完成、指定的任务执行完成
            specifyTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("there was an error when executing the CompletableFuture", e);
            interrupted[0] = true;
        }

        // 拿到已经完成的 CompletableFuture 对象
        // 如果 any 为 false，那么所有任务都已经完成
        // 如果 any 为 true，那么这里拿到的是第一个完成的任务
        // 如果为 must，那么这里获取到的就是指定的任务
        // 这里过滤和转换一起用 lambda 做了
        List<WhenFutureObj> allCompletableWhenFutureObjList = whenAllTaskList.stream().filter(f -> {
            // 过滤出已经完成的，没完成的就直接终止
            if (f.isDone()) {
                return true;
            } else {
                f.cancel(true);
                return false;
            }
        }).map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                interrupted[0] = true;
                return null;
            }
        }).collect(Collectors.toList());

        // 判断超时，上面已经拿到了所有已经完成的 CompletableFuture
        // 那我们只要过滤出超时的 CompletableFuture
        List<WhenFutureObj> timeOutWhenFutureObjList = allCompletableWhenFutureObjList.stream()
                .filter(WhenFutureObj::isTimeout)
                .collect(Collectors.toList());

        // 输出超时信息
        timeOutWhenFutureObjList.forEach(whenFutureObj -> LOG.warn(
                "executing thread has reached max-wait-seconds, thread canceled.Execute-item: [{}]", whenFutureObj.getExecutorName()));

        // 当配置了 ignoreError = false，出现 interrupted 或者 !f.get() 的情况，将抛出 WhenExecuteException
        if (!whenCondition.isIgnoreError()) {
            if (interrupted[0]) {
                throw new WhenExecuteException(StrUtil
                        .format("requestId [{}] when execute interrupted. errorResume [false].", slot.getRequestId()));
            }

            // 循环判断CompletableFuture的返回值，如果异步执行失败，则抛出相应的业务异常
            for (WhenFutureObj whenFutureObj : allCompletableWhenFutureObjList) {
                if (!whenFutureObj.isSuccess()) {
                    LOG.info(StrUtil.format("when-executor[{}] execute failed. errorResume [false].", whenFutureObj.getExecutorName()));
                    throw whenFutureObj.getEx();
                }
            }
        } else if (interrupted[0]) {
            // 这里由于配置了 ignoreError，所以只打印 warn 日志
            LOG.warn("executing when condition timeout , but ignore with errorResume.");
        }
    }

    public abstract void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception;

}
