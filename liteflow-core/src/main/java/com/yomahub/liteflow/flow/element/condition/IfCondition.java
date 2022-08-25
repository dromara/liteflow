package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

/**
 * 条件Condition
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class IfCondition extends Condition {

    private Executable trueCaseExecutableItem;

    private Executable falseCaseExecutableItem;

    @Override
    public void execute(Integer slotIndex) throws Exception {
        if (getIfNode().getType().equals(NodeTypeEnum.IF)) {
            //先执行IF节点
            this.getIfNode().setCurrChainName(this.getCurrChainName());
            this.getIfNode().execute(slotIndex);

            Slot slot = DataBus.getSlot(slotIndex);
            //这里可能会有spring代理过的bean，所以拿到user原始的class
            Class<?> originalClass = LiteFlowProxyUtil.getUserClass(this.getIfNode().getInstance().getClass());
            //拿到If执行过的结果
            boolean ifResult = slot.getIfResult(originalClass.getName());

            if (ifResult) {
                //trueCaseExecutableItem这个不能为空，否则执行什么呢
                if (ObjectUtil.isNull(trueCaseExecutableItem)) {
                    String errorInfo = StrUtil.format("[{}]:no if-true node found for the component[{}]", slot.getRequestId(), this.getIfNode().getInstance().getDisplayName());
                    throw new NoIfTrueNodeException(errorInfo);
                }

                //trueCaseExecutableItem 不能为前置或者后置组件
                if (trueCaseExecutableItem instanceof PreCondition || trueCaseExecutableItem instanceof FinallyCondition) {
                    String errorInfo = StrUtil.format("[{}]:if component[{}] error, if true node cannot be pre or finally", slot.getRequestId(), this.getIfNode().getInstance().getDisplayName());
                    throw new IfTargetCannotBePreOrFinallyException(errorInfo);
                }

                //执行trueCaseExecutableItem
                trueCaseExecutableItem.setCurrChainName(this.getCurrChainName());
                trueCaseExecutableItem.execute(slotIndex);
            } else {
                //falseCaseExecutableItem可以为null，但是不为null时就执行否的情况
                if (ObjectUtil.isNotNull(falseCaseExecutableItem)) {
                    //falseCaseExecutableItem 不能为前置或者后置组件
                    if (falseCaseExecutableItem instanceof PreCondition || falseCaseExecutableItem instanceof FinallyCondition) {
                        String errorInfo = StrUtil.format("[{}]:if component[{}] error, if true node cannot be pre or finally", slot.getRequestId(), this.getIfNode().getInstance().getDisplayName());
                        throw new IfTargetCannotBePreOrFinallyException(errorInfo);
                    }

                    //执行falseCaseExecutableItem
                    falseCaseExecutableItem.setCurrChainName(this.getCurrChainName());
                    falseCaseExecutableItem.execute(slotIndex);
                }
            }
        } else {
            throw new IfTypeErrorException("if instance must be NodeIfComponent");
        }
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return null;
    }

    public Executable getTrueCaseExecutableItem() {
        return trueCaseExecutableItem;
    }

    public void setTrueCaseExecutableItem(Executable trueCaseExecutableItem) {
        this.trueCaseExecutableItem = trueCaseExecutableItem;
    }

    public Executable getFalseCaseExecutableItem() {
        return falseCaseExecutableItem;
    }

    public void setFalseCaseExecutableItem(Executable falseCaseExecutableItem) {
        this.falseCaseExecutableItem = falseCaseExecutableItem;
    }

    public Node getIfNode() {
        return (Node) this.getExecutableList().get(0);
    }
}
