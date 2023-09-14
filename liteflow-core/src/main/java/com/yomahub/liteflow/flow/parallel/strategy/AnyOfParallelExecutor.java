package com.yomahub.liteflow.flow.parallel.strategy;

import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 完成任一任务
 *
 * @author luo yi
 * @since 2.11.0
 */
public class AnyOfParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        // 获取所有 CompletableFuture 任务
        List<CompletableFuture<WhenFutureObj>> allTaskList = this.getAllTaskList(whenCondition, slotIndex);

        // 把这些 CompletableFuture 通过 anyOf 合成一个 CompletableFuture，表明完成任一任务
        CompletableFuture<?> specifyTask = CompletableFuture.anyOf(allTaskList.toArray(new CompletableFuture[] {}));

        // 结果处理
        this.handleTaskResult(whenCondition, slotIndex, allTaskList, specifyTask);

    }

}
