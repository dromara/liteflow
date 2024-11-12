package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.NoForNodeException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.parallel.LoopFutureObj;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 循环次数Condition
 *
 * @author Bryan.Zhang
 * @author jason
 * @since 2.9.0
 */
public class ForCondition extends LoopCondition {

    @Override
    public void executeCondition(Integer slotIndex) throws Exception {
        Slot slot = DataBus.getSlot(slotIndex);
        Node forNode = this.getForNode();
        if (ObjectUtil.isNull(forNode)) {
            String errorInfo = StrUtil.format("[{}]:no for-node found", slot.getRequestId());
            throw new NoForNodeException(errorInfo);
        }

        // 提前设置 chainId，避免无法在 isAccess 方法中获取到
        forNode.setCurrChainId(this.getCurrChainId());

        // 先去判断isAccess方法，如果isAccess方法都返回false，整个FOR表达式不执行
        if (!forNode.isAccess(slotIndex)) {
            return;
        }

        // 执行forCount组件
        forNode.execute(slotIndex);

        // 获得循环次数
        int forCount = forNode.getItemResultMetaValue(slotIndex);

        // 获得要循环的可执行对象
        Executable executableItem = this.getDoExecutor();

        // 获取Break节点
        Executable breakItem = this.getBreakItem();

        try {
            if (!isParallel()) {
                //串行循环执行
                for (int i = 0; i < forCount; i++) {
                    executableItem.setCurrChainId(this.getCurrChainId());
                    // 设置循环index
                    setLoopIndex(executableItem, i);
                    executableItem.execute(slotIndex);
                    // 如果break组件不为空，则去执行
                    if (ObjectUtil.isNotNull(breakItem)) {
                        breakItem.setCurrChainId(this.getCurrChainId());
                        setLoopIndex(breakItem, i);
                        breakItem.execute(slotIndex);
                        boolean isBreak = breakItem.getItemResultMetaValue(slotIndex);
                        if (isBreak) {
                            break;
                        }
                    }
                }
            }else{
                //并行循环执行
                //存储所有的并行执行子项的CompletableFuture
                List<CompletableFuture<LoopFutureObj>> futureList = new ArrayList<>();
                //获取并行循环的线程池
                ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildExecutorService(this, slotIndex
                        , this.getConditionType());
                for (int i = 0; i < forCount; i++){
                    //提交异步任务
                    CompletableFuture<LoopFutureObj> future =
                            CompletableFuture.supplyAsync(new LoopParallelSupplier(executableItem, this.getCurrChainId(), slotIndex, i), parallelExecutor);
                    futureList.add(future);
                    if (ObjectUtil.isNotNull(breakItem)) {
                        breakItem.setCurrChainId(this.getCurrChainId());
                        setLoopIndex(breakItem, i);
                        breakItem.execute(slotIndex);
                        boolean isBreak = breakItem.getItemResultMetaValue(slotIndex);
                        if (isBreak) {
                            break;
                        }
                    }
                }
                //等待所有的异步执行完毕
                handleFutureList(futureList);
            }
        } finally {
            removeLoopIndex(executableItem);
        }
    }

    @Override
    public ConditionTypeEnum getConditionType() {
        return ConditionTypeEnum.TYPE_FOR;
    }

    public Node getForNode() {
        return (Node) this.getExecutableOne(ConditionKey.FOR_KEY);
    }

    public void setForNode(Node forNode) {
        this.addExecutable(ConditionKey.FOR_KEY, forNode);
    }

}
