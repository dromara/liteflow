package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.FallbackNode;
import com.yomahub.liteflow.flow.element.Node;

/**
 * EL规则中的node的操作符
 *
 * @author Bryan.Zhang
 * @author DaleLee
 * @since 2.8.3
 */
public class NodeOperator extends BaseOperator<Node> {

    @Override
    public Node build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqOne(objects);
        String nodeId = OperatorHelper.convert(objects[0], String.class);

        if (FlowBus.containNode(nodeId)) {
            // 找到对应节点
            return FlowBus.getNode(nodeId);
        } else {
            // 生成代理节点
            return new FallbackNode(nodeId);
        }
    }

}
