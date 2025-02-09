package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.parallel.LoopFutureObj;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 循环Condition的抽象类 主要继承对象有ForCondition和WhileCondition
 *
 * @author Bryan.Zhang
 * @author jason
 * @since 2.9.0
 */
public abstract class LoopCondition extends Condition {
    //判断循环是否并行执行，默认为false
    private boolean parallel = false;
    //loop condition层级的线程池
    private String threadPoolExecutorClass;

    protected Executable getBreakItem() {
        return this.getExecutableOne(ConditionKey.BREAK_KEY);
    }

    public void setBreakItem(Executable breakNode) {
        this.addExecutable(ConditionKey.BREAK_KEY, breakNode);
    }

    protected Executable getDoExecutor() {
        return this.getExecutableOne(ConditionKey.DO_KEY);
    }

    public void setDoExecutor(Executable executable) {
        this.addExecutable(ConditionKey.DO_KEY, executable);
    }

    public String getThreadPoolExecutorClass() {
        return threadPoolExecutorClass;
    }

    public void setThreadPoolExecutorClass(String threadPoolExecutorClass) {
        this.threadPoolExecutorClass = threadPoolExecutorClass;
    }

    protected void setLoopIndex(Executable executableItem, int index) {
        LoopCondition thisCondition = this;
        LiteflowMetaOperator.getNodes(executableItem).forEach(node -> node.setLoopIndex(thisCondition, index));
    }

    protected void setCurrLoopObject(Executable executableItem, Object obj) {
        LoopCondition thisCondition = this;
        LiteflowMetaOperator.getNodes(executableItem).forEach(node -> node.setCurrLoopObject(thisCondition, obj));
    }

    protected void removeLoopIndex(Executable executableItem) {
        LiteflowMetaOperator.getNodes(executableItem).forEach(Node::removeLoopIndex);
    }

    protected void removeCurrLoopObject(Executable executableItem) {
        LiteflowMetaOperator.getNodes(executableItem).forEach(Node::removeCurrLoopObject);
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    //循环并行执行的futureList处理
    protected void handleFutureList(List<CompletableFuture<LoopFutureObj>> futureList)throws Exception{
        CompletableFuture<?> resultCompletableFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{}));
        resultCompletableFuture.get();
        //获取所有的执行结果,如果有失败的，那么需要抛出异常
        for (CompletableFuture<LoopFutureObj> future : futureList) {
            LoopFutureObj loopFutureObj = future.get();
            if (!loopFutureObj.isSuccess()) {
                throw loopFutureObj.getEx();
            }
        }
    }

    // 循环并行执行的Supplier封装
    public class LoopParallelSupplier implements Supplier<LoopFutureObj> {
        private final Executable executableItem;
        private final String currChainId;
        private final Integer slotIndex;
        private final Integer loopIndex;
        private final Object itObj;

        public LoopParallelSupplier(Executable executableItem, String currChainId, Integer slotIndex, Integer loopIndex) {
            this.executableItem = executableItem;
            this.currChainId = currChainId;
            this.slotIndex = slotIndex;
            this.loopIndex = loopIndex;
            this.itObj = null;
        }

        public LoopParallelSupplier(Executable executableItem, String currChainId, Integer slotIndex, Integer loopIndex, Object itObj) {
            this.executableItem = executableItem;
            this.currChainId = currChainId;
            this.slotIndex = slotIndex;
            this.loopIndex = loopIndex;
            this.itObj = itObj;
        }


        @Override
        public LoopFutureObj get() {
            try {
                executableItem.setCurrChainId(this.currChainId);
                // 设置循环index
                setLoopIndex(executableItem, loopIndex);
                //IteratorCondition的情况下，需要设置当前循环对象
                if(itObj != null){
                    setCurrLoopObject(executableItem, itObj);
                }
                executableItem.execute(slotIndex);
                return LoopFutureObj.success(executableItem.getId());
            } catch (Exception e) {
                return LoopFutureObj.fail(executableItem.getId(), e);
            }
        }
    }

}
