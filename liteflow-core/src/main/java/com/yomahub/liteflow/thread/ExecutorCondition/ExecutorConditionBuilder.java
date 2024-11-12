package com.yomahub.liteflow.thread.ExecutorCondition;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.property.LiteflowConfig;

public class ExecutorConditionBuilder {

    /**
     * 构建执行器条件
     */
    public static ExecutorCondition buildExecutorCondition(
            Condition condition,
            Chain chain,
            LiteflowConfig liteflowConfig,
            ConditionTypeEnum type) {

        boolean conditionLevel;
        String conditionExecutorClass;

        switch (type) {
            case TYPE_FOR:
            case TYPE_WHILE:
            case TYPE_ITERATOR:
                LoopCondition loopCondition = (LoopCondition) condition;
                conditionLevel = ObjectUtil.isNotEmpty(loopCondition.getThreadPoolExecutorClass());
                conditionExecutorClass = loopCondition.getThreadPoolExecutorClass();
                break;
            case TYPE_WHEN:
                WhenCondition whenCondition = (WhenCondition) condition;
                conditionLevel = BooleanUtil.isTrue(liteflowConfig.getWhenThreadPoolIsolate());
                conditionExecutorClass = whenCondition.getThreadExecutorClass();
                break;
            default:
                throw new IllegalArgumentException("Unsupported condition type: " + type);
        }

        return ExecutorCondition.create(
                conditionLevel,
                ObjectUtil.isNotEmpty(chain.getThreadPoolExecutorClass()),
                conditionExecutorClass
        );
    }
}