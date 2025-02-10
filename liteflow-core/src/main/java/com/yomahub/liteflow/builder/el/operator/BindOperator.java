package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

/**
 * EL规则中的bind的操作符
 *
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class BindOperator extends BaseOperator<Executable> {
    @Override
    public Executable build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqThree(objects);

        Executable bindItem = OperatorHelper.convert(objects[0], Executable.class);

        String key = OperatorHelper.convert(objects[1], String.class);

        String value = OperatorHelper.convert(objects[2], String.class);

        LiteflowMetaOperator.getNodes(bindItem).forEach(node -> node.putBindData(key, value));

        return bindItem;
    }
}
