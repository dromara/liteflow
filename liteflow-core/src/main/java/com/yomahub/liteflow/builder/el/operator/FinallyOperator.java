package com.yomahub.liteflow.builder.el.operator;

import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.FinallyCondition;
import com.yomahub.liteflow.flow.element.condition.PreCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的THEN的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class FinallyOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public FinallyCondition executeInner(Object[] objects) throws Exception {
        try {
            if (objects.length == 0) {
                throw new QLException("parameter is empty");
            }

            FinallyCondition finallyCondition = new FinallyCondition();
            for (Object obj : objects) {
                if (obj instanceof Executable) {
                    finallyCondition.addExecutable((Executable) obj);
                } else {
                    throw new QLException("parameter must be executable item!");
                }
            }
            return finallyCondition;
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
