package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.NoWhileNodeException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

/**
 * 循环条件Condition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class WhileCondition extends LoopCondition{

    private Node whileNode;

    @Override
    public void execute(Integer slotIndex) throws Exception {
        Slot slot = DataBus.getSlot(slotIndex);
        if (ObjectUtil.isNull(whileNode)){
            String errorInfo = StrUtil.format("[{}]:no while-node found", slot.getRequestId());
            throw new NoWhileNodeException(errorInfo);
        }

        //获得要循环的可执行对象
        Executable executableItem = this.getDoExecutor();

        //循环执行
        while(getWhileResult(slotIndex)){
            executableItem.execute(slotIndex);
            //如果break组件不为空，则去执行
            if (ObjectUtil.isNotNull(breakNode)){
                breakNode.setCurrChainName(this.getCurrChainName());
                Class<?> originalBreakClass = LiteFlowProxyUtil.getUserClass(this.breakNode.getClass());
                boolean isBreak = slot.getBreakResult(originalBreakClass.getName());
                if (isBreak){
                    break;
                }
            }
        }
    }

    private boolean getWhileResult(Integer slotIndex) throws Exception{
        Slot slot = DataBus.getSlot(slotIndex);
        //执行while组件
        whileNode.setCurrChainName(this.getCurrChainName());
        whileNode.execute(slotIndex);
        Class<?> originalWhileClass = LiteFlowProxyUtil.getUserClass(this.whileNode.getClass());
        return slot.getWhileResult(originalWhileClass.getName());
    }

    public Executable getDoExecutor() {
        return this.getExecutableList().get(0);
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_WHILE;
    }

    public Node getWhileNode() {
        return whileNode;
    }

    public void setWhileNode(Node whileNode) {
        this.whileNode = whileNode;
    }
}
