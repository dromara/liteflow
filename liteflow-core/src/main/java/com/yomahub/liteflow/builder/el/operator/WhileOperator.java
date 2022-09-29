package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;

/**
 * EL规则中的WHILE的操作符
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class WhileOperator extends BaseOperator<WhileCondition> {
    @Override
    public WhileCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEq(objects, 1);

        Node node = OperatorHelper.convert(objects[0], Node.class);
        if (!ListUtil.toList(NodeTypeEnum.WHILE, NodeTypeEnum.WHILE_SCRIPT).contains(node.getType())) {
            throw new QLException("The parameter must be while-node item");
        }

        WhileCondition whileCondition = new WhileCondition();
        whileCondition.setWhileNode(node);
        return whileCondition;
    }
}
