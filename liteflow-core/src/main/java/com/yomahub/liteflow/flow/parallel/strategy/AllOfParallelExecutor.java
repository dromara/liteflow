package com.yomahub.liteflow.flow.parallel.strategy;

import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 完成全部任务
 *
 * @author luo yi
 * @author Bryan.Zhang
 * @since 2.11.0
 */
public class AllOfParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        // 获取所有 CompletableFuture 任务
        List<CompletableFuture<WhenFutureObj>> whenAllTaskList = this.getWhenAllTaskList(whenCondition, slotIndex);

        // 把这些 CompletableFuture 通过 allOf 合成一个 CompletableFuture，表明完成所有任务
        CompletableFuture<?> specifyTask = CompletableFuture.allOf(whenAllTaskList.toArray(new CompletableFuture[] {}));

        // 结果处理
        this.handleTaskResult(whenCondition, slotIndex, whenAllTaskList, specifyTask);

    }

    // 在 allOf 这个场景中，不需要过滤
    @Override
    protected Stream<Executable> filterAccess(Stream<Executable> stream, Integer slotIndex, String currentChainId) {
        return stream;
    }

}
