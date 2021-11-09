/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.yomahub.liteflow.asynctool.executor.Async;
import com.yomahub.liteflow.asynctool.worker.ResultState;
import com.yomahub.liteflow.asynctool.worker.WorkResult;
import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.exception.WhenExecuteException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * chain对象，实现可执行器
 *
 * @author Bryan.Zhang
 */
public class Chain implements Executable {

    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);

    private String chainName;

    private List<Condition> conditionList;

    public Chain(String chainName, List<Condition> conditionList) {
        this.chainName = chainName;
        this.conditionList = conditionList;
    }

    public List<Condition> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<Condition> conditionList) {
        this.conditionList = conditionList;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    //执行chain的主方法
    @Override
    public void execute(Integer slotIndex) throws Exception {
        if (CollUtil.isEmpty(conditionList)) {
            throw new FlowSystemException("no conditionList in this chain[" + chainName + "]");
        }

        Slot slot = DataBus.getSlot(slotIndex);

        //循环chain里包含的condition，每一个condition有可能是then，也有可能是when
        //when的话为异步，用闭锁进行等待，所有when结束后才能进入下一个condition
        for (Condition condition : conditionList) {
            if (condition instanceof ThenCondition) {
                for (Executable executableItem : condition.getNodeList()) {
                    executableItem.execute(slotIndex);
                }
            } else if (condition instanceof WhenCondition) {
                executeAsyncCondition((WhenCondition) condition, slotIndex, slot.getRequestId());
            }
        }
    }

    @Override
    public ExecuteTypeEnum getExecuteType() {
        return ExecuteTypeEnum.CHAIN;
    }

    @Override
    public String getExecuteName() {
        return chainName;
    }


    //使用线程池执行when并发流程
    private void executeAsyncCondition(WhenCondition condition, Integer slotIndex, String requestId) throws Exception{

        //此方法其实只会初始化一次Executor，不会每次都会初始化。Executor是唯一的
        ExecutorService parallelExecutor = TtlExecutors.getTtlExecutorService(ExecutorHelper.loadInstance().buildExecutor());


        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        //封装asyncTool的workerWrapper对象
        List<WorkerWrapper<Void, String>> parallelWorkerWrapperList = condition.getNodeList().stream()
                        .map(executable -> new WorkerWrapper.Builder<Void, String>()
                                .worker(new ParallelWorker(executable, slotIndex))
                                .next(new WorkerWrapper.Builder<Void, Void>().worker((object, allWrappers) -> Void.TYPE.newInstance()).build(), true)
                                .build())
                        .collect(Collectors.toList());

        boolean interrupted = false;
        boolean asyncToolResult;

        //这里利用asyncTool框架进行并行调用
        try{
            asyncToolResult = Async.beginWork(liteflowConfig.getWhenMaxWaitSeconds()*1000,
                    parallelExecutor,
                    parallelWorkerWrapperList.toArray(new WorkerWrapper[]{}));
        }catch (Exception e){
            throw new WhenExecuteException(StrUtil.format("requestId [{}] AsyncTool framework execution exception.", requestId));
        }

        //asyncToolResult为false，说明是timeout状态了
        //遍历wrapper拿到worker，拿到defaultValue，其实就是nodeId，打印出来
        if (!asyncToolResult){
            parallelWorkerWrapperList.forEach(workerWrapper -> {
                if(workerWrapper.getWorkResult().getResultState().equals(ResultState.TIMEOUT)){
                    LOG.warn("requestId [{}] executing thread has reached max-wait-seconds, thread canceled.Execute-item: [{}]",
                            requestId, workerWrapper.getWorker().defaultValue());
                }
            });
            interrupted = true;
        }

        //errorResume是一个condition里的参数，如果为true，表示即便出现了错误，也继续执行下一个condition
        //当配置了errorResume = false，出现interrupted或者其中一个线程执行出错的情况，将抛出WhenExecuteException
        if (!condition.isErrorResume()) {
            if (interrupted) {
                throw new WhenExecuteException(StrUtil.format("requestId [{}] when execute interrupted. errorResume [false].", requestId));
            }

            for (WorkerWrapper<Void, String> workerWrapper : parallelWorkerWrapperList){
                WorkResult<String> workResult = workerWrapper.getWorkResult();
                if (!workResult.getResultState().equals(ResultState.SUCCESS)){
                    throw workResult.getEx();
                }
            }
        } else if (interrupted) {
            //  这里由于配置了errorResume=true，所以只打印warn日志
            LOG.warn("requestId [{}] executing when condition timeout , but ignore with errorResume.", requestId);
        }
    }
}
