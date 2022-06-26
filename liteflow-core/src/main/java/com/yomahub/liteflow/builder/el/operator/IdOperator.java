package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
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
        try{
            if (ArrayUtil.isEmpty(objects)){
                throw new Exception();
            }

            if (objects.length != 2){
                LOG.error("parameter error");
                throw new Exception();
            }

            Condition condition;
            if (objects[0] instanceof Condition){
                condition = (Condition) objects[0];
            }else{
                LOG.error("The caller must be condition item!");
                throw new Exception();
            }

            String id;
            if (objects[1] instanceof String){
                id = objects[1].toString();
            }else{
                LOG.error("the parameter must be String type!");
                throw new Exception();
            }

            condition.setId(id);

            return condition;

        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
