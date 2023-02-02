package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

/**
 * 循环Condition的抽象类
 * 主要继承对象有ForCondition和WhileCondition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public abstract class LoopCondition extends Condition {

    protected Node breakNode;

    public Node getBreakNode() {
        return breakNode;
    }

    public void setBreakNode(Node breakNode) {
        this.breakNode = breakNode;
    }

    protected void setLoopIndex(Executable executableItem, int index){
        if (executableItem instanceof Chain){
            ((Chain)executableItem).getConditionList().forEach(condition -> setLoopIndex(condition, index));
        }else if(executableItem instanceof Condition){
            ((Condition)executableItem).getExecutableList().forEach(executable -> setLoopIndex(executable, index));
        }else if(executableItem instanceof Node){
            ((Node)executableItem).setLoopIndex(index);
        }
    }

    protected void setCurrLoopObject(Executable executableItem, Object obj){
        if (executableItem instanceof Chain){
            ((Chain)executableItem).getConditionList().forEach(condition -> setCurrLoopObject(condition, obj));
        }else if(executableItem instanceof Condition){
            ((Condition)executableItem).getExecutableList().forEach(executable -> setCurrLoopObject(executable, obj));
        }else if(executableItem instanceof Node){
            ((Node)executableItem).setCurrLoopObject(obj);
        }
    }

    protected Executable getDoExecutor() {
        return this.getExecutableList().get(0);
    }
}
