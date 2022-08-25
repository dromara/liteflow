package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.IfCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的ELSE的操作符
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ElseOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Override
    public IfCondition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            //参数只能是1个，但这里为什么是2个呢？第一个是caller，第二个才是参数
            if (objects.length != 2) {
                throw new QLException("parameter error");
            }

            IfCondition ifCondition;
            if (objects[0] instanceof IfCondition) {
                ifCondition = (IfCondition) objects[0];

                if (ifCondition.getFalseCaseExecutableItem() != null) {
                    throw new QLException("The if caller already has else item");
                }
            } else {
                throw new QLException("The caller must be IfCondition item");
            }

            Executable elseExecutableItem = (Executable) objects[1];

            ifCondition.setFalseCaseExecutableItem(elseExecutableItem);

            return ifCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
