package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的TO的操作符，用法须和SWITCH联合使用
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ToOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public SwitchCondition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            if (objects.length <= 1) {
                throw new QLException("parameter error");
            }

            SwitchCondition switchCondition;
            if (objects[0] instanceof SwitchCondition) {
                switchCondition = (SwitchCondition) objects[0];
            } else {
                throw new QLException("The caller must be SwitchCondition item");
            }

            for (int i = 1; i < objects.length; i++) {
                if (objects[i] instanceof Executable) {
                    Executable target = (Executable) objects[i];
                    switchCondition.addTargetItem(target);
                } else {
                    throw new QLException("The parameter must be Executable item");
                }
            }
            return switchCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
