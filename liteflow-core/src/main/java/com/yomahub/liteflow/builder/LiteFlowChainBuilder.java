package com.yomahub.liteflow.builder;

import cn.hutool.core.collection.CollectionUtil;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain基于代码形式的组装器
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowChainBuilder {

    private Chain chain;

    public static LiteFlowChainBuilder createChain(){
        return new LiteFlowChainBuilder();
    }

    public LiteFlowChainBuilder(){
        chain = new Chain();
    }

    public LiteFlowChainBuilder setChainName(String chainName){
        if (FlowBus.containChain(chainName)){
            this.chain = FlowBus.getChain(chainName);
        }else{
            this.chain.setChainName(chainName);
        }
        return this;
    }

    public LiteFlowChainBuilder setCondition(Condition condition){
        //这里把condition组装进conditionList，
        buildConditions(condition);
        return this;
    }

    public void build(){
        FlowBus.addChain(this.chain);
    }

    private void buildConditions(Condition condition) {
        //这里进行合并逻辑
        //对于then来说，相邻的2个then会合并成一个condition
        //对于when来说，相同组的when会合并成一个condition，不同组的when还是会拆开
        if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_PRE)) {
            this.chain.getConditionList().add(new PreCondition(condition));
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_THEN)) {
            if (this.chain.getConditionList().size() >= 1 &&
                    CollectionUtil.getLast(this.chain.getConditionList()) instanceof ThenCondition) {
                CollectionUtil.getLast(this.chain.getConditionList()).getNodeList().addAll(condition.getNodeList());
            } else {
                this.chain.getConditionList().add(new ThenCondition(condition));
            }
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_WHEN)) {
            if (this.chain.getConditionList().size() > 1 &&
                    CollectionUtil.getLast(this.chain.getConditionList()) instanceof WhenCondition &&
                    CollectionUtil.getLast(this.chain.getConditionList()).getGroup().equals(condition.getGroup())) {
                CollectionUtil.getLast(this.chain.getConditionList()).getNodeList().addAll(condition.getNodeList());
            } else {
                this.chain.getConditionList().add(new WhenCondition(condition));
            }
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY)) {
            this.chain.getConditionList().add(new FinallyCondition(condition));
        }

        //每一次build之后，对conditionList进行排序，pre最前面，finally最后
        //这里为什么要排序，因为在声明的时候，哪怕有人不把pre放最前，finally放最后，但最终也要确保是正确的顺序
        CollectionUtil.sort(this.chain.getConditionList(), (o1, o2) -> {
            if (o1.getConditionType().equals(ConditionTypeEnum.TYPE_PRE) || o2.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY)){
                return -1;
            } else if (o2.getConditionType().equals(ConditionTypeEnum.TYPE_PRE) || o1.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY)){
                return 1;
            }
            return 0;
        });
    }
}
