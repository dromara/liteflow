package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 循环Condition的抽象类
 * 主要继承对象有ForCondition和WhileCondition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class LoopCondition extends Condition {

    private final String DO_ITEM = "DO_ITEM";

    private final String BREAK_ITEM = "BREAK_ITEM";

    protected Node getBreakNode() {
        return (Node) this.getExecutableOne(BREAK_ITEM);
    }

    public void setBreakNode(Node breakNode) {
        this.addExecutable(BREAK_ITEM, breakNode);
    }

    protected Executable getDoExecutor() {
        return this.getExecutableOne(DO_ITEM);
    }

    public void setDoExecutor(Executable executable) {
        this.addExecutable(DO_ITEM, executable);
    }

    protected void setLoopIndex(Executable executableItem, int index){
        if (executableItem instanceof Chain){
            ((Chain)executableItem).getConditionList().forEach(condition -> setLoopIndex(condition, index));
        }else if(executableItem instanceof Condition){
            ((Condition) executableItem).getExecutableGroup().forEach((key, value) -> value.forEach(executable -> setLoopIndex(executable, index)));
        }else if(executableItem instanceof Node){
            ((Node)executableItem).setLoopIndex(index);
        }
    }

    protected void setCurrLoopObject(Executable executableItem, Object obj){
        if (executableItem instanceof Chain){
            ((Chain)executableItem).getConditionList().forEach(condition -> setCurrLoopObject(condition, obj));
        }else if(executableItem instanceof Condition){
            ((Condition) executableItem).getExecutableGroup().forEach((key, value) -> value.forEach(executable -> setCurrLoopObject(executable, obj)));
        }else if(executableItem instanceof Node){
            ((Node)executableItem).setCurrLoopObject(obj);
        }
    }


}
