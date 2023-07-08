package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.CollUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.ConditionKey;
import com.yomahub.liteflow.flow.element.condition.FinallyCondition;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * EL 规则中的 maxWaitSeconds 的操作符
 *
 * @author DaleLee
 * @since 2.11.0
 */
public class MaxWaitSecondsOperator extends BaseOperator<Condition> {
    @Override
    public Condition build(Object[] objects) throws Exception {
        OperatorHelper.checkObjectSizeEqTwo(objects);
        Executable executable = OperatorHelper.convert(objects[0], Executable.class);
        // 获取传入的时间参数
        Integer maxWaitSeconds = OperatorHelper.convert(objects[1], Integer.class);
        if (executable instanceof WhenCondition) {
            // WhenCondition，直接设置等待时间
            WhenCondition whenCondition = OperatorHelper.convert(executable, WhenCondition.class);
            whenCondition.setMaxWaitTime(maxWaitSeconds);
            whenCondition.setMaxWaitTimeUnit(TimeUnit.SECONDS);
            return whenCondition;
        } else if (executable instanceof FinallyCondition) {
            // FINALLY，报错
            String errorMsg = "The caller cannot be FinallyCondition item";
            throw new QLException(errorMsg);
        } else if (containsFinally(executable)) {
            // 处理 THEN 中的 FINALLY
            ThenCondition thenCondition = OperatorHelper.convert(executable, ThenCondition.class);
            return handleFinally(thenCondition, maxWaitSeconds);
        } else {
            // 其他情况，被 WHEN 包装
            return wrappedByWhen(executable, maxWaitSeconds);
        }
    }

    /**
     * 将一个 Executable 包装为带有单独超时控制的 WhenCondition
     * @param executable 带包装对象
     * @param maxWaitSeconds 最大等待秒数
     * @return 包装后的 WhenCondition
     */
    private WhenCondition wrappedByWhen(Executable executable, Integer maxWaitSeconds) {
        WhenCondition whenCondition = new WhenCondition();
        whenCondition.addExecutable(executable);
        whenCondition.setMaxWaitTime(maxWaitSeconds);
        whenCondition.setMaxWaitTimeUnit(TimeUnit.SECONDS);
        return whenCondition;
    }

    /**
     * 判断 THEN 中是否含有 FINALLY 组件
     * @param executable 判断对象
     * @return 含有 FINALLY 组件返回 true，否则返回 false
     */
    private boolean containsFinally(Executable executable) {
        return executable instanceof ThenCondition
                && CollUtil.isNotEmpty(((ThenCondition) executable).getFinallyConditionList());
    }

    /**
     * 将 FINALLY 排除在超时控制之外
     * @param thenCondition 待处理的 ThenCondition
     * @param maxWaitSeconds 最大等待秒数
     * @return 处理后的 ThenCondition
     */
    private ThenCondition handleFinally(ThenCondition thenCondition, Integer maxWaitSeconds) {
        // 进行如下转换
        // THEN(PRE(a),b,FINALLY(c))
        // => THEN(
        //      WHEN(THEN(PRE(a),b)),
        //      FINALLY(c))

        // 定义外层 THEN
        ThenCondition outerThenCondition = new ThenCondition();

        // 把 FINALLY 转移到外层 THEN
        List<Executable> finallyList = thenCondition.getExecutableList(ConditionKey.FINALLY_KEY);
        finallyList.forEach(executable
                -> outerThenCondition
                .addFinallyCondition((FinallyCondition) executable));
        finallyList.clear();

        // 包装内部 THEN
        WhenCondition whenCondition = wrappedByWhen(thenCondition, maxWaitSeconds);
        outerThenCondition.addExecutable(whenCondition);

        return outerThenCondition;
    }
}
