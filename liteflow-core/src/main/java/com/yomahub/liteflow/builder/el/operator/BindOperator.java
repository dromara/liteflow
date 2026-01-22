package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.ChainBindWrapperCondition;
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
        OperatorHelper.checkObjectSizeEq(objects, 3, 4);

        Executable bindItem = OperatorHelper.convert(objects[0], Executable.class);

        String key = OperatorHelper.convert(objects[1], String.class);

        String value = OperatorHelper.convert(objects[2], String.class);

        // 获取 override 参数（第四个参数，默认为 false）
        boolean override = false;
        if (objects.length > 3) {
            override = OperatorHelper.convert(objects[3], Boolean.class);
        }

        // 场景1：对 Node bind（保持现有逻辑，bind 数据存在 Node 上）
        if (bindItem instanceof Node) {
            Node node = (Node) bindItem;
            node.putBindData(key, value);
            return node;
        }

        // 场景2：对 Condition bind（如 THEN(...).bind(...)），bind 数据存在 Condition 上
        if (bindItem instanceof Condition) {
            Condition condition = (Condition) bindItem;
            condition.putBindData(key, value);
            // 如果 override=true，需要清除该 Condition 下所有 Node 上相同 key 的 bind 数据
            // 这样可以确保 Condition 级别的 bind 能够覆盖 Node 级别的 bind
            if (override) {
                clearNodeBindData(condition, key);
            }
            return condition;
        }

        // 场景3：对 Chain bind（新逻辑：包装成 ChainBindWrapperCondition）
        // 这样不会修改 Chain 本身，而是创建一个包装 Condition 来持有 bind 数据
        // 从而避免多个 chain 引用同一个子 chain 时的 bind 数据污染问题
        if (bindItem instanceof Chain) {
            Chain chain = (Chain) bindItem;
            ChainBindWrapperCondition wrapper = new ChainBindWrapperCondition(chain);
            wrapper.putBindData(key, value);
            return wrapper;
        }

        return bindItem;
    }

    /**
     * 清除 Condition 下所有 Node 上指定 key 的 bind 数据
     * 用于 override=true 时，确保 Condition 级别的 bind 能够覆盖 Node 级别的 bind
     */
    private void clearNodeBindData(Condition condition, String key) {
        LiteflowMetaOperator.getNodes(condition).forEach(node -> {
            if (node.hasBindData(key)) {
                node.removeBindData(key);
            }
        });
    }
}
