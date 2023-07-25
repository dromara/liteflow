package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.parallel.LoopFutureObj;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 循环Condition的抽象类 主要继承对象有ForCondition和WhileCondition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class LoopCondition extends Condition {
    //判断循环是否并行执行，默认为false
    private boolean parallel = false;

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

    protected void setLoopIndex(Executable executableItem, int index) {
        if (executableItem instanceof Chain) {
            ((Chain) executableItem).getConditionList().forEach(condition -> setLoopIndex(condition, index));
        } else if (executableItem instanceof Condition) {
            ((Condition) executableItem).getExecutableGroup()
                    .forEach((key, value) -> value.forEach(executable -> setLoopIndex(executable, index)));
        } else if (executableItem instanceof Node) {
            ((Node) executableItem).setLoopIndex(index);
        }
    }

    protected void setCurrLoopObject(Executable executableItem, Object obj) {
        if (executableItem instanceof Chain) {
            ((Chain) executableItem).getConditionList().forEach(condition -> setCurrLoopObject(condition, obj));
        } else if (executableItem instanceof Condition) {
            ((Condition) executableItem).getExecutableGroup()
                    .forEach((key, value) -> value.forEach(executable -> setCurrLoopObject(executable, obj)));
        } else if (executableItem instanceof Node) {
            ((Node) executableItem).setCurrLoopObject(obj);
        }
    }

    protected void removeLoopIndex(Executable executableItem) {
        if (executableItem instanceof Chain) {
            ((Chain) executableItem).getConditionList().forEach(this::removeLoopIndex);
        } else if (executableItem instanceof Condition) {
            ((Condition) executableItem).getExecutableGroup()
                    .forEach((key, value) -> value.forEach(this::removeLoopIndex));
        } else if (executableItem instanceof Node) {
            ((Node) executableItem).removeLoopIndex();
        }
    }

    protected void removeCurrLoopObject(Executable executableItem) {
        if (executableItem instanceof Chain) {
            ((Chain) executableItem).getConditionList().forEach(this::removeCurrLoopObject);
        } else if (executableItem instanceof Condition) {
            ((Condition) executableItem).getExecutableGroup()
                    .forEach((key, value) -> value.forEach(this::removeCurrLoopObject));
        } else if (executableItem instanceof Node) {
            ((Node) executableItem).removeCurrLoopObject();
        }
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
