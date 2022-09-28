package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.Condition;
import com.yomahub.liteflow.flow.element.condition.ForCondition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;

/**
 * EL规则中的BREAK的操作符
 * 有两种用法
 * FOR...DO...BREAK
 * WHILE...DO...BREAK
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class BreakOperator extends BaseOperator<LoopCondition> {
    @Override
    public LoopCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqTwo(objects);

        //由于BREAK关键字有可能用在FOR后面，也有可能用于WHILE后面，所以这里要进行判断
        LoopCondition condition;
        if (objects[0] instanceof ForCondition){
            //获得caller，也就是ForCondition
            condition = OperatorHelper.convert(objects[0], ForCondition.class);
        }else if(objects[0] instanceof WhileCondition){
            //获得caller，也就是WhileCondition
            condition = OperatorHelper.convert(objects[0], WhileCondition.class);
        }else{
            throw new QLException("The caller must be ForCondition or WhileCondition item");
        }

        //获得需要执行的可执行表达式
        if (objects[1] instanceof Node){
            Node breakNode = OperatorHelper.convert(objects[1], Node.class);

            if (ListUtil.toList(NodeTypeEnum.BREAK, NodeTypeEnum.BREAK_SCRIPT).contains(breakNode.getType())){
                condition.setBreakNode(breakNode);
            }else{
                throw new QLException("The parameter must be node-break item");
            }
        }else{
            throw new QLException("The parameter must be Node item");
        }
        return condition;
    }
}
