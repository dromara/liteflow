package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的threadPool的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ThreadPoolOperator extends Operator {

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

            String threadPoolClazz = null;
            if (objects[1] instanceof String){
                threadPoolClazz = objects[1].toString();
            }else{
                LOG.error("the parameter must be String type!");
                throw new Exception();
            }

            whenCondition.setThreadExecutorClass(threadPoolClazz);

            return whenCondition;

        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
