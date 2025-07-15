package com.yomahub.liteflow.flow.parallel.strategy;

import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 完成指定阈值任务
 *
 * @author luo yi
 * @since 2.13.4
 */
public class PercentageOfParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        // 获取所有 CompletableFuture 任务
        List<CompletableFuture<WhenFutureObj>> whenAllTaskList = this.getWhenAllTaskList(whenCondition, slotIndex);

        int total = whenAllTaskList.size();

        // 计算阈值数量（向上取整）
        int thresholdCount = (int) Math.ceil(total * whenCondition.getPercentage());

        // 已完成任务收集器（对 List 加锁保证线程安全）
        List<CompletableFuture<WhenFutureObj>> completedFutures = Collections.synchronizedList(new ArrayList<>(Math.max(thresholdCount, 1) << 1));

        // 阈值触发门闩
        CompletableFuture<Void> thresholdFuture = new CompletableFuture<>();

        // 原子计数器
        AtomicInteger completedCount = new AtomicInteger(0);

        // 为每个任务添加回调
        whenAllTaskList.forEach(future ->
                future.whenComplete((result, ex) -> {
                    // 安全添加已完成任务
                    completedFutures.add(future);
                    // 检查是否达到阈值
                    if (completedCount.incrementAndGet() >= thresholdCount) {
                        // 确保只触发一次
                        if (!thresholdFuture.isDone()) {
                            thresholdFuture.complete(null);
                        }
                    }
                })
        );

        // 创建组合任务（仅包含已完成任务）
        CompletableFuture<Void> combinedTask = thresholdFuture.thenRun(() -> {
            // 达到阈值时创建 allOf 任务
            CompletableFuture.allOf(completedFutures.toArray(new CompletableFuture[]{})).join();
        });

        // 处理结果（会阻塞直到阈值任务完成）
        this.handleTaskResult(whenCondition, slotIndex, whenAllTaskList, combinedTask);

    }

}
