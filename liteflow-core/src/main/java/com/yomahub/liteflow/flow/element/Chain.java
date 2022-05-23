/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.flow.parallel.CompletableFutureTimeout;
import com.yomahub.liteflow.flow.parallel.ParallelSupplier;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.ExecuteTypeEnum;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.exception.WhenExecuteException;
import com.yomahub.liteflow.flow.condition.Condition;
import com.yomahub.liteflow.flow.condition.ThenCondition;
import com.yomahub.liteflow.flow.condition.WhenCondition;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.thread.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * chain对象，实现可执行器
 *
 * @author Bryan.Zhang
 */
public class Chain implements Executable {

    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);

    private String chainName;

    private List<Condition> conditionList = new ArrayList<>();

    public Chain(String chainName){
        this.chainName = chainName;
    }

    public Chain(){}

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
        for (Condition condition : conditionList) {
            if (condition instanceof ThenCondition) {
                for (Executable executableItem : condition.getNodeList()) {
                    executableItem.execute(slotIndex);
                }
            } else if (condition instanceof WhenCondition) {
                executeAsyncCondition((WhenCondition) condition, slotIndex);
            }
        }
    }

    // 执行pre节点
    public void executePre(Integer slotIndex) throws Exception {
        doExecuteForPointConditionType(slotIndex, ConditionTypeEnum.TYPE_PRE);
    }

    public void executeFinally(Integer slotIndex) throws Exception {
        doExecuteForPointConditionType(slotIndex, ConditionTypeEnum.TYPE_FINALLY);
    }

    // 执行指定的conditionType的节点
    private void doExecuteForPointConditionType(Integer slotIndex, ConditionTypeEnum conditionTypeEnum)  throws Exception {
        //先把指定condition类型的节点过滤出来
        List<Condition> conditions =filterCondition(conditionTypeEnum);
        for (Condition condition : conditions){
            for(Executable executableItem : condition.getNodeList()){
                executableItem.execute(slotIndex);
            }
        }
    }

    // 根据节点condition类型过去出节点列表
    private List<Condition> filterCondition(ConditionTypeEnum conditionTypeEnum) {
        assert conditionTypeEnum != null;
        return  conditionList.stream().filter(condition ->
                condition.getConditionType().equals(conditionTypeEnum)).collect(Collectors.toList());
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
    //这块涉及到挺多的多线程逻辑，所以注释比较详细，看到这里的童鞋可以仔细阅读
    private void executeAsyncCondition(WhenCondition condition, Integer slotIndex) throws Exception{
        Slot slot = DataBus.getSlot(slotIndex);

        //此方法其实只会初始化一次Executor，不会每次都会初始化。Executor是唯一的
        ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildWhenExecutor(condition.getThreadExecutorClass());

        //获得liteflow的参数
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        //定义是否中断参数
        //这里为什么要定义成数组呢，因为后面lambda要用到，根据final不能修改引用的原则，这里用了数组对象
        final boolean[] interrupted = {false};

        //这里主要是做了封装CompletableFuture对象，用lumbda表达式做了很多事情，这句代码要仔细理清
        //1.根据condition.getNodeList()的集合进行流处理，用map进行把executable对象转换成List<CompletableFuture<WhenFutureObj>>
        //2.在转的过程中，套入CompletableFutureTimeout方法进行超时判断，如果超时则用WhenFutureObj.timeOut返回超时的对象
        //3.第2个参数是主要的本体CompletableFuture，传入了ParallelSupplier和线程池对象
        List<CompletableFuture<WhenFutureObj>> completableFutureList = condition.getNodeList().stream().map(
                    executable -> CompletableFutureTimeout.completeOnTimeout(
                                    WhenFutureObj.timeOut(executable.getExecuteName()),
                                    CompletableFuture.supplyAsync(new ParallelSupplier(executable, slotIndex), parallelExecutor),
                                    liteflowConfig.getWhenMaxWaitSeconds(),
                                    TimeUnit.SECONDS
                                  )
                    ).collect(Collectors.toList());


        CompletableFuture<?> resultCompletableFuture;

        //这里判断执行方式
        //如果any为false，说明这些异步任务全部执行好或者超时，才返回
        //如果any为true，说明这些异步任务只要任意一个执行完成，就返回
        if(condition.isAny()){
            //把这些CompletableFuture通过anyOf合成一个CompletableFuture
            resultCompletableFuture = CompletableFuture.anyOf(completableFutureList.toArray(new CompletableFuture[]{}));
        }else{
            //把这些CompletableFuture通过allOf合成一个CompletableFuture
            resultCompletableFuture = CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[]{}));
        }

        try {
            //进行执行，这句执行完后，就意味着所有的任务要么执行完毕，要么超时返回
            resultCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("there was an error when executing the CompletableFuture",e);
            interrupted[0] = true;
        }

        //拿到已经完成的CompletableFuture
        //如果any为false，那么所有任务都已经完成
        //如果any为true，那么这里拿到的是第一个完成的任务
        //这里过滤和转换一起用lumbda做了
        List<WhenFutureObj> allCompletableWhenFutureObjList = completableFutureList.stream().filter(f -> {
            //过滤出已经完成的，没完成的就直接终止
            if (f.isDone()){
                return true;
            }else{
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

        //判断超时，上面已经拿到了所有已经完成的CompletableFuture
        //那我们只要过滤出超时的CompletableFuture
        List<WhenFutureObj> timeOutWhenFutureObjList = allCompletableWhenFutureObjList.stream().filter(WhenFutureObj::isTimeout).collect(Collectors.toList());

        //输出超时信息
        timeOutWhenFutureObjList.forEach(whenFutureObj ->
                LOG.warn("requestId [{}] executing thread has reached max-wait-seconds, thread canceled.Execute-item: [{}]", slot.getRequestId(), whenFutureObj.getExecutorName()));

        //当配置了errorResume = false，出现interrupted或者!f.get()的情况，将抛出WhenExecuteException
        if (!condition.isErrorResume()) {
            if (interrupted[0]) {
                throw new WhenExecuteException(StrUtil.format("requestId [{}] when execute interrupted. errorResume [false].", slot.getRequestId()));
            }

            //循环判断CompletableFuture的返回值，如果异步执行失败，则抛出相应的业务异常
            for(WhenFutureObj whenFutureObj : allCompletableWhenFutureObjList){
                if (!whenFutureObj.isSuccess()){
                    LOG.info(StrUtil.format("requestId [{}] when-executor[{}] execute failed. errorResume [false].", whenFutureObj.getExecutorName(), slot.getRequestId()));
                    throw whenFutureObj.getEx();
                }
            }
        } else if (interrupted[0]) {
            //  这里由于配置了errorResume，所以只打印warn日志
            LOG.warn("requestId [{}] executing when condition timeout , but ignore with errorResume.", slot.getRequestId());
        }
    }
}
