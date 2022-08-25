package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的THEN的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ThenOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public ThenCondition executeInner(Object[] objects) throws Exception {
        try {
            if (objects.length == 0) {
                throw new QLException("parameter is empty");
            }

            ThenCondition thenCondition = new ThenCondition();
            for (Object obj : objects) {
                if (obj instanceof Executable) {
                    thenCondition.addExecutable((Executable) obj);
                } else {
                    throw new QLException("parameter must be executable item");
                }
            }
            return thenCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
