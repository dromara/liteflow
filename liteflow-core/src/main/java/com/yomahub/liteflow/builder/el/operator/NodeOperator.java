package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.FallbackNodeProxy;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

/**
 * EL规则中的node的操作符
 *
 * @author Bryan.Zhang,DaleLee
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
            // 检查是否开启了组件降级功能
            LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
            Boolean enable = liteflowConfig.getFallbackCmpEnable();
            if (!enable) {
                throw new ELParseException("The fallback component is disabled");
            }
            // 生成代理节点
            return new FallbackNodeProxy(nodeId);
        }
    }

}
