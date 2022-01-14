package com.yomahub.liteflow.builder;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.RegexEntity;
import com.yomahub.liteflow.parser.RegexNodeEntity;

import java.util.ArrayList;

/**
 * Condition基于代码形式的组装器
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowConditionBuilder {

    protected Condition condition;

    public static LiteFlowConditionBuilder createCondition(ConditionTypeEnum conditionType){
        return new LiteFlowConditionBuilder(conditionType);
    }

    public static LiteFlowConditionBuilder createThenCondition(){
        return new LiteFlowConditionBuilder(ConditionTypeEnum.TYPE_THEN);
    }

    public static LiteFlowWhenConditionBuilder createWhenCondition(){
        return new LiteFlowWhenConditionBuilder(ConditionTypeEnum.TYPE_WHEN);
    }

    public static LiteFlowConditionBuilder createPreCondition(){
        return new LiteFlowConditionBuilder(ConditionTypeEnum.TYPE_PRE);
    }

    public static LiteFlowConditionBuilder createFinallyCondition(){
        return new LiteFlowConditionBuilder(ConditionTypeEnum.TYPE_FINALLY);
    }

    public LiteFlowConditionBuilder(ConditionTypeEnum conditionType){
        this.condition = new Condition();
        this.condition.setConditionType(conditionType);
        this.condition.setNodeList(new ArrayList<>());
    }

    public LiteFlowConditionBuilder setValue(String value){
        if (StrUtil.isBlank(value)){
            return this;
        }
        String[] condArray = value.split(",");

        RegexEntity regexEntity;
        String itemExpression;
        RegexNodeEntity item;
        for (String s : condArray) {
            itemExpression = s.trim();
            regexEntity = RegexEntity.parse(itemExpression);
            item = regexEntity.getItem();
            if (FlowBus.containNode(item.getId())) {
                Node node = FlowBus.copyNode(item.getId());
                node.setTag(regexEntity.getItem().getTag());
                this.condition.getNodeList().add(node);
                //这里判断是不是条件节点，条件节点会含有realItem，也就是括号里的node
                if (ObjectUtil.isNotNull(regexEntity.getRealItemArray())) {
                    for (RegexNodeEntity realItem : regexEntity.getRealItemArray()) {
                        if (FlowBus.containNode(realItem.getId())) {
                            Node condNode = FlowBus.copyNode(realItem.getId());
                            condNode.setTag(realItem.getTag());
                            node.setCondNode(condNode.getId(), condNode);
                        } else if (hasChain(realItem.getId())) {
                            Chain chain = FlowBus.getChain(realItem.getId());
                            node.setCondNode(chain.getChainName(), chain);
                        } else{
                            String errorMsg = StrUtil.format("executable node[{}] is not found!", realItem.getId());
                            throw new ExecutableItemNotFoundException(errorMsg);
                        }
                    }
                }
            } else if (hasChain(item.getId())) {
                Chain chain = FlowBus.getChain(item.getId());
                this.condition.getNodeList().add(chain);
            } else {
                String errorMsg = StrUtil.format("executable node[{}] is not found!", regexEntity.getItem().getId());
                throw new ExecutableItemNotFoundException(errorMsg);
            }
        }
        return this;
    }

    public Condition build(){
        return this.condition;
    }

    private boolean hasChain(String chainId){
        return FlowBus.containChain(chainId);
    }
}
