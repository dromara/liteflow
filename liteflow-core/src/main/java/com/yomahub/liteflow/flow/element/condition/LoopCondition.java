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

    protected Node getBreakNode() {
        return (Node) this.getExecutableOne(ConditionKey.BREAK_KEY);
    }

    public void setBreakNode(Node breakNode) {
        this.addExecutable(ConditionKey.BREAK_KEY, breakNode);
    }

    protected Executable getDoExecutor() {
        return this.getExecutableOne(ConditionKey.DO_KEY);
    }

    public void setDoExecutor(Executable executable) {
        this.addExecutable(ConditionKey.DO_KEY, executable);
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
