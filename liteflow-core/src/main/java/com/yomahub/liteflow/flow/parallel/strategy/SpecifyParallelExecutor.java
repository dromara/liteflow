package com.yomahub.liteflow.flow.parallel.strategy;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * 完成指定任务执行器，使用 ID 进行比较
 *
 * @author luo yi
 * @author Bryan.Zhang
 * @since 2.11.0
 */
public class SpecifyParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        String currChainName = whenCondition.getCurrChainId();

        // 设置 whenCondition 参数
        this.setWhenConditionParams(whenCondition);

        // 获取 WHEN 所需线程池
        ExecutorService parallelExecutor = getWhenExecutorService(whenCondition);

        // 指定完成的任务
        CompletableFuture<?> specifyTask;

        // 已存在的任务 ID 集合
        Set<String> exitingTaskIdSet = new HashSet<>();

        // 指定任务列表，可以为 0 或者多个
        List<CompletableFuture<?>> specifyTaskList = new ArrayList<>();

        // 所有任务集合
        List<CompletableFuture<WhenFutureObj>> allTaskList = new ArrayList<>();

        // 遍历 when 所有 node，进行筛选及处理
        filterWhenTaskList(whenCondition.getExecutableList(), slotIndex)
                .forEach(executable -> {
                    // 处理 task，封装成 CompletableFuture 对象
                    CompletableFuture<WhenFutureObj> completableFutureTask = wrappedFutureObj(executable, parallelExecutor, whenCondition, currChainName, slotIndex);
                    // 存在 must 指定 ID 的 task，且该任务只会有一个或者没有
                    if (whenCondition.getSpecifyIdSet().contains(executable.getId())) {
                        // 设置指定任务 future 对象
                        specifyTaskList.add(completableFutureTask);
                        // 记录已存在的任务 ID
                        exitingTaskIdSet.add(executable.getId());
                    }
                    // 组合所有任务
                    allTaskList.add(completableFutureTask);
                });

        if (CollUtil.isEmpty(specifyTaskList)) {
            LOG.warn("The specified task{} was not found, waiting for all tasks to complete by default.", whenCondition.getSpecifyIdSet());
            // 不存在指定任务，则需要等待所有任务都执行完成
            specifyTask = CompletableFuture.allOf(allTaskList.toArray(new CompletableFuture[] {}));
        } else {
            // 判断 specifyIdSet 中有哪些任务是不存在的，给出提示
            Collection<String> absentTaskIdSet = CollUtil.subtract(whenCondition.getSpecifyIdSet(), exitingTaskIdSet);
            if (CollUtil.isNotEmpty(absentTaskIdSet)) {
                LOG.warn("The specified task{} was not found, you need to define and register it.", absentTaskIdSet);
            }
            // 将指定要完成的任务通过 allOf 合成一个 CompletableFuture，表示需要等待 must 方法里面所有任务完成
            specifyTask = CompletableFuture.allOf(specifyTaskList.toArray(new CompletableFuture[]{}));
        }

        // 结果处理
        this.handleTaskResult(whenCondition, slotIndex, allTaskList, specifyTask);

    }

    //在must这个场景中，需要过滤掉isAccess为false的场景
    //因为不过滤这个的话，如果加上了 any，那么 isAccess 为 false 那就是最快的了
    //换句话说，就是must这个场景，isAccess会被执行两次
    @Override
    protected Stream<Executable> filterAccess(Stream<Executable> stream, Integer slotIndex) {
        return stream.filter(executable -> {
            try {
                return executable.isAccess(slotIndex);
            } catch (Exception e) {
                LOG.error("there was an error when executing the when component isAccess", e);
                return false;
            }
        });
    }

}
