package com.yomahub.liteflow.flow.parallel.strategy;

import cn.hutool.core.util.ObjUtil;
import com.yomahub.liteflow.flow.element.condition.FinallyCondition;
import com.yomahub.liteflow.flow.element.condition.PreCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 完成指定任务执行器，使用 ID 进行比较
 *
 * @author luo yi
 * @since 2.11.0
 */
public class SpecifyParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        String currChainName = whenCondition.getCurrChainId();

        // 设置 whenCondition 参数
        this.setWhenConditionParams(whenCondition);

        // 此方法其实只会初始化一次Executor，不会每次都会初始化。Executor 是唯一的
        ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildWhenExecutor(whenCondition.getThreadExecutorClass());

        // 指定任务
        final CompletableFuture<?>[] specifyTask = { null };

        // 所有任务集合
        List<CompletableFuture<WhenFutureObj>> allTaskList = new ArrayList<>();

        // 遍历 when 所有 node，进行筛选及处理
        whenCondition.getExecutableList()
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
                .forEach(executable -> {
                    // 处理 task，封装成 CompletableFuture 对象
                    CompletableFuture<WhenFutureObj> completableFutureTask = wrappedFutureObj(executable, parallelExecutor, whenCondition, currChainName, slotIndex);
                    // 存在 must 指定 ID 的 task，且该任务只会有一个或者没有
                    if (whenCondition.getSpecifyId().equals(executable.getId())) {
                        // 设置指定任务 future 对象
                        specifyTask[0] = completableFutureTask;
                    }
                    // 组合所有任务
                    allTaskList.add(completableFutureTask);
                });

        if (ObjUtil.isNull(specifyTask[0])) {
            LOG.warn("The specified task[{}] was not found, waiting for all tasks to complete by default.", whenCondition.getSpecifyId());
            // 不存在指定任务，则需要等待所有任务都执行完成
            specifyTask[0] = CompletableFuture.allOf(allTaskList.toArray(new CompletableFuture[] {}));
        }

        // 结果处理
        this.handleTaskResult(whenCondition, slotIndex, allTaskList, specifyTask[0]);

    }

}
