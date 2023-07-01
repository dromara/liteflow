package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

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

}
