package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的SWITCH的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class SwitchOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public SwitchCondition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            if (objects.length != 1) {
                throw new QLException("parameter error");
            }

            Node switchNode;
            if (objects[0] instanceof Node) {
                switchNode = (Node) objects[0];

                if (!ListUtil.toList(NodeTypeEnum.SWITCH, NodeTypeEnum.SWITCH_SCRIPT).contains(switchNode.getType())) {
                    throw new QLException("The caller must be Switch item");
                }
            } else {
                throw new QLException("The caller must be Switch item");
            }

            SwitchCondition switchCondition = new SwitchCondition();
            switchCondition.setSwitchNode(switchNode);

            return switchCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
