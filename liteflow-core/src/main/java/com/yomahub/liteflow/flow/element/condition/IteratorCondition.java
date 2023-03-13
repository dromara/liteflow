package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.NoIteratorNodeException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;

import java.util.Iterator;

public class IteratorCondition extends LoopCondition{

    @Override
    public void execute(Integer slotIndex) throws Exception {
        Slot slot = DataBus.getSlot(slotIndex);
        Node iteratorNode = this.getIteratorNode();

        if (ObjectUtil.isNull(iteratorNode)){
            String errorInfo = StrUtil.format("[{}]:no iterator-node found", slot.getRequestId());
            throw new NoIteratorNodeException(errorInfo);
        }

        //先去判断isAccess方法，如果isAccess方法都返回false，整个ITERATOR表达式不执行
        if (!this.getIteratorNode().isAccess(slotIndex)){
            return;
        }

        //执行Iterator组件
        iteratorNode.setCurrChainId(this.getCurrChainId());
        iteratorNode.execute(slotIndex);

        //这里可能会有spring代理过的bean，所以拿到user原始的class
        Class<?> originalForCountClass = LiteFlowProxyUtil.getUserClass(iteratorNode.getInstance().getClass());
        //获得迭代器
        Iterator<?> it = slot.getIteratorResult(originalForCountClass.getName());

        //获得要循环的可执行对象
        Executable executableItem = this.getDoExecutor();

        //获取Break节点
        Node breakNode = this.getBreakNode();

        int index = 0;
        while(it.hasNext()){
            Object itObj = it.next();

            executableItem.setCurrChainId(this.getCurrChainId());
            //设置循环index
            setLoopIndex(executableItem, index);
            //设置循环迭代器对象
            setCurrLoopObject(executableItem, itObj);
            //执行可执行对象
            executableItem.execute(slotIndex);
            //如果break组件不为空，则去执行
            if (ObjectUtil.isNotNull(breakNode)){
                breakNode.setCurrChainId(this.getCurrChainId());
                setLoopIndex(breakNode, index);
                setCurrLoopObject(breakNode, itObj);
                breakNode.execute(slotIndex);
                Class<?> originalBreakClass = LiteFlowProxyUtil.getUserClass(breakNode.getInstance().getClass());
                boolean isBreak = slot.getBreakResult(originalBreakClass.getName());
                if (isBreak){
                    break;
                }
            }
            index++;
        }
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_ITERATOR;
    }

    public Node getIteratorNode() {
        return (Node) this.getExecutableOne(ConditionKey.ITERATOR_KEY);
    }

    public void setIteratorNode(Node iteratorNode) {
        this.addExecutable(ConditionKey.ITERATOR_KEY, iteratorNode);
    }
}
