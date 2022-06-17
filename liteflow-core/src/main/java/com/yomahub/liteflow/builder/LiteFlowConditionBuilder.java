package com.yomahub.liteflow.builder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.condition.*;
import com.yomahub.liteflow.builder.entity.ExecutableEntity;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.RegexEntity;
import com.yomahub.liteflow.parser.RegexNodeEntity;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Condition基于代码形式的组装器
 * @author Bryan.Zhang
 * @since 2.6.8
 */
public class LiteFlowConditionBuilder {

    protected Condition condition;

    public static LiteFlowConditionBuilder createCondition(ConditionTypeEnum conditionType){
        switch (conditionType){
            case TYPE_THEN:
                return createThenCondition();
            case TYPE_WHEN:
                return createWhenCondition();
            case TYPE_PRE:
                return createPreCondition();
            case TYPE_FINALLY:
                return createFinallyCondition();
            default:
                return null;
        }
    }

    public static LiteFlowConditionBuilder createThenCondition(){
        return new LiteFlowConditionBuilder(new ThenCondition());
    }

    public static LiteFlowWhenConditionBuilder createWhenCondition(){
        return new LiteFlowWhenConditionBuilder(new WhenCondition());
    }

    public static LiteFlowConditionBuilder createPreCondition(){
        return new LiteFlowConditionBuilder(new PreCondition());
    }

    public static LiteFlowConditionBuilder createFinallyCondition(){
        return new LiteFlowConditionBuilder(new FinallyCondition());
    }

    public LiteFlowConditionBuilder(Condition condition){
        this.condition = condition;
    }

    public LiteFlowConditionBuilder setValue(String value){
        if (StrUtil.isBlank(value)){
            return this;
        }
        String[] condArray = value.split(",");

        RegexEntity regexEntity;
        String itemExpression;
        for (String s : condArray) {
            itemExpression = s.trim();
            regexEntity = RegexEntity.parse(itemExpression);
            // 先转化为执行实体对象
            ExecutableEntity executableEntity = convertExecutableEntity(regexEntity);
            // 构建节点或流程
            setExecutable(executableEntity);
        }
        return this;
    }

    // 将正则表达式实体转化为执行实体
    private ExecutableEntity convertExecutableEntity(RegexEntity regexEntity) {
        RegexNodeEntity item = regexEntity.getItem();
        ExecutableEntity executableEntity = new ExecutableEntity(item.getId(), item.getTag());
        if (ObjectUtil.isNotNull(regexEntity.getRealItemArray())) {
            for (RegexNodeEntity realItem : regexEntity.getRealItemArray()) {
                executableEntity.addNodeCondComponent(new ExecutableEntity(realItem.getId(), realItem.getTag()));
            }
        }
        return executableEntity;
    }

    // 设置执行节点或流程列表
    public LiteFlowConditionBuilder setExecutables(List<ExecutableEntity> executableEntities) {
        if (CollUtil.isEmpty(executableEntities)) {
            return this;
        }
        for (ExecutableEntity conditionNodeEntity : executableEntities) {
            setExecutable(conditionNodeEntity);
        }
        return this;
    }

    // 设置执行节点或者流程
    public LiteFlowConditionBuilder setExecutable(ExecutableEntity executableEntity) {
        if (FlowBus.containNode(executableEntity.getId())) {
            Node node = FlowBus.copyNode(executableEntity.getId());
            node.setTag(executableEntity.getTag());
            this.condition.getExecutableList().add(node);
            // 构建条件节点-通过是否包含条件节点列表-解析条件节点会含有realItem，也就是括号里的node
            buildCondNode(node, executableEntity.getNodeCondComponents());
        } else if (hasChain(executableEntity.getId())) {
            Chain chain = FlowBus.getChain(executableEntity.getId());
            this.condition.getExecutableList().add(chain);
        } else {
            //元数据没有的话，从spring上下文再取一遍
            //这部分有2个目的
            //一是为了防止标有@Lazy懒加载的组件，二是spring负责扫描，而用动态代码的形式加载组件这种情况。
            NodeComponent nodeComponent =  ContextAwareHolder.loadContextAware().getBean(executableEntity.getId());
            if (ObjectUtil.isNotNull(nodeComponent)){
                FlowBus.addSpringScanNode(executableEntity.getId(), nodeComponent);
                setExecutable(executableEntity);
            } else{
                String errorMsg = StrUtil.format("executable node[{}] is not found!", executableEntity.getId());
                throw new ExecutableItemNotFoundException(errorMsg);
            }
        }
        return this;
    }

    // 构建条件节点
    private void buildCondNode(Node node, List<ExecutableEntity> executableEntities) {
        if (CollUtil.isEmpty(executableEntities)) {
            return;
        }
        for (ExecutableEntity realItem : executableEntities) {
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

    public Condition build(){
        return this.condition;
    }

    private boolean hasChain(String chainId){
        return FlowBus.containChain(chainId);
    }
}
