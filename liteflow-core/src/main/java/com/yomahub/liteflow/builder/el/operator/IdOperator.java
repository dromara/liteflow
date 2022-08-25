package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.condition.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的id的操作符,只有condition可加id
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class IdOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public Condition executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            if (objects.length != 2) {
                throw new QLException("parameter error");
            }

            Condition condition;
            if (objects[0] instanceof Condition) {
                condition = (Condition) objects[0];
            } else {
                throw new QLException("The caller must be condition item");
            }

            String id;
            if (objects[1] instanceof String) {
                id = objects[1].toString();
            } else {
                LOG.error("the parameter must be String type!");
                throw new QLException("the parameter must be String type");
            }

            condition.setId(id);

            return condition;

        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
