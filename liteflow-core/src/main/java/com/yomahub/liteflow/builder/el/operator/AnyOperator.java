package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的any的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class AnyOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public WhenCondition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            if (objects.length != 2) {
                throw new QLException("parameter error");
            }

            WhenCondition whenCondition;
            if (objects[0] instanceof WhenCondition) {
                whenCondition = (WhenCondition) objects[0];
            } else {
                throw new QLException("The caller must be when condition item");
            }

            boolean any = false;
            if (objects[1] instanceof Boolean) {
                any = Boolean.parseBoolean(objects[1].toString());
            } else {
                throw new QLException("the parameter must be boolean type");
            }

            whenCondition.setAny(any);

            return whenCondition;

        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
