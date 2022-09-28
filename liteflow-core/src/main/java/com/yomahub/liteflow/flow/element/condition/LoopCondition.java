package com.yomahub.liteflow.flow.element.condition;

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
}
