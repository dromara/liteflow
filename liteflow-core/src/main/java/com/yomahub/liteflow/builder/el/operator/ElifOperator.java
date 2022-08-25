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

import java.util.List;

/**
 * EL规则中的ELIF的操作符
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ElifOperator extends Operator {

    @Override
    public Object executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            //参数只能是3个，第一个是caller，后面只能跟2个参数
            if (objects.length != 3) {
                throw new QLException("parameter error");
            }

            //解析caller
            IfCondition ifCondition;
            if (objects[0] instanceof IfCondition){
                ifCondition = (IfCondition) objects[0];
            }else{
                throw new QLException("elif caller must be IfCondition");
            }

            //解析第一个参数
            Node ifNode;
            if (objects[1] instanceof Node) {
                ifNode = (Node) objects[1];

                if (!ListUtil.toList(NodeTypeEnum.IF, NodeTypeEnum.IF_SCRIPT).contains(ifNode.getType())) {
                    throw new QLException("The first parameter must be If item");
                }
            } else {
                throw new QLException("The first parameter must be Node item");
            }

            //解析第二个参数
            Executable trueCaseExecutableItem = (Executable) objects[2];

            //构建一个内部的IfCondition
            IfCondition ifConditionItem = new IfCondition();
            ifConditionItem.setExecutableList(ListUtil.toList(ifNode));
            ifConditionItem.setTrueCaseExecutableItem(trueCaseExecutableItem);

            //因为可能会有多个ELIF，所以每一次拿到的caller总是最开始大的if，需要遍历到没有falseCaseExecutable的地方。
            //塞进去是一个新的IfCondition
            IfCondition loopIfCondition = ifCondition;
            while (true){
                if (loopIfCondition.getFalseCaseExecutableItem() == null){
                    loopIfCondition.setFalseCaseExecutableItem(ifConditionItem);
                    break;
                }else{
                    loopIfCondition = (IfCondition) loopIfCondition.getFalseCaseExecutableItem();
                }
            }

            return ifCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
