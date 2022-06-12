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
import com.yomahub.liteflow.flow.element.condition.Condition;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
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

    //主体Condition
    private List<Condition> conditionList = new ArrayList<>();

    //前置处理Condition，用来区别主体的Condition
    private List<Condition> preConditionList = new ArrayList<>();

    //后置处理Condition，用来区别主体的Condition
    private List<Condition> finallyConditionList = new ArrayList<>();

    public Chain(String chainName){
        this.chainName = chainName;
    }

    public Chain(){}

    public Chain(String chainName, List<Condition> conditionList) {
        this.chainName = chainName;
        this.conditionList = conditionList;
    }

    //执行chain的主方法
    @Override
    public void execute(Integer slotIndex) throws Exception {
        if (CollUtil.isEmpty(conditionList)) {
            throw new FlowSystemException("no conditionList in this chain[" + chainName + "]");
        }
        for (Condition condition : conditionList) {
            condition.execute(slotIndex);
        }
    }

    // 执行pre节点
    public void executePre(Integer slotIndex) throws Exception {
        for (Condition condition : this.preConditionList){
            condition.execute(slotIndex);
        }
    }

    public void executeFinally(Integer slotIndex) throws Exception {
        for (Condition condition : this.finallyConditionList){
            condition.execute(slotIndex);
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

    public List<Condition> getPreConditionList() {
        return preConditionList;
    }

    public void setPreConditionList(List<Condition> preConditionList) {
        this.preConditionList = preConditionList;
    }

    public List<Condition> getFinallyConditionList() {
        return finallyConditionList;
    }

    public void setFinallyConditionList(List<Condition> finallyConditionList) {
        this.finallyConditionList = finallyConditionList;
    }
}
