package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.RetryCondition;

/**
 *
 * @author Rain
 * @since 2.12.0
 *
 */
public class RetryOperator extends BaseOperator<Condition> {
    @Override
    public Condition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeGteTwo(objects);
        Executable executable = OperatorHelper.convert(objects[0], Executable.class);
        Integer retryTimes = OperatorHelper.convert(objects[1], Integer.class);
        RetryCondition retryCondition = new RetryCondition();
        retryCondition.addExecutable(executable);
        retryCondition.setRetryTimes(retryTimes);
        if(objects.length > 2) {
            Class[] forExceptions = new Class[objects.length - 2];
            for(int i = 2; i < objects.length; i ++) {
                String className = OperatorHelper.convert(objects[i], String.class);
                forExceptions[i - 2] = Class.forName(className);
            }
            retryCondition.setRetryForExceptions(forExceptions);
        }
        return retryCondition;
    }

}
