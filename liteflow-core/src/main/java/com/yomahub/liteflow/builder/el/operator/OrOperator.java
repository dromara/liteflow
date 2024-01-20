package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.AndOrCondition;
import com.yomahub.liteflow.flow.element.condition.BooleanConditionTypeEnum;

/**
 * EL表达式中OR关键字的操作
 * 主要用于适用于产生布尔类型结果的表达式中，比如IF(OR(a,b)),WHILE(OR(a,b))
 *
 * @author Bryan.Zhang
 * @since 2.10.2
 */
public class OrOperator extends BaseOperator<AndOrCondition> {
    @Override
    public AndOrCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeGteTwo(objects);

        AndOrCondition andOrCondition = new AndOrCondition();
        andOrCondition.setBooleanConditionType(BooleanConditionTypeEnum.OR);

        for (Object object : objects){
            OperatorHelper.checkObjMustBeBooleanTypeItem(object);

            Executable item = OperatorHelper.convert(object, Executable.class);
            andOrCondition.addItem(item);
        }

        return andOrCondition;
    }
}
