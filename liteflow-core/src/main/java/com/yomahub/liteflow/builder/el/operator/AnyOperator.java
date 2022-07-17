package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
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
        try{
            if (ArrayUtil.isEmpty(objects)){
                throw new Exception();
            }

            if (objects.length != 2){
                LOG.error("parameter error");
                throw new Exception();
            }

            WhenCondition whenCondition;
            if (objects[0] instanceof WhenCondition){
                whenCondition = (WhenCondition) objects[0];
            }else{
                LOG.error("The caller must be when condition item!");
                throw new Exception();
            }

            boolean any = false;
            if (objects[1] instanceof Boolean){
                any = Boolean.parseBoolean(objects[1].toString());
            }else{
                LOG.error("the parameter must be boolean type!");
                throw new Exception();
            }

            whenCondition.setAny(any);

            return whenCondition;

        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
