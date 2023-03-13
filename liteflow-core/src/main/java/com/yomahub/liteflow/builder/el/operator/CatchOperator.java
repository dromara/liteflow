package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.CatchCondition;

/**
 * EL规则中的CATCH的操作符
 * 用法：CATCH...DO...
 * @author Bryan.Zhang
 * @since 2.9.8
 */
public class CatchOperator extends BaseOperator<CatchCondition> {
    @Override
    public CatchCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEq(objects, 1);

        Executable catchItem = OperatorHelper.convert(objects[0], Executable.class);

        CatchCondition catchCondition = new CatchCondition();
        catchCondition.setCatchItem(catchItem);

        return catchCondition;
    }
}
