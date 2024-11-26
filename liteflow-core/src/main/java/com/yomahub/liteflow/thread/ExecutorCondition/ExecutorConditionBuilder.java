package com.yomahub.liteflow.thread.ExecutorCondition;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.condition.LoopCondition;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;
import com.yomahub.liteflow.property.LiteflowConfig;

/**
 * <p>Title: ExecutorConditionBuilder</p>
 * <p>Description: 执行器构建对象</p>
 *
 * @author jason
 * @since 2.13.0
 */

public class ExecutorConditionBuilder {

    /**
     * 构建执行器条件
     * @param condition condition
     * @param chain chain
     * @param liteflowConfig liteflowConfig
     * @param type type
     * @return ExecutorCondition
     */
    public static ExecutorCondition buildExecutorCondition(
            Condition condition,
            Chain chain,
            LiteflowConfig liteflowConfig,
            ConditionTypeEnum type) {

        boolean conditionLevel;
        boolean chainLevel;
        String conditionExecutorClass;

        switch (type) {
            case TYPE_FOR:
            case TYPE_WHILE:
            case TYPE_ITERATOR:
                LoopCondition loopCondition = (LoopCondition) condition;
                conditionLevel = ObjectUtil.isNotEmpty(loopCondition.getThreadPoolExecutorClass());
                conditionExecutorClass = loopCondition.getThreadPoolExecutorClass();
                chainLevel = ObjectUtil.isNotEmpty(chain.getThreadPoolExecutorClass());
                break;
            case TYPE_WHEN:
                WhenCondition whenCondition = (WhenCondition) condition;
                conditionLevel =
                        (BooleanUtil.isTrue(liteflowConfig.getWhenThreadPoolIsolate())) || (ObjectUtil.isNotEmpty(whenCondition.getThreadExecutorClass()));
                //当whenThreadPoolIsolate为true，需要有默认值
                conditionExecutorClass = whenCondition.getThreadExecutorClass() == null ?
                        liteflowConfig.getGlobalThreadPoolExecutorClass() : whenCondition.getThreadExecutorClass();
                chainLevel = ObjectUtil.isNotEmpty(chain.getThreadPoolExecutorClass());
                break;
            default:
                throw new IllegalArgumentException("Unsupported condition type: " + type);
        }

        return ExecutorCondition.create(
                conditionLevel,
                chainLevel,
                conditionExecutorClass
        );
    }
}