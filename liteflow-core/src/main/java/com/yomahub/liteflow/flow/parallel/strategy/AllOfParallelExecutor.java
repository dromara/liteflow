package com.yomahub.liteflow.flow.parallel.strategy;

import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 完成全部任务
 *
 * @author luo yi
 * @since 2.11.0
 */
public class AllOfParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        // 获取所有 CompletableFuture
        List<CompletableFuture<WhenFutureObj>> completableFutureList = this.getCompletableFutureList(whenCondition, slotIndex);

        // 把这些 CompletableFuture 通过 anyOf 合成一个 CompletableFuture
        CompletableFuture<?> resultCompletableFuture = CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[] {}));

        // 结果处理
        this.handleResult(whenCondition, slotIndex, completableFutureList, resultCompletableFuture);

    }

}
