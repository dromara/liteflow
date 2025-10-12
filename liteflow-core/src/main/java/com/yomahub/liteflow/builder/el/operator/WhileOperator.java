package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.core.NodeForComponent;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
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
		OperatorHelper.checkObjectSizeEqOne(objects);

        Object param = objects[0];

        WhileCondition whileCondition = new WhileCondition();
        if (param instanceof Boolean) {
            boolean booleanParam = OperatorHelper.convert(objects[0], Boolean.class);
            Node node = new Node();
            node.setType(NodeTypeEnum.BOOLEAN);
            NodeBooleanComponent nodeBooleanComponent = new NodeBooleanComponent() {
                @Override
                public boolean processBoolean() {
                    return booleanParam;
                }
            };
            nodeBooleanComponent.setSelf(nodeBooleanComponent);
            nodeBooleanComponent.setNodeId(StrUtil.format("LOOP_{}", booleanParam));
            nodeBooleanComponent.setType(NodeTypeEnum.BOOLEAN);
            node.setInstance(nodeBooleanComponent);
            node.setId(nodeBooleanComponent.getNodeId());
            whileCondition.setWhileItem(node);
        }else{
            OperatorHelper.checkObjMustBeBooleanTypeItem(param);
            Executable whileItem = OperatorHelper.convert(param, Executable.class);
            whileCondition.setWhileItem(whileItem);
        }
		return whileCondition;
	}

}
