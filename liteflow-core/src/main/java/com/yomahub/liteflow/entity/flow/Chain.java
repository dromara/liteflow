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
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.exception.WhenExecuteException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.ExecutorHelper;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * chain对象，实现可执行器
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

        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        Slot slot = DataBus.getSlot(slotIndex);

        //循环chain里包含的condition，每一个condition有可能是then，也有可能是when
        //when的话为异步，用闭锁进行等待，所有when结束后才能进入下一个condition
        for (Condition condition : conditionList) {
            if (condition instanceof ThenCondition) {
                for (Executable executableItem : condition.getNodeList()) {
                    //进行重试循环判断，如果重试次数为0，则只进行一次循环
                    for (int i = 0; i <= liteflowConfig.getRetryCount(); i++) {
                        try {
                            if (i > 0){
                                LOG.info("[{}]:component[{}] performs {} retry", slot.getRequestId(), executableItem.getExecuteName(), i+1);
                            }
                            executableItem.execute(slotIndex);
                            break;
                        } catch (Exception e) {
                            if (i >= liteflowConfig.getRetryCount()){
                                throw e;
                            }
                        }
                    }
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


    //  使用线程池执行when并发流程
    private void executeAsyncCondition(WhenCondition condition, Integer slotIndex, String requestId) {
        final CountDownLatch latch = new CountDownLatch(condition.getNodeList().size());
        final List<Future<Boolean>> futures = new ArrayList<>(condition.getNodeList().size());

        //此方法其实只会初始化一次Executor，不会每次都会初始化。Executor是唯一的
        ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildExecutor();

        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        for (int i = 0; i < condition.getNodeList().size(); i++) {
            futures.add(parallelExecutor.submit(
                    new ParallelCallable(condition.getNodeList().get(i), slotIndex, requestId, latch, liteflowConfig.getRetryCount())
            ));
        }

        boolean interrupted = false;
        try {
            if (!latch.await(liteflowConfig.getWhenMaxWaitSeconds(), TimeUnit.SECONDS)) {
                for (Future<Boolean> f : futures) {
                    f.cancel(true);
                }
                interrupted = true;
                LOG.warn("requestId [{}] executing async condition has reached max-wait-seconds, condition canceled.", requestId);
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }

        //当配置了errorResume = false，出现interrupted或者!f.get()的情况，将抛出WhenExecuteException
        if (!condition.isErrorResume()) {
            if (interrupted) {
                throw new WhenExecuteException(StrUtil.format("requestId [{}] when execute interrupted. errorResume [false].", requestId));
            }

            for (Future<Boolean> f : futures) {
                try {
                    if (!f.get()) {
                        throw new WhenExecuteException(StrUtil.format("requestId [{}] when execute failed. errorResume [false].", requestId));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new WhenExecuteException(StrUtil.format("requestId [{}] when execute failed. errorResume [false].", requestId));
                }
            }
        } else if (interrupted) {
            //  这里由于配置了errorResume，所以只打印warn日志
            LOG.warn("requestId [{}] executing when condition timeout , but ignore with errorResume.", requestId);
        }
    }
}
