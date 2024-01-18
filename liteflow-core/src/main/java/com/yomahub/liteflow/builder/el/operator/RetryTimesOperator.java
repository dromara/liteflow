package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.RetryCondition;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;
import com.yomahub.liteflow.flow.element.condition.WhileCondition;

/**
 *
 * @author Rain
 * @since 2.11.5
 *
 */
public class RetryTimesOperator extends BaseOperator<Condition> {
    @Override
    public Condition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeGtTwo(objects);
        Executable executable = OperatorHelper.convert(objects[0], Executable.class);
        Integer retryTimes = OperatorHelper.convert(objects[1], Integer.class);
        RetryCondition retryCondition = new RetryCondition();
        retryCondition.addExecutable(executable);
        retryCondition.setRetryTimes(retryTimes);
        return retryCondition;
    }

}
