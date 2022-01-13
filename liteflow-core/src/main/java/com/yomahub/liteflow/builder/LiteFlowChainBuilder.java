package com.yomahub.liteflow.builder;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.flow.FlowBus;

import java.util.ArrayList;

/**
 * Chain基于代码形式的组装器
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowChainBuilder {

    private final Chain chain;

    public static LiteFlowChainBuilder createChain(){
        return new LiteFlowChainBuilder();
    }

    public LiteFlowChainBuilder(){
        chain = new Chain();
        chain.setConditionList(new ArrayList<>());
    }

    public LiteFlowChainBuilder setChainName(String chainName){
        this.chain.setChainName(chainName);
        return this;
    }

    public LiteFlowChainBuilder setCondition(Condition condition){
        this.chain.getConditionList().add(condition);
        return this;
    }

    public void build(){
        FlowBus.addChain(this.chain);
    }
}
