package com.yomahub.liteflow.flow.parallel.strategy;

import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 完成任一任务
 *
 * @author luo yi
 * @author Bryan.Zhang
 * @since 2.11.0
 */
public class AnyOfParallelExecutor extends ParallelStrategyExecutor {

    @Override
    public void execute(WhenCondition whenCondition, Integer slotIndex) throws Exception {

        // 获取所有 CompletableFuture 任务
        List<CompletableFuture<WhenFutureObj>> whenAllTaskList = this.getWhenAllTaskList(whenCondition, slotIndex);

        // 把这些 CompletableFuture 通过 anyOf 合成一个 CompletableFuture，表明完成任一任务
        CompletableFuture<?> specifyTask = CompletableFuture.anyOf(whenAllTaskList.toArray(new CompletableFuture[] {}));

        // 结果处理
        this.handleTaskResult(whenCondition, slotIndex, whenAllTaskList, specifyTask);

    }

    //在anyOf这个场景中，需要过滤掉isAccess为false的场景
    //因为不过滤这个的话，如果加上了 any，那么 isAccess 为 false 那就是最快的了
    //换句话说，就是anyOf这个场景，isAccess会被执行两次
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
