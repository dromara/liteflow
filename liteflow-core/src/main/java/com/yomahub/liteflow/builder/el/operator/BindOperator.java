package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.BooleanUtil;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * EL规则中的bind的操作符
 *
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class BindOperator extends BaseOperator<Executable> {
    @Override
    public Executable build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEq(objects, 3, 4);

        Executable bindItem = OperatorHelper.convert(objects[0], Executable.class);

        String key = OperatorHelper.convert(objects[1], String.class);

        String value = OperatorHelper.convert(objects[2], String.class);

        AtomicBoolean override = new AtomicBoolean(false);
        if (objects.length > 3) {
            override.set(OperatorHelper.convert(objects[3], Boolean.class));
        }

        LiteflowMetaOperator.getNodes(bindItem).forEach(node -> {
            if (BooleanUtil.isFalse(node.hasBindData(key)) || override.get()){
                node.putBindData(key, value);
            }
        });

        return bindItem;
    }
}
