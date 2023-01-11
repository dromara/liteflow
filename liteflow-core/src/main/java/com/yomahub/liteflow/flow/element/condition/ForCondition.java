package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.NoForNodeException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

/**
 * 循环次数Condition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ForCondition extends LoopCondition{

    private Node forNode;

    @Override
    public void execute(Integer slotIndex) throws Exception {
        Slot slot = DataBus.getSlot(slotIndex);
        if (ObjectUtil.isNull(forNode)){
            String errorInfo = StrUtil.format("[{}]:no for-node found", slot.getRequestId());
            throw new NoForNodeException(errorInfo);
        }

        //执行forCount组件
        forNode.setCurrChainId(this.getCurrChainId());
        forNode.execute(slotIndex);

        //这里可能会有spring代理过的bean，所以拿到user原始的class
        Class<?> originalForCountClass = LiteFlowProxyUtil.getUserClass(this.forNode.getInstance().getClass());
        //获得循环次数
        int forCount = slot.getForResult(originalForCountClass.getName());

        //获得要循环的可执行对象
        Executable executableItem = this.getDoExecutor();

        //循环执行
        for (int i = 0; i < forCount; i++) {
            executableItem.setCurrChainId(this.getCurrChainId());
            //设置循环index
            setLoopIndex(executableItem, i);
            executableItem.execute(slotIndex);
            //如果break组件不为空，则去执行
            if (ObjectUtil.isNotNull(breakNode)){
                breakNode.setCurrChainId(this.getCurrChainId());
                setLoopIndex(breakNode, i);
                breakNode.execute(slotIndex);
                Class<?> originalBreakClass = LiteFlowProxyUtil.getUserClass(this.breakNode.getInstance().getClass());
                boolean isBreak = slot.getBreakResult(originalBreakClass.getName());
                if (isBreak){
                    break;
                }
            }
        }
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_FOR;
    }

    public Node getForNode() {
        return forNode;
    }

    public void setForNode(Node forNode) {
        this.forNode = forNode;
    }
}
