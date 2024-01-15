package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.NotCondition;

/**
 * EL表达式中NOT关键字的操作，表示非的操作
 * 主要用于适用于产生布尔类型结果的表达式中，比如IF(NOT(a)),WHILE(NOT(a))
 *
 * @author Bryan.Zhang
 * @since 2.10.2
 */
public class NotOperator extends BaseOperator<NotCondition> {
    @Override
    public NotCondition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqOne(objects);

        Object object = objects[0];
        OperatorHelper.checkObjMustBeBooleanTypeItem(object);
        Executable item = OperatorHelper.convert(object, Executable.class);

        NotCondition notCondition = new NotCondition();
        notCondition.setItem(item);

        return notCondition;
    }
}
