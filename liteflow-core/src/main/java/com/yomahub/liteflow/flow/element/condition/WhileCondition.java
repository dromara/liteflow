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
        int index = 0;
        while(getWhileResult(slotIndex)){
            executableItem.setCurrChainId(this.getCurrChainId());
            setLoopIndex(executableItem, index);
            executableItem.execute(slotIndex);
            //如果break组件不为空，则去执行
            if (ObjectUtil.isNotNull(breakNode)){
                breakNode.setCurrChainId(this.getCurrChainId());
                setLoopIndex(breakNode, index);
                breakNode.execute(slotIndex);
                Class<?> originalBreakClass = LiteFlowProxyUtil.getUserClass(this.breakNode.getInstance().getClass());
                boolean isBreak = slot.getBreakResult(originalBreakClass.getName());
                if (isBreak){
                    break;
                }
            }
            index++;
        }
    }

    private boolean getWhileResult(Integer slotIndex) throws Exception{
        Slot slot = DataBus.getSlot(slotIndex);
        //执行while组件
        whileNode.setCurrChainId(this.getCurrChainId());
        whileNode.execute(slotIndex);
        Class<?> originalWhileClass = LiteFlowProxyUtil.getUserClass(this.whileNode.getInstance().getClass());
        return slot.getWhileResult(originalWhileClass.getName());
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
