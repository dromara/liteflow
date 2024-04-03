package com.yomahub.liteflow.flow.element;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.FallbackCmpNotFoundException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.condition.ConditionKey;
import com.yomahub.liteflow.flow.element.condition.ForCondition;
import com.yomahub.liteflow.flow.element.condition.IfCondition;
import com.yomahub.liteflow.flow.element.condition.IteratorCondition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

/**
 * 降级组件代理
 *
 * @author DaleLee
 * @since 2.11.1
 */
public class FallbackNode extends Node {

    // 原节点 id
    private String expectedNodeId;

    // 降级节点
    private Node fallbackNode;

    public FallbackNode() {
        this.setType(NodeTypeEnum.FALLBACK);
    }

    public FallbackNode(String expectedNodeId) {
        this();
        this.expectedNodeId = expectedNodeId;
    }

    @Override
    public void execute(Integer slotIndex) throws Exception {
        Node node = FlowBus.getNode(this.expectedNodeId);
        if (node != null){
            this.fallbackNode = node;
        }else{
            loadFallBackNode(slotIndex);
        }
        this.fallbackNode.setCurrChainId(this.getCurrChainId());
        this.fallbackNode.execute(slotIndex);
    }

    private void loadFallBackNode(Integer slotIndex) throws Exception {
        if (ObjectUtil.isNotNull(this.fallbackNode)) {
            // 已经加载过了
            return;
        }
        Slot slot = DataBus.getSlot(slotIndex);
        Condition curCondition = slot.getCurrentCondition();
        if (ObjectUtil.isNull(curCondition)) {
            throw new FlowSystemException("The current executing condition could not be found.");
        }
        Node node = findFallbackNode(curCondition);
        if (ObjectUtil.isNull(node)) {
            throw new FallbackCmpNotFoundException(
                    StrFormatter.format("No fallback component found for [{}] in chain[{}].", this.expectedNodeId,
                            this.getCurrChainId()));
        }
        // 使用 node 的副本
        this.fallbackNode = node.clone();
    }

    private Node findFallbackNode(Condition condition) {
        ConditionTypeEnum conditionType = condition.getConditionType();
        switch (conditionType) {
            case TYPE_THEN:
            case TYPE_WHEN:
            case TYPE_PRE:
            case TYPE_FINALLY:
            case TYPE_CATCH:
                return FlowBus.getFallBackNode(NodeTypeEnum.COMMON);
            case TYPE_IF:
                return findNodeInIf((IfCondition) condition);
            case TYPE_SWITCH:
                return findNodeInSwitch((SwitchCondition) condition);
            case TYPE_FOR:
                return findNodeInFor((ForCondition) condition);
            case TYPE_WHILE:
                return findNodeInWhile((WhileCondition) condition);
            case TYPE_ITERATOR:
                return findNodeInIterator((IteratorCondition) condition);
            case TYPE_NOT_OPT:
            case TYPE_AND_OR_OPT:
                //组件降级用在与并或中，只能用在IF表达式中
                return FlowBus.getFallBackNode(NodeTypeEnum.BOOLEAN);
            default:
                return null;
        }
    }

    private Node findNodeInIf(IfCondition ifCondition) {
        Executable ifItem = ifCondition.getIfItem();
        if (ifItem == this) {
            // 需要条件组件
            return FlowBus.getFallBackNode(NodeTypeEnum.BOOLEAN);
        }

        // 需要普通组件
        return FlowBus.getFallBackNode(NodeTypeEnum.COMMON);
    }

    private Node findNodeInSwitch(SwitchCondition switchCondition) {
        Node switchNode = switchCondition.getSwitchNode();
        if (switchNode == this) {
            return FlowBus.getFallBackNode(NodeTypeEnum.SWITCH);
        }

        return FlowBus.getFallBackNode(NodeTypeEnum.COMMON);
    }

    private Node findNodeInFor(ForCondition forCondition) {
        Node forNode = forCondition.getForNode();
        if (forNode == this) {
            return FlowBus.getFallBackNode(NodeTypeEnum.FOR);
        }

        return findNodeInLoop(forCondition);
    }

    private Node findNodeInWhile(WhileCondition whileCondition) {
        Executable whileItem = whileCondition.getWhileItem();
        if (whileItem == this) {
            return FlowBus.getFallBackNode(NodeTypeEnum.BOOLEAN);
        }

        return findNodeInLoop(whileCondition);
    }

    private Node findNodeInIterator(IteratorCondition iteratorCondition) {
        Node iteratorNode = iteratorCondition.getIteratorNode();
        if (iteratorNode == this) {
            return FlowBus.getFallBackNode(NodeTypeEnum.ITERATOR);
        }

        return findNodeInLoop(iteratorCondition);
    }

    private Node findNodeInLoop(LoopCondition loopCondition) {
        Executable breakItem = loopCondition.getExecutableOne(ConditionKey.BREAK_KEY);
        if (breakItem == this) {
            return FlowBus.getFallBackNode(NodeTypeEnum.BOOLEAN);
        }

        return FlowBus.getFallBackNode(NodeTypeEnum.COMMON);
    }

    @Override
    public <T> T getItemResultMetaValue(Integer slotIndex) {
        return this.fallbackNode.getItemResultMetaValue(slotIndex);
    }

    @Override
    public boolean isAccess(Integer slotIndex) throws Exception {
        // 可能会先访问这个方法，所以在这里就要加载降级节点
        loadFallBackNode(slotIndex);
        return this.fallbackNode.isAccess(slotIndex);
    }

    @Override
    public NodeComponent getInstance() {
        if (fallbackNode == null){
            return null;
        }
        return fallbackNode.getInstance();
    }

    @Override
    public String getId() {
        return this.fallbackNode == null ? null : this.fallbackNode.getId();
    }

    @Override
    public Node clone() {
        // 代理节点不复制
        return this;
    }

    @Override
    public NodeTypeEnum getType() {
        return NodeTypeEnum.FALLBACK;
    }

    public String getExpectedNodeId() {
        return expectedNodeId;
    }

    public void setExpectedNodeId(String expectedNodeId) {
        this.expectedNodeId = expectedNodeId;
    }
}
