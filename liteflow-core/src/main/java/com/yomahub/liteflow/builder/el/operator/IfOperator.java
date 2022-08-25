package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
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
    public Object executeInner(Object[] objects) throws Exception {
        try{
            if (ArrayUtil.isEmpty(objects)){
                throw new Exception();
            }

            //参数只能是2个或者3个
            if (objects.length != 2 && objects.length != 3){
                LOG.error("parameter error");
                throw new Exception();
            }

            //解析第一个参数
            Node ifNode;
            if (objects[0] instanceof Node){
                ifNode = (Node) objects[0];

                if(!ifNode.getType().equals(NodeTypeEnum.IF)){
                    LOG.error("The first parameter must be If item!");
                    throw new Exception();
                }
            }else{
                LOG.error("The first parameter must be Node item!");
                throw new Exception();
            }

            //解析第二个参数
            Executable trueCaseExecutableItem = (Executable) objects[1];

            //解析第三个参数，如果有的话
            Executable falseCaseExecutableItem = null;
            if (objects.length == 3){
                falseCaseExecutableItem = (Executable) objects[2];
            }

            IfCondition ifCondition = new IfCondition();
            ifCondition.setExecutableList(ListUtil.toList(ifNode));
            ifCondition.setTrueCaseExecutableItem(trueCaseExecutableItem);
            ifCondition.setFalseCaseExecutableItem(falseCaseExecutableItem);
            return ifCondition;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
