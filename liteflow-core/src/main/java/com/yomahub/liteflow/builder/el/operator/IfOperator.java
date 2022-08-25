package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.IfCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的IF的操作符
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class IfOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public IfCondition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            //参数只能是2个或者3个
            if (objects.length != 2 && objects.length != 3) {
                throw new QLException("parameter error");
            }

            //解析第一个参数
            Node ifNode;
            if (objects[0] instanceof Node) {
                ifNode = (Node) objects[0];

                if (!ListUtil.toList(NodeTypeEnum.IF, NodeTypeEnum.IF_SCRIPT).contains(ifNode.getType())) {
                    throw new QLException("The first parameter must be If item");
                }
            } else {
                throw new QLException("The first parameter must be Node item");
            }

            //解析第二个参数
            Executable trueCaseExecutableItem = (Executable) objects[1];

            //解析第三个参数，如果有的话
            Executable falseCaseExecutableItem = null;
            if (objects.length == 3) {
                falseCaseExecutableItem = (Executable) objects[2];
            }

            IfCondition ifCondition = new IfCondition();
            ifCondition.setExecutableList(ListUtil.toList(ifNode));
            ifCondition.setTrueCaseExecutableItem(trueCaseExecutableItem);
            ifCondition.setFalseCaseExecutableItem(falseCaseExecutableItem);
            return ifCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
